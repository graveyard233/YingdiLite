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
public val thumbDown: ImageVector
    get() {
        if (_thumbDown != null) {
            return _thumbDown!!
        }
        _thumbDown =
            ImageVector.Builder(
                name = "thumb_down",
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
                        moveTo(6f, 3f)
                        horizontalLineTo(17f)
                        verticalLineTo(16f)
                        lineToRelative(-7f, 7f)
                        lineTo(8.75f, 21.75f)
                        quadTo(8.58f, 21.58f, 8.46f, 21.28f)
                        reflectiveQuadTo(8.35f, 20.7f)
                        verticalLineTo(20.35f)
                        lineTo(9.45f, 16f)
                        horizontalLineTo(3f)
                        quadTo(2.2f, 16f, 1.6f, 15.4f)
                        reflectiveQuadTo(1f, 14f)
                        verticalLineTo(12f)
                        quadTo(1f, 11.83f, 1.05f, 11.63f)
                        reflectiveQuadToRelative(0.1f, -0.38f)
                        lineToRelative(3f, -7.05f)
                        quadTo(4.38f, 3.7f, 4.9f, 3.35f)
                        reflectiveQuadTo(6f, 3f)
                        close()
                        moveToRelative(9f, 2f)
                        horizontalLineTo(6f)
                        lineTo(3f, 12f)
                        verticalLineToRelative(2f)
                        horizontalLineToRelative(9f)
                        lineToRelative(-1.35f, 5.5f)
                        lineTo(15f, 15.15f)
                        verticalLineTo(5f)
                        close()
                        moveToRelative(0f, 10.15f)
                        verticalLineTo(14f)
                        verticalLineTo(12f)
                        verticalLineTo(5f)
                        verticalLineTo(15.15f)
                        close()
                        moveTo(17f, 16f)
                        verticalLineTo(14f)
                        horizontalLineToRelative(3f)
                        verticalLineTo(5f)
                        horizontalLineTo(17f)
                        verticalLineTo(3f)
                        horizontalLineToRelative(5f)
                        verticalLineTo(16f)
                        horizontalLineTo(17f)
                        close()
                    }
                }
                .build()
        return _thumbDown!!
    }

private var _thumbDown: ImageVector? = null


@Suppress("CheckReturnValue")
public val thumbDownFill: ImageVector
    get() {
        if (_thumbDownFill != null) {
            return _thumbDownFill!!
        }
        _thumbDownFill =
            ImageVector.Builder(
                name = "thumb_down",
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
                        moveTo(6f, 3f)
                        horizontalLineTo(16f)
                        verticalLineTo(16f)
                        lineTo(9f, 23f)
                        lineTo(7.75f, 21.75f)
                        quadTo(7.58f, 21.58f, 7.46f, 21.28f)
                        reflectiveQuadTo(7.35f, 20.7f)
                        verticalLineTo(20.35f)
                        lineTo(8.45f, 16f)
                        horizontalLineTo(3f)
                        quadTo(2.2f, 16f, 1.6f, 15.4f)
                        reflectiveQuadTo(1f, 14f)
                        verticalLineTo(12f)
                        quadTo(1f, 11.83f, 1.04f, 11.63f)
                        reflectiveQuadTo(1.15f, 11.25f)
                        lineToRelative(3f, -7.05f)
                        quadTo(4.38f, 3.7f, 4.9f, 3.35f)
                        reflectiveQuadTo(6f, 3f)
                        close()
                        moveTo(18f, 16f)
                        verticalLineTo(3f)
                        horizontalLineToRelative(4f)
                        verticalLineTo(16f)
                        horizontalLineTo(18f)
                        close()
                    }
                }
                .build()
        return _thumbDownFill!!
    }

private var _thumbDownFill: ImageVector? = null