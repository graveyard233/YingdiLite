package com.graveyard.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.graveyard.core.designsystem.icons.AppIcon

@Composable
fun AppSearchToolbar(
    placeholder: String,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    searchTrailingContent: @Composable RowScope.() -> Unit = {},
    leadingContent: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val searchColor = colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.6f)

    Surface(modifier = modifier, color = colors.surfaceContainerLowest) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 64.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leadingContent()
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = CircleShape,
                        color = colors.surfaceContainerLow,
                        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable(
                                    enabled = enabled,
                                    role = Role.Button,
                                    onClick = onSearchClick,
                                )
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(start = 16.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = AppIcon.Search.imageVector,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = searchColor,
                            )
                            Text(
                                text = placeholder,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = searchColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            searchTrailingContent()
                        }
                    }
                }
                actions()
            }
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.2f))
        }
    }
}
