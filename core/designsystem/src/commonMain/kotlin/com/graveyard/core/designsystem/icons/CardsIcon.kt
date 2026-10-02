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
public val cards: ImageVector
    get() {
        if (_cards != null) {
            return _cards!!
        }
        _cards =
            ImageVector.Builder(
                name = "cards",
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
                        moveTo(15.2f, 14.8f)
                        lineToRelative(1.15f, -4.15f)
                        lineTo(12.8f, 8.2f)
                        lineToRelative(-1.15f, 4.15f)
                        lineTo(15.2f, 14.8f)
                        close()
                        moveTo(4f, 18.83f)
                        lineTo(3.18f, 18.43f)
                        quadTo(2.4f, 18.1f, 2.13f, 17.31f)
                        reflectiveQuadTo(2.2f, 15.75f)
                        lineTo(4f, 11.85f)
                        verticalLineToRelative(6.98f)
                        close()
                        moveTo(8f, 21f)
                        quadTo(7.18f, 21f, 6.59f, 20.4f)
                        reflectiveQuadTo(6f, 18.98f)
                        verticalLineTo(13f)
                        lineToRelative(2.68f, 7.35f)
                        quadToRelative(0.07f, 0.17f, 0.13f, 0.34f)
                        reflectiveQuadTo(8.98f, 21f)
                        horizontalLineTo(8f)
                        close()
                        moveToRelative(5.15f, -0.13f)
                        quadTo(12.38f, 21.15f, 11.6f, 20.8f)
                        reflectiveQuadTo(10.55f, 19.68f)
                        lineTo(6.13f, 7.45f)
                        quadTo(5.85f, 6.68f, 6.2f, 5.91f)
                        reflectiveQuadTo(7.33f, 4.88f)
                        lineTo(14.85f, 2.13f)
                        quadTo(15.63f, 1.85f, 16.39f, 2.2f)
                        reflectiveQuadToRelative(1.04f, 1.13f)
                        lineToRelative(4.45f, 12.23f)
                        quadToRelative(0.28f, 0.78f, -0.07f, 1.54f)
                        reflectiveQuadToRelative(-1.13f, 1.04f)
                        lineToRelative(-7.53f, 2.75f)
                        close()
                        moveTo(12.45f, 19f)
                        lineTo(20f, 16.25f)
                        lineTo(15.53f, 4f)
                        lineTo(8f, 6.75f)
                        lineTo(12.45f, 19f)
                        close()
                        moveTo(14f, 11.5f)
                        close()
                    }
                }
                .build()
        return _cards!!
    }

private var _cards: ImageVector? = null


@Suppress("CheckReturnValue")
public val cardsFill: ImageVector
    get() {
        if (_cardsFill != null) {
            return _cardsFill!!
        }
        _cardsFill =
            ImageVector.Builder(
                name = "cardsFill",
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
                        moveTo(15.2f, 14.8f)
                        lineToRelative(1.15f, -4.15f)
                        lineTo(12.8f, 8.2f)
                        lineToRelative(-1.15f, 4.15f)
                        lineTo(15.2f, 14.8f)
                        close()
                        moveTo(4f, 18.83f)
                        lineTo(3.18f, 18.43f)
                        quadTo(2.4f, 18.1f, 2.13f, 17.31f)
                        reflectiveQuadTo(2.2f, 15.75f)
                        lineTo(4f, 11.85f)
                        verticalLineToRelative(6.98f)
                        close()
                        moveTo(8f, 21f)
                        quadTo(7.18f, 21f, 6.59f, 20.4f)
                        reflectiveQuadTo(6f, 18.98f)
                        verticalLineTo(13f)
                        lineToRelative(2.68f, 7.35f)
                        quadToRelative(0.07f, 0.17f, 0.13f, 0.34f)
                        reflectiveQuadTo(8.98f, 21f)
                        horizontalLineTo(8f)
                        close()
                        moveToRelative(5.15f, -0.13f)
                        quadTo(12.38f, 21.15f, 11.6f, 20.8f)
                        reflectiveQuadTo(10.55f, 19.68f)
                        lineTo(6.13f, 7.45f)
                        quadTo(5.85f, 6.68f, 6.2f, 5.91f)
                        reflectiveQuadTo(7.33f, 4.88f)
                        lineTo(14.85f, 2.13f)
                        quadTo(15.63f, 1.85f, 16.39f, 2.2f)
                        reflectiveQuadToRelative(1.04f, 1.13f)
                        lineToRelative(4.45f, 12.23f)
                        quadToRelative(0.28f, 0.78f, -0.07f, 1.54f)
                        reflectiveQuadToRelative(-1.13f, 1.04f)
                        lineToRelative(-7.53f, 2.75f)
                        close()
                    }
                }
                .build()
        return _cardsFill!!
    }

private var _cardsFill: ImageVector? = null