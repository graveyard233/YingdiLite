package com.graveyard.core.designsystem.icons


import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val news: ImageVector
    get() {
        if (_news != null) {
            return _news!!
        }
        _news =
            ImageVector.Builder(
                name = "news",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.Companion.NonZero,
                    ) {
                        moveTo(5f, 21f)
                        quadTo(4.18f, 21f, 3.59f, 20.41f)
                        reflectiveQuadTo(3f, 19f)
                        verticalLineTo(5f)
                        quadTo(3f, 4.17f, 3.59f, 3.59f)
                        reflectiveQuadTo(5f, 3f)
                        horizontalLineTo(16f)
                        lineToRelative(5f, 5f)
                        verticalLineTo(19f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(19f, 21f)
                        horizontalLineTo(5f)
                        close()
                        moveTo(5f, 19f)
                        horizontalLineTo(19f)
                        verticalLineTo(9f)
                        horizontalLineTo(15f)
                        verticalLineTo(5f)
                        horizontalLineTo(5f)
                        verticalLineTo(19f)
                        close()
                        moveTo(7f, 17f)
                        horizontalLineTo(17f)
                        verticalLineTo(15f)
                        horizontalLineTo(7f)
                        verticalLineToRelative(2f)
                        close()
                        moveTo(7f, 9f)
                        horizontalLineToRelative(5f)
                        verticalLineTo(7f)
                        horizontalLineTo(7f)
                        verticalLineTo(9f)
                        close()
                        moveToRelative(0f, 4f)
                        horizontalLineTo(17f)
                        verticalLineTo(11f)
                        horizontalLineTo(7f)
                        verticalLineToRelative(2f)
                        close()
                        moveTo(5f, 5f)
                        verticalLineTo(9f)
                        verticalLineTo(5f)
                        verticalLineTo(9f)
                        verticalLineTo(19f)
                        verticalLineTo(5f)
                        close()
                    }
                }
                .build()
        return _news!!
    }

private var _news: ImageVector? = null




@Suppress("CheckReturnValue")
public val newsFill: ImageVector
    get() {
        if (_newsFill != null) {
            return _newsFill!!
        }
        _newsFill =
            ImageVector.Builder(
                name = "newsFill",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.Companion.NonZero,
                    ) {
                        moveTo(5f, 21f)
                        quadTo(4.18f, 21f, 3.59f, 20.41f)
                        reflectiveQuadTo(3f, 19f)
                        verticalLineTo(5f)
                        quadTo(3f, 4.17f, 3.59f, 3.59f)
                        reflectiveQuadTo(5f, 3f)
                        horizontalLineTo(16f)
                        lineToRelative(5f, 5f)
                        verticalLineTo(19f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(19f, 21f)
                        horizontalLineTo(5f)
                        close()
                        moveTo(7f, 17f)
                        horizontalLineTo(17f)
                        verticalLineTo(15f)
                        horizontalLineTo(7f)
                        verticalLineToRelative(2f)
                        close()
                        moveTo(7f, 13f)
                        horizontalLineTo(17f)
                        verticalLineTo(11f)
                        horizontalLineTo(7f)
                        verticalLineToRelative(2f)
                        close()
                        moveTo(15f, 9f)
                        horizontalLineToRelative(4f)
                        lineTo(15f, 5f)
                        verticalLineTo(9f)
                        close()
                        moveTo(7f, 9f)
                        horizontalLineToRelative(5f)
                        verticalLineTo(7f)
                        horizontalLineTo(7f)
                        verticalLineTo(9f)
                        close()
                    }
                }
                .build()
        return _newsFill!!
    }

private var _newsFill: ImageVector? = null