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
    "M6,14H18V12H6v2Z M6,11H18V9H6v2Z M6,8H18V6H6v2Z " +
        "M22,22L18,18H4Q3.18,18 2.59,17.41T2,16V4Q2,3.17 2.59,2.59T4,2H20q0.83,0 1.41,0.59T22,4V22Z " +
        "M4,16H18.85L20,17.13V4H4V16Z m0,0V4V16Z",
)

internal val editIcon = toolbarIcon(
    "Edit",
    "M5,19H6.43L16.2,9.23L14.78,7.8L5,17.58V19Z " +
        "M3,21V16.75L16.2,3.57Q16.5,3.3 16.86,3.15T17.63,3q0.4,0 0.78,0.15T19.05,3.6L20.43,5 " +
        "q0.3,0.27 0.44,0.65T21,6.4q0,0.4 -0.14,0.76T20.43,7.82L7.25,21H3Z " +
        "M19,6.4L17.6,5L19,6.4Z M15.48,8.52L14.78,7.8L16.2,9.23L15.48,8.52Z",
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

// Google Material Icons, 24px (Apache 2.0).
internal val addIcon = toolbarIcon(
    "Add",
    "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z",
)

internal val closeIcon = toolbarIcon(
    "Close",
    "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 " +
        "5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z",
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
