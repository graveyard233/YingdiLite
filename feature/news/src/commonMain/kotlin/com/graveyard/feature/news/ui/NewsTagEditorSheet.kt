package com.graveyard.feature.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.model.news.TagInfo
import com.graveyard.feature.news.viewmodel.NewsTagEditorState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.news_manage_tags
import yingdilite.feature.news.generated.resources.news_retry
import yingdilite.feature.news.generated.resources.news_tags_add
import yingdilite.feature.news.generated.resources.news_tags_all_added
import yingdilite.feature.news.generated.resources.news_tags_close
import yingdilite.feature.news.generated.resources.news_tags_hint
import yingdilite.feature.news.generated.resources.news_tags_initialization_failed
import yingdilite.feature.news.generated.resources.news_tags_loading
import yingdilite.feature.news.generated.resources.news_tags_more
import yingdilite.feature.news.generated.resources.news_tags_missing
import yingdilite.feature.news.generated.resources.news_tags_move_down
import yingdilite.feature.news.generated.resources.news_tags_move_up
import yingdilite.feature.news.generated.resources.news_tags_my
import yingdilite.feature.news.generated.resources.news_tags_refresh
import yingdilite.feature.news.generated.resources.news_tags_refresh_failed
import yingdilite.feature.news.generated.resources.news_tags_refreshing
import yingdilite.feature.news.generated.resources.news_tags_remove
import yingdilite.feature.news.generated.resources.news_tags_reorder
import yingdilite.feature.news.generated.resources.news_tags_save_failed
import yingdilite.feature.news.generated.resources.news_tags_saving

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewsTagEditorSheet(
    state: NewsTagEditorState,
    onDismiss: () -> Unit,
    onRetryInitialization: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (Int) -> Unit,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onDragStarted: (Int) -> Unit,
    onMove: (Int, Int) -> Boolean,
    onDragStopped: () -> Unit,
    onMoveBy: (Int, Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val latestState by rememberUpdatedState(state)
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val fromId = from.key as? Int
        val toId = to.key as? Int
        val current = latestState
        if (
            fromId != null && toId != null && current.canEdit &&
            (current.draggingTagId == null || current.draggingTagId == fromId)
        ) {
            val fromIndex = current.userTags.indexOfFirst { it.id == fromId }
            val toIndex = current.userTags.indexOfFirst { it.id == toId }
            if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
                val expected = current.userTags.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
                if (onMove(fromId, toId)) {
                    // Wait for the displayed order, including the first move before start-drag
                    // state has reached composition. Rejected/late callbacks never wait.
                    snapshotFlow { latestState }.first {
                        it.userTags == expected || !it.canEdit
                    }
                }
            }
        }
    }
    val userTags = state.userTags
    // Validation, filtering and grouping are prepared off the UI thread.
    // Dragging changes only userTags and reuses these rows.
    val availableTagRows = state.availableTagRows
    val missingSubscribedIds = state.missingSubscribedIds
    val tagCount = userTags.size
    val canEdit = state.canEdit
    val canAddOrRemove = state.canAddOrRemove
    val canRefresh = canAddOrRemove && !state.isRefreshing
    val isRefreshing = state.isRefreshing
    val draggingTagId = state.draggingTagId
    val keepActionColors = state.isSaving && tagCount > 1
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        sheetMaxWidth = 640.dp,
        sheetGesturesEnabled = draggingTagId == null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surfaceContainerLow,
    ) {
        // Keep the viewport independent of the number of tags, without subcomposition.
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
        ) {
            TagEditorHeader(isSaving = state.isSaving, onDismiss = onDismiss)
            Text(
                stringResource(Res.string.news_tags_hint),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 5.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            if (state.saveFailed) {
                Text(
                    stringResource(Res.string.news_tags_save_failed),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 11.dp, bottom = 24.dp),
            ) {
                when {
                    state.isInitializing -> item(key = "initializing", contentType = "status") {
                        TagEditorProgress(stringResource(Res.string.news_tags_loading))
                    }
                    state.initializationFailed -> item(key = "initialization-error", contentType = "status") {
                        TagEditorError(
                            stringResource(Res.string.news_tags_initialization_failed),
                            onRetryInitialization,
                        )
                    }
                    else -> {
                        item(key = "my-tags-heading", contentType = "heading") {
                            TagEditorHeading(
                                stringResource(Res.string.news_tags_my, tagCount),
                            )
                        }
                        itemsIndexed(
                            userTags,
                            key = { _, tag -> tag.id },
                            contentType = { _, _ -> "subscribed-tag" },
                        ) { index, tag ->
                            TagEditorSubscribedItem(
                                tag = tag,
                                index = index,
                                tagCount = tagCount,
                                isMissing = tag.id in missingSubscribedIds,
                                reorderableState = reorderableState,
                                dragEnabled = canEdit && tagCount > 1 &&
                                    (draggingTagId == null || draggingTagId == tag.id),
                                canAddOrRemove = canAddOrRemove,
                                keepActionColors = keepActionColors,
                                onSelect = onSelect,
                                onRemove = onRemove,
                                onDragStarted = onDragStarted,
                                onDragStopped = onDragStopped,
                                onMoveBy = onMoveBy,
                            )
                        }
                        item(key = "more-tags-heading", contentType = "heading") {
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                color = colors.outlineVariant,
                            )
                            TagEditorMoreTagsHeader(
                                isRefreshing = isRefreshing,
                                canRefresh = canRefresh,
                                onRefresh = onRefresh,
                            )
                        }
                        if (state.refreshError != null) {
                            item(key = "refresh-error", contentType = "status") {
                                TagEditorError(
                                    message = stringResource(Res.string.news_tags_refresh_failed),
                                    onRetry = onRefresh,
                                    enabled = canRefresh,
                                )
                            }
                        }
                        if (availableTagRows.isEmpty()) {
                            item(key = "available-tags-empty", contentType = "status") {
                                Text(
                                    stringResource(Res.string.news_tags_all_added),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        } else {
                            items(
                                availableTagRows,
                                key = { tags -> "available-row-${tags.first().id}" },
                                contentType = { "available-tag-row" },
                            ) { tags ->
                                TagEditorAvailablePair(
                                    firstTag = tags[0],
                                    secondTag = tags.getOrNull(1),
                                    canAdd = canAddOrRemove,
                                    onAdd = onAdd,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagEditorMoreTagsHeader(
    isRefreshing: Boolean,
    canRefresh: Boolean,
    onRefresh: () -> Unit,
) {
    val refreshLabel = stringResource(
        if (isRefreshing) Res.string.news_tags_refreshing else Res.string.news_tags_refresh,
    )
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp).padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.news_tags_more),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(
            onClick = onRefresh,
            enabled = canRefresh,
            modifier = Modifier.size(48.dp).semantics { contentDescription = refreshLabel },
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    AppIcon.Sync.imageVector,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun TagEditorHeader(isSaving: Boolean, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.news_manage_tags),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TagEditorSavingStatus(isSaving)
        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(
                AppIcon.Close.imageVector,
                contentDescription = stringResource(Res.string.news_tags_close),
            )
        }
    }
}

@Composable
private fun TagEditorSavingStatus(isSaving: Boolean) {
    val showSaving by produceState(initialValue = false, key1 = isSaving) {
        value = false
        if (isSaving) {
            delay(200L)
            value = true
        }
    }
    // Keep the slot fixed so fast writes don't flash or move the title.
    Box(
        modifier = Modifier.width(64.dp).height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isSaving && showSaving) {
            Text(
                stringResource(Res.string.news_tags_saving),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LazyItemScope.TagEditorSubscribedItem(
    tag: TagInfo,
    index: Int,
    tagCount: Int,
    isMissing: Boolean,
    reorderableState: ReorderableLazyListState,
    dragEnabled: Boolean,
    canAddOrRemove: Boolean,
    keepActionColors: Boolean,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onDragStarted: (Int) -> Unit,
    onDragStopped: () -> Unit,
    onMoveBy: (Int, Int) -> Unit,
) {
    val moveUpLabel = stringResource(Res.string.news_tags_move_up)
    val moveDownLabel = stringResource(Res.string.news_tags_move_down)
    ReorderableItem(reorderableState, key = tag.id) { isDragging ->
        val reorderLabel = stringResource(Res.string.news_tags_reorder, tag.title)
        val dragModifier = Modifier
            .size(48.dp)
            .draggableHandle(
                enabled = dragEnabled,
                onDragStarted = { onDragStarted(tag.id) },
                onDragStopped = onDragStopped,
            )
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || !canAddOrRemove) {
                    false
                } else {
                    when (event.key) {
                        Key.DirectionUp -> {
                            if (index > 0) onMoveBy(tag.id, -1)
                            true
                        }
                        Key.DirectionDown -> {
                            if (index < tagCount - 1) onMoveBy(tag.id, 1)
                            true
                        }
                        else -> false
                    }
                }
            }
            .focusable(enabled = dragEnabled)
            .semantics {
                contentDescription = reorderLabel
                if (!dragEnabled) disabled()
                customActions = buildList {
                    if (canAddOrRemove && index > 0) {
                        add(CustomAccessibilityAction(moveUpLabel) {
                            onMoveBy(tag.id, -1)
                            true
                        })
                    }
                    if (canAddOrRemove && index < tagCount - 1) {
                        add(CustomAccessibilityAction(moveDownLabel) {
                            onMoveBy(tag.id, 1)
                            true
                        })
                    }
                }
            }
        TagEditorRow(
            tag = tag,
            isMissing = isMissing,
            isDragging = isDragging,
            canSelect = canAddOrRemove && !isDragging,
            canRemove = canAddOrRemove && tagCount > 1,
            dragEnabled = dragEnabled,
            keepActionColors = keepActionColors,
            dragModifier = dragModifier,
            onSelect = { onSelect(tag.id) },
            onRemove = { onRemove(tag.id) },
        )
    }
}

@Composable
private fun TagEditorAvailablePair(
    firstTag: TagInfo,
    secondTag: TagInfo?,
    canAdd: Boolean,
    onAdd: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        key(firstTag.id) {
            TagEditorAvailableRow(
                tag = firstTag,
                canAdd = canAdd,
                onAdd = onAdd,
                modifier = Modifier.weight(1f),
            )
        }
        if (secondTag != null) {
            key(secondTag.id) {
                TagEditorAvailableRow(
                    tag = secondTag,
                    canAdd = canAdd,
                    onAdd = onAdd,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun TagEditorAvailableRow(
    tag: TagInfo,
    canAdd: Boolean,
    onAdd: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(56.dp).padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            tag.title,
            modifier = Modifier.weight(1f).padding(end = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(
            onClick = { onAdd(tag.id) },
            enabled = canAdd,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                AppIcon.Add.imageVector,
                contentDescription = stringResource(Res.string.news_tags_add, tag.title),
            )
        }
    }
}

@Composable
private fun TagEditorRow(
    tag: TagInfo,
    isMissing: Boolean,
    isDragging: Boolean,
    canSelect: Boolean,
    canRemove: Boolean,
    dragEnabled: Boolean,
    keepActionColors: Boolean,
    dragModifier: Modifier,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp)
            .background(if (isDragging) colors.surfaceContainerHighest else colors.surfaceContainerLow),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Keep the action buttons outside the selection hit area, including a handle tap.
        Row(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .clickable(enabled = canSelect, role = Role.Button, onClick = onSelect)
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TagEditorImage(tag)
            Column(
                modifier = Modifier.weight(1f).padding(start = 12.dp, end = 4.dp),
            ) {
                Text(
                    tag.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isMissing) {
                    Text(
                        stringResource(Res.string.news_tags_missing),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        IconButton(
            onClick = onRemove,
            enabled = canRemove,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                AppIcon.Close.imageVector,
                contentDescription = stringResource(Res.string.news_tags_remove, tag.title),
                tint = if (canRemove || keepActionColors) {
                    colors.onSurfaceVariant
                } else {
                    colors.onSurface.copy(alpha = 0.38f)
                },
            )
        }
        Box(modifier = dragModifier, contentAlignment = Alignment.Center) {
            Icon(
                AppIcon.DragHandle.imageVector,
                contentDescription = null,
                tint = if (dragEnabled || keepActionColors) {
                    colors.onSurfaceVariant
                } else {
                    colors.onSurface.copy(alpha = 0.38f)
                },
            )
        }
    }
}

@Composable
private fun TagEditorImage(tag: TagInfo) {
    Box(
        modifier = Modifier.size(32.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(tag.title.take(1), style = MaterialTheme.typography.labelMedium)
        if (tag.iconUrl.isNotBlank()) {
            AsyncImage(
                model = tag.iconUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun TagEditorHeading(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TagEditorProgress(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TagEditorError(
    message: String,
    onRetry: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry, enabled = enabled) { Text(stringResource(Res.string.news_retry)) }
    }
}
