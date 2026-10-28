package com.graveyard.yingdilite.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import kotlin.math.roundToInt

/**
 * 按场景提供的 dp 边界绘制各 Entry；pane 的数量、组合和布局策略由调用方决定。
 * 边界使用场景内容区域内的物理坐标，调用方已处理 RTL，放置时无需再次镜像。
 */
@Composable
internal fun AdaptivePaneLayout(
    panes: List<Pair<NavEntry<NavKey>, PaneBounds>>,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            // 使用条目自身的标识，避免 pane 顺序变化时按索引复用其他条目的组合。
            panes.forEach { (entry, _) -> key(entry.contentKey) { Box { entry.Content() } } }
        },
    ) { measurables, constraints ->
        val placements = measurables.zip(panes).map { (measurable, pane) ->
            val bounds = pane.second
            // 先换算为像素，再限制到实际内容范围内，保留原有的边缘取整行为。
            val x = (bounds.left * density).roundToInt().coerceIn(0, constraints.maxWidth)
            val y = (bounds.top * density).roundToInt().coerceIn(0, constraints.maxHeight)
            val width = (bounds.width * density).roundToInt().coerceIn(0, constraints.maxWidth - x)
            val height = (bounds.height * density).roundToInt().coerceIn(0, constraints.maxHeight - y)
            Triple(measurable.measure(Constraints.fixed(width, height)), x, y)
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placements.forEach { (placeable, x, y) -> placeable.place(x, y) }
        }
    }
}
