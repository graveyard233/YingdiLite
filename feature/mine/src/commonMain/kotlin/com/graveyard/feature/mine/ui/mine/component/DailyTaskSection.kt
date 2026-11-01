package com.graveyard.feature.mine.ui.mine.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.model.user.DailyTask
import com.graveyard.core.model.user.DailyTaskKind
import com.graveyard.feature.mine.viewmodel.DailyTasksUiState
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.mine.generated.resources.Res
import yingdilite.feature.mine.generated.resources.mine_daily_all_done
import yingdilite.feature.mine.generated.resources.mine_daily_done
import yingdilite.feature.mine.generated.resources.mine_daily_load_failed
import yingdilite.feature.mine.generated.resources.mine_daily_progress
import yingdilite.feature.mine.generated.resources.mine_daily_progress_label
import yingdilite.feature.mine.generated.resources.mine_daily_reset
import yingdilite.feature.mine.generated.resources.mine_daily_reward_campfire
import yingdilite.feature.mine.generated.resources.mine_daily_reward_credits
import yingdilite.feature.mine.generated.resources.mine_daily_task_check_in
import yingdilite.feature.mine.generated.resources.mine_daily_task_comment
import yingdilite.feature.mine.generated.resources.mine_daily_task_like
import yingdilite.feature.mine.generated.resources.mine_daily_task_read
import yingdilite.feature.mine.generated.resources.mine_daily_tasks
import yingdilite.feature.mine.generated.resources.mine_daily_todo

private const val PLACEHOLDER_ROWS = 4
private val TaskSymbolShape = RoundedCornerShape(11.dp)

/** 每日任务「紧凑清单」：只展示完成状态与奖励，不提供去完成入口。 */
@Composable
fun DailyTaskSection(state: DailyTasksUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val tasks = state.tasks
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.mine_daily_tasks),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(Res.string.mine_daily_reset),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            when {
                tasks != null -> {
                    DailyProgressHeader(completed = tasks.count(DailyTask::completed), total = tasks.size)
                    tasks.forEach { task ->
                        TaskDivider()
                        DailyTaskRow(task)
                    }
                }
                state.loadFailed -> LoadFailedRow(onRetry)
                else -> repeat(PLACEHOLDER_ROWS) { index ->
                    if (index > 0) TaskDivider()
                    PlaceholderRow()
                }
            }
        }
    }
}

@Composable
private fun DailyProgressHeader(completed: Int, total: Int) {
    val allDone = total > 0 && completed == total
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(if (allDone) Res.string.mine_daily_all_done else Res.string.mine_daily_progress_label),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.mine_daily_progress, completed, total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val fraction = if (total == 0) 0f else completed.toFloat() / total
        Box(
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(fraction).height(5.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun DailyTaskRow(task: DailyTask) {
    val title = when (task.kind) {
        DailyTaskKind.CheckIn -> stringResource(Res.string.mine_daily_task_check_in)
        DailyTaskKind.ReadArticle -> stringResource(Res.string.mine_daily_task_read)
        DailyTaskKind.LikeComment -> stringResource(Res.string.mine_daily_task_like)
        DailyTaskKind.PostComment -> stringResource(Res.string.mine_daily_task_comment)
        DailyTaskKind.Unknown -> task.title
    }
    val rewards = buildList {
        if (task.credits > 0) add(stringResource(Res.string.mine_daily_reward_credits, task.credits))
        if (task.campfire > 0) add(stringResource(Res.string.mine_daily_reward_campfire, task.campfire))
    }.joinToString(" · ")
    val symbol = when {
        task.completed -> AppIcon.Check
        task.kind == DailyTaskKind.CheckIn -> AppIcon.CalendarCheck
        task.kind == DailyTaskKind.ReadArticle -> AppIcon.News
        task.kind == DailyTaskKind.LikeComment -> AppIcon.Favorite
        task.kind == DailyTaskKind.PostComment -> AppIcon.Comment
        else -> AppIcon.Info
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            // 合并为一个节点，读屏按「标题、奖励、状态」连贯朗读。
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(TaskSymbolShape).background(
                if (task.completed) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = symbol.imageVector,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = if (task.completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (rewards.isNotEmpty()) {
                Text(
                    text = rewards,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = stringResource(if (task.completed) Res.string.mine_daily_done else Res.string.mine_daily_todo),
            style = MaterialTheme.typography.labelMedium,
            color = if (task.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadFailedRow(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onRetry)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AppIcon.Refresh.imageVector,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.mine_daily_load_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlaceholderRow() {
    val placeholder = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(TaskSymbolShape).background(placeholder))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(width = 96.dp, height = 14.dp).clip(CircleShape).background(placeholder))
            Box(Modifier.size(width = 64.dp, height = 10.dp).clip(CircleShape).background(placeholder))
        }
    }
}

@Composable
private fun TaskDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}
