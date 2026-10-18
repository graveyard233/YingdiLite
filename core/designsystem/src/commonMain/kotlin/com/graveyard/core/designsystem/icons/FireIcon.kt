package com.graveyard.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

val fire: ImageVector by lazy {
    fireIcon(
        name = "Fire",
        path = flameOutline +
            "M10.8,5.7C10.3,8.1 9,9.8 7.5,11.5C6.5,12.6 6,13.9 6,15.2" +
            "C6,18.1 8.6,20 12,20C15.4,20 18,17.7 18,14.7" +
            "C18,13.2 17.5,11.8 16.5,10.4C16.1,11.6 15.4,12.6 14.3,13.5" +
            "C14.6,10.4 13.5,7.6 10.8,5.7Z" + innerFlame,
    )
}

val fireFill: ImageVector by lazy {
    fireIcon(name = "FireFill", path = flameOutline + innerFlame)
}

// Both variants share the silhouette and inner flame; even-odd fill cuts out the cavities.
private const val flameOutline =
    "M10,2C10.4,5.7 8.6,7.6 6,10.3C4.7,11.7 4,13.4 4,15.2" +
        "C4,19.3 7.6,22 12,22C16.4,22 20,18.9 20,14.7" +
        "C20,11.9 18.6,9.5 16.3,7.2C16.3,8.7 15.9,10.1 15,11" +
        "C14.9,6.9 13.1,4 10,2Z"

private const val innerFlame =
    "M12.4,13.3C12.4,15.2 10,15.6 10,17.3C10,18.4 10.9,19.1 12,19.1" +
        "C13.4,19.1 14.3,18.1 14.3,16.8C14.3,15.6 13.6,14.3 12.4,13.3Z"

private fun fireIcon(name: String, path: String): ImageVector = ImageVector.Builder(
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
