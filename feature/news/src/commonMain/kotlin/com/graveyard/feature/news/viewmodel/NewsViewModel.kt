package com.graveyard.feature.news.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.graveyard.core.data.local.preferences.DefaultArticleTags
import com.graveyard.core.data.local.preferences.UserSettings
import com.graveyard.core.data.network.model.NewsArticleTag
import com.graveyard.core.data.paging.NewsRefreshEvent
import com.graveyard.core.data.repository.NewsRepository
import com.graveyard.core.data.result.DataResult
import com.graveyard.core.model.news.NewsArticle
import com.graveyard.feature.news.model.NewsCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewsViewModel(
    private val repository: NewsRepository,
    userSettings: UserSettings,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = mutableUiState.asStateFlow()

    // 新闻标签来自用户设置；同步缓存值作为初始值避免首帧空白，空列表回退内置默认（TabRow 要求非空）。
    val categories: StateFlow<List<NewsCategory>> = userSettings.articleTags
        .map { tags -> tags.toCategories() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = userSettings.getArticleTags().toCategories(),
        )

    private val newsSessions = mutableMapOf<Int, NewsSession>()
    private val bannerJobs = mutableMapOf<Int, Job>()

    @MainThread
    fun updateCategories(tagIds: List<Int>) {
        require(tagIds.isNotEmpty()) { "News requires at least one category." }
        require(tagIds.distinct().size == tagIds.size) { "News category IDs must be unique." }
        val retainedIds = tagIds.toSet()
        (newsSessions.keys - retainedIds).forEach { tagId ->
            newsSessions.remove(tagId)?.scope?.cancel()
        }
        (bannerJobs.keys - retainedIds).forEach { tagId ->
            bannerJobs.remove(tagId)?.cancel()
        }
        mutableUiState.update { state ->
            state.copy(bannersByTag = state.bannersByTag.filterKeys { it in retainedIds })
        }
        selectTag(mutableUiState.value.selectedTagId?.takeIf { it in retainedIds } ?: tagIds.first())
    }

    @MainThread
    fun selectTag(tagId: Int) {
        if (mutableUiState.value.selectedTagId != tagId) {
            mutableUiState.update { it.copy(selectedTagId = tagId) }
        }
        newsFor(tagId)
        if (tagId !in mutableUiState.value.bannersByTag) {
            reloadBanners(tagId)
        }
    }

    @MainThread
    fun newsFor(tagId: Int): Flow<PagingData<NewsArticle>> =
        sessionFor(tagId).news

    @MainThread
    internal fun pagingRefreshStateFor(tagId: Int): StateFlow<NewsPagingRefreshState> =
        sessionFor(tagId).refreshState.asStateFlow()

    private fun sessionFor(tagId: Int): NewsSession = newsSessions.getOrPut(tagId) {
        val scope = CoroutineScope(
            viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]),
        )
        val refreshState = MutableStateFlow(NewsPagingRefreshState())
        val news = repository.getNews(
            tagId = tagId,
            onRefreshEvent = { event ->
                refreshState.update { previous ->
                    when {
                        event is NewsRefreshEvent.Started -> NewsPagingRefreshState(
                            request = event.request,
                            isLoading = true,
                        )
                        previous.request !== event.request -> previous
                        event is NewsRefreshEvent.Completed -> NewsPagingRefreshState(
                            request = event.request,
                            error = event.error,
                        )
                        else -> NewsPagingRefreshState(
                            request = event.request,
                            isCancelled = true,
                        )
                    }
                }
            },
        ).cachedIn(scope)
        NewsSession(scope, refreshState, news)
    }

    @MainThread
    fun reloadBanners(tagId: Int): Job {
        bannerJobs[tagId]?.cancel()
        mutableUiState.update { state ->
            val previous = state.bannersByTag[tagId] ?: BannerUiState()
            state.copy(
                bannersByTag = state.bannersByTag + (
                    tagId to previous.copy(isLoading = true, error = null)
                ),
            )
        }

        // Register the job before it can complete or suspend in the repository.
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                val result = repository.getBanners(tagId)
                currentCoroutineContext().ensureActive()
                mutableUiState.update { state ->
                    val bannerState = when (result) {
                        is DataResult.Success -> BannerUiState(items = result.data)
                        is DataResult.Failure -> {
                            val previous = state.bannersByTag[tagId] ?: BannerUiState()
                            previous.copy(isLoading = false, error = result.error)
                        }
                    }
                    state.copy(bannersByTag = state.bannersByTag + (tagId to bannerState))
                }
            } finally {
                if (bannerJobs[tagId] === currentCoroutineContext()[Job]) {
                    bannerJobs.remove(tagId)
                    mutableUiState.update { state ->
                        val previous = state.bannersByTag[tagId]
                        if (previous?.isLoading == true) {
                            state.copy(
                                bannersByTag = state.bannersByTag +
                                    (tagId to previous.copy(isLoading = false)),
                            )
                        } else {
                            state
                        }
                    }
                }
            }
        }
        bannerJobs[tagId] = job
        job.start()
        return job
    }

    override fun onCleared() {
        bannerJobs.values.toList().forEach { it.cancel() }
        bannerJobs.clear()
        newsSessions.values.forEach { it.scope.cancel() }
        newsSessions.clear()
        super.onCleared()
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** 用户设置标签 → UI 分类；空列表回退内置默认标签。 */
private fun List<NewsArticleTag>.toCategories(): List<NewsCategory> =
    ifEmpty { DefaultArticleTags }.map { NewsCategory(id = it.id, label = it.label) }

private class NewsSession(
    val scope: CoroutineScope,
    val refreshState: MutableStateFlow<NewsPagingRefreshState>,
    val news: Flow<PagingData<NewsArticle>>,
)
