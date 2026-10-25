package com.graveyard.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

internal val sortAscending = sortIcon(
    "SortAscending",
    "M4 8 L8 4 L12 8 M8 4 V20 M15 6 H17 M15 12 H19 M15 18 H21",
)

internal val sortDescending = sortIcon(
    "SortDescending",
    "M4 16 L8 20 L12 16 M8 4 V20 M15 6 H21 M15 12 H19 M15 18 H17",
)

private fun sortIcon(name: String, path: String): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes(path),
    fill = null,
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 1.8f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
).build()
