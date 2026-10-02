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
public val community: ImageVector
    get() {
        if (_community != null) {
            return _community!!
        }
        _community =
            ImageVector.Builder(
                name = "community",
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
                        moveTo(22f, 22f)
                        lineTo(18f, 18f)
                        horizontalLineTo(8f)
                        quadTo(7.18f, 18f, 6.59f, 17.41f)
                        reflectiveQuadTo(6f, 16f)
                        verticalLineTo(15f)
                        horizontalLineTo(17f)
                        quadToRelative(0.82f, 0f, 1.41f, -0.59f)
                        reflectiveQuadTo(19f, 13f)
                        verticalLineTo(6f)
                        horizontalLineToRelative(1f)
                        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                        quadTo(22f, 7.18f, 22f, 8f)
                        verticalLineTo(22f)
                        close()
                        moveTo(4f, 12.18f)
                        lineTo(5.18f, 11f)
                        horizontalLineTo(15f)
                        verticalLineTo(4f)
                        horizontalLineTo(4f)
                        verticalLineToRelative(8.17f)
                        close()
                        moveTo(2f, 17f)
                        verticalLineTo(4f)
                        quadTo(2f, 3.17f, 2.59f, 2.59f)
                        reflectiveQuadTo(4f, 2f)
                        horizontalLineTo(15f)
                        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                        reflectiveQuadTo(17f, 4f)
                        verticalLineToRelative(7f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(15f, 13f)
                        horizontalLineTo(6f)
                        lineTo(2f, 17f)
                        close()
                        moveTo(4f, 11f)
                        verticalLineTo(4f)
                        verticalLineToRelative(7f)
                        close()
                    }
                }
                .build()
        return _community!!
    }

private var _community: ImageVector? = null


@Suppress("CheckReturnValue")
public val communityFill: ImageVector
    get() {
        if (_communityFill != null) {
            return _communityFill!!
        }
        _communityFill =
            ImageVector.Builder(
                name = "communityFill",
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
                        moveTo(7f, 18f)
                        quadTo(6.58f, 18f, 6.29f, 17.71f)
                        quadTo(6f, 17.43f, 6f, 17f)
                        verticalLineTo(15f)
                        horizontalLineTo(19f)
                        verticalLineTo(6f)
                        horizontalLineToRelative(2f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(22f, 7f)
                        verticalLineTo(22f)
                        lineTo(18f, 18f)
                        horizontalLineTo(7f)
                        close()
                        moveTo(2f, 17f)
                        verticalLineTo(3f)
                        quadTo(2f, 2.57f, 2.29f, 2.29f)
                        reflectiveQuadTo(3f, 2f)
                        horizontalLineTo(16f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(17f, 3f)
                        verticalLineToRelative(9f)
                        quadToRelative(0f, 0.42f, -0.29f, 0.71f)
                        reflectiveQuadTo(16f, 13f)
                        horizontalLineTo(6f)
                        lineTo(2f, 17f)
                        close()
                    }
                }
                .build()
        return _communityFill!!
    }

private var _communityFill: ImageVector? = null