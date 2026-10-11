package com.graveyard.feature.news.viewmodel

import androidx.annotation.MainThread
import com.graveyard.core.data.local.preferences.DefaultTagInfos
import com.graveyard.core.data.local.preferences.TagSettingsSnapshot
import com.graveyard.core.data.local.preferences.UserSettings
import com.graveyard.core.data.repository.NewsRepository
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataResult
import com.graveyard.core.data.result.toDataException
import com.graveyard.core.model.news.TagInfo
import com.graveyard.core.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class NewsTagEditorState(
    val isVisible: Boolean = false,
    val isInitializing: Boolean = false,
    val initializationFailed: Boolean = false,
    val userTags: List<TagInfo> = DefaultTagInfos,
    val allTags: List<TagInfo> = DefaultTagInfos,
    val availableTagRows: List<List<TagInfo>> = emptyList(),
    val missingSubscribedIds: Set<Int> = emptySet(),
    val isRefreshing: Boolean = false,
    val refreshError: DataError? = null,
    val isSaving: Boolean = false,
    val isApplyingRefresh: Boolean = false,
    val saveFailed: Boolean = false,
    val draggingTagId: Int? = null,
) {
    val canEdit: Boolean
        get() = isVisible && !isInitializing && !initializationFailed &&
            !isSaving && !isApplyingRefresh

    val canAddOrRemove: Boolean
        get() = canEdit && draggingTagId == null
}

/** Owned by the ViewModel; closing the sheet cancels observation, never a write or refresh. */
internal class NewsTagEditor(
    private val userSettings: UserSettings,
    private val repository: NewsRepository,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(NewsTagEditorState())
    val state = mutableState.asStateFlow()
    private val saveFailureMessages = Channel<Unit>(Channel.BUFFERED)
    val messages = saveFailureMessages.receiveAsFlow()

    private var savedUserTags: List<TagInfo> = DefaultTagInfos
    private var savedSnapshot = TagSettingsSnapshot()
    private var appliedRevision = -1L
    private var settingsJob: Job? = null
    private var refreshJob: Job? = null
    private var saveJob: Job? = null

    @MainThread
    fun open() {
        if (mutableState.value.isVisible) return
        mutableState.update { it.copy(isVisible = true, saveFailed = false) }
        loadSettings()
    }

    @MainThread
    fun retryInitialization() {
        if (mutableState.value.isVisible && mutableState.value.initializationFailed) {
            loadSettings()
        }
    }

    private fun loadSettings() {
        settingsJob?.cancel()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                // Reopening while a write is pending must not restore the old disk snapshot.
                saveJob?.join()
                // UI readiness must not wait for a refresh or its stream publication.
                // applySnapshot already rejects stale replays; a committed snapshot can
                // reach this editor before the settings flow publishes that revision.
                val snapshot = userSettings.tagSettings.first()
                applySnapshot(snapshot)
                mutableState.update {
                    it.copy(isInitializing = false)
                }
                if (!savedSnapshot.hasCachedCatalog) refresh()
                // A persisted snapshot cannot overwrite a live drag or optimistic save.
                // The write/refresh completion applies its returned snapshot explicitly.
                userSettings.tagSettings.collect { persisted ->
                    val current = mutableState.value
                    if (!current.isSaving && !current.isApplyingRefresh && current.draggingTagId == null) {
                        applySnapshot(persisted)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()
                Logger.e(TAG, "读取新闻标签设置失败", exception)
                mutableState.update {
                    it.copy(isInitializing = false, initializationFailed = true)
                }
            } finally {
                // A cancelled observer belongs to one opening only. It must never
                // clear the loading state of a subsequently opened sheet.
                if (settingsJob === currentCoroutineContext()[Job]) {
                    mutableState.update { it.copy(isInitializing = false) }
                }
            }
        }
        // Register before starting so close() can cancel even an immediately resumed read.
        settingsJob = job
        mutableState.update {
            it.copy(isInitializing = appliedRevision < 0, initializationFailed = false)
        }
        job.start()
    }

    @MainThread
    fun close() {
        val reportFailure = mutableState.value.saveFailed
        val observer = settingsJob
        settingsJob = null
        observer?.cancel()
        mutableState.update {
            it.copy(isVisible = false, isInitializing = false, saveFailed = false)
        }
        finishDrag()
        if (reportFailure) saveFailureMessages.trySend(Unit)
    }

    @MainThread
    fun refresh() {
        if (refreshJob?.isActive == true) return
        mutableState.update { it.copy(isRefreshing = true, refreshError = null) }
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val result = repository.refreshTagInfos()
                currentCoroutineContext().ensureActive()
                when (result) {
                    is DataResult.Success -> {
                        // Fetching may overlap edits; committing waits for their durable save.
                        state.first {
                            !it.isSaving && it.draggingTagId == null
                        }
                        // Disable edits synchronously before dispatching the transaction.
                        mutableState.update { it.copy(isApplyingRefresh = true) }
                        applySnapshot(userSettings.setAllTagList(result.data))
                    }
                    is DataResult.Failure -> {
                        Logger.e(TAG, "刷新全量新闻标签失败：${result.error}")
                        mutableState.update { it.copy(refreshError = result.error) }
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()
                Logger.e(TAG, "提交新闻标签目录失败，保留原列表", exception)
                mutableState.update { it.copy(refreshError = exception.toDataException().error) }
            } finally {
                mutableState.update { it.copy(isRefreshing = false, isApplyingRefresh = false) }
            }
        }
        refreshJob = job
        job.start()
    }

    @MainThread
    fun add(tagId: Int) {
        val current = mutableState.value
        if (!current.canAddOrRemove || current.userTags.any { it.id == tagId }) return
        val tag = current.allTags.firstOrNull { it.id == tagId } ?: return
        save(current.userTags + tag)
    }

    @MainThread
    fun remove(tagId: Int) {
        val current = mutableState.value
        if (!current.canAddOrRemove || current.userTags.size <= 1) return
        save(current.userTags.filterNot { it.id == tagId })
    }

    @MainThread
    fun startDrag(tagId: Int) {
        val current = mutableState.value
        if (!current.canAddOrRemove || current.userTags.none { it.id == tagId }) return
        mutableState.update { it.copy(draggingTagId = tagId, saveFailed = false) }
    }

    @MainThread
    fun move(fromId: Int, toId: Int): Boolean {
        val current = mutableState.value
        if (!current.canEdit || current.draggingTagId != fromId) return false
        val fromIndex = current.userTags.indexOfFirst { it.id == fromId }
        val toIndex = current.userTags.indexOfFirst { it.id == toId }
        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return false
        val reordered = current.userTags.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        mutableState.update { it.copy(userTags = reordered) }
        return true
    }

    @MainThread
    fun finishDrag() {
        val current = mutableState.value
        if (current.draggingTagId == null) return
        save(current.userTags, finishDrag = true)
    }

    /** Keyboard/screen-reader alternative to the drag handle. */
    @MainThread
    fun moveBy(tagId: Int, offset: Int) {
        val current = mutableState.value
        if (!current.canAddOrRemove) return
        val index = current.userTags.indexOfFirst { it.id == tagId }
        val target = index + offset
        if (index < 0 || target !in current.userTags.indices) return
        val reordered = current.userTags.toMutableList().apply {
            add(target, removeAt(index))
        }
        save(reordered)
    }

    private fun save(tags: List<TagInfo>, finishDrag: Boolean = false) {
        if (mutableState.value.isSaving) return
        if (tags == savedUserTags) {
            if (finishDrag) mutableState.update { it.copy(draggingTagId = null) }
            return
        }
        // Disable edits synchronously, before launching, so rapid taps cannot race writes.
        // End-drag and start-save are one emission; a waiting refresh sees no idle gap.
        mutableState.update {
            it.copy(
                userTags = tags,
                draggingTagId = if (finishDrag) null else it.draggingTagId,
                isSaving = true,
                saveFailed = false,
            )
        }
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                applySnapshot(userSettings.setUserTagList(tags))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()
                Logger.e(TAG, "保存用户新闻标签失败", exception)
                val isVisible = mutableState.value.isVisible
                mutableState.update {
                    it.copy(
                        userTags = savedUserTags,
                        allTags = savedSnapshot.allTags,
                        availableTagRows = savedSnapshot.availableTagRows,
                        missingSubscribedIds = savedSnapshot.missingSubscribedIds,
                        saveFailed = isVisible,
                    )
                }
                if (!isVisible) saveFailureMessages.trySend(Unit)
            } finally {
                mutableState.update { it.copy(isSaving = false) }
            }
        }
        saveJob = job
        job.start()
    }

    private fun applySnapshot(snapshot: TagSettingsSnapshot) {
        // Ignore an older replay after reopening or a queued pre-commit emission.
        if (snapshot.revision <= appliedRevision) return
        appliedRevision = snapshot.revision
        savedSnapshot = snapshot
        savedUserTags = snapshot.userTags
        mutableState.update {
            it.copy(
                userTags = snapshot.userTags,
                allTags = snapshot.allTags,
                availableTagRows = snapshot.availableTagRows,
                missingSubscribedIds = snapshot.missingSubscribedIds,
            )
        }
    }

    private companion object {
        const val TAG = "NewsTagEditor"
    }
}
