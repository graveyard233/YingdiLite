package com.graveyard.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

internal val searchIcon = toolbarIcon(
    "Search",
    "M9.5,3a6.5,6.5 0,1 0,4.02,11.6L19,20.08 20.08,19 14.6,13.52A6.5,6.5 0,0 0,9.5,3Z" +
        "M9.5,5a4.5,4.5 0,1 1,0,9 4.5,4.5 0,0 1,0,-9Z",
)

internal val tuneIcon = toolbarIcon(
    "Tune",
    "M3,6h4V4h2v6H7V8H3Z M11,6h10v2H11Z " +
        "M3,11h10v2H3Z M15,9h2v2h4v2h-4v2h-2Z " +
        "M3,16h4v-2h2v6H7v-2H3Z M11,16h10v2H11Z",
)

internal val notificationsIcon = toolbarIcon(
    "Notifications",
    "M12,22a2,2 0,0 0,2,-2h-4a2,2 0,0 0,2,2Z " +
        "M18,16V10a6,6 0,0 0,-5,-5.91V2h-2v2.09A6,6 0,0 0,6,10v6l-2,2v1h16v-1Z " +
        "M8,17V10a4,4 0,0 1,8,0v7Z",
)

internal val commentIcon = toolbarIcon(
    "Comment",
    "M4,3h16a2,2 0,0 1,2,2v12a2,2 0,0 1,-2,2H6l-4,4V5a2,2 0,0 1,2,-2Z " +
        "M4,5v13.17L5.17,17H20V5Z",
)

internal val imageIcon = toolbarIcon(
    "Image",
    "M5,3h14a2,2 0,0 1,2,2v14a2,2 0,0 1,-2,2H5a2,2 0,0 1,-2,-2V5a2,2 0,0 1,2,-2Z " +
        "M5,5v14h14V5Z M6,17l4,-5 3,3 2,-3 3,5Z M8.5,6.5a1.5,1.5 0,1 1,0,3 1.5,1.5 0,0 1,0,-3Z",
)

internal val refreshIcon = toolbarIcon(
    "Refresh",
    "M17.65,6.35A8,8 0,1 0,20,12H18a6,6 0,1 1,-1.76,-4.24L13,11h8V3Z",
)

private fun toolbarIcon(name: String, path: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(path),
        pathFillType = PathFillType.EvenOdd,
        fill = SolidColor(Color.Black),
    ).build()
