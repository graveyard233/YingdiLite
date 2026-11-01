package com.graveyard.core.designsystem.icons


import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Material Symbols `event_available`（Outlined），用于每日签到。 */
@Suppress("CheckReturnValue")
public val calendarCheck: ImageVector
    get() {
        if (_calendarCheck != null) {
            return _calendarCheck!!
        }
        _calendarCheck =
            ImageVector.Builder(
                name = "calendarCheck",
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
                        // 对勾
                        moveTo(10.95f, 18f)
                        lineTo(7.4f, 14.45f)
                        lineTo(8.85f, 13f)
                        lineTo(10.95f, 15.1f)
                        lineTo(15.15f, 10.9f)
                        lineTo(16.6f, 12.35f)
                        close()
                        // 日历外框与挂环
                        moveTo(5f, 22f)
                        quadToRelative(-0.83f, 0f, -1.41f, -0.59f)
                        quadTo(3f, 20.83f, 3f, 20f)
                        verticalLineTo(6f)
                        quadToRelative(0f, -0.83f, 0.59f, -1.41f)
                        quadTo(4.18f, 4f, 5f, 4f)
                        horizontalLineToRelative(1f)
                        verticalLineTo(2f)
                        horizontalLineToRelative(2f)
                        verticalLineToRelative(2f)
                        horizontalLineToRelative(8f)
                        verticalLineTo(2f)
                        horizontalLineToRelative(2f)
                        verticalLineToRelative(2f)
                        horizontalLineToRelative(1f)
                        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                        quadTo(21f, 5.18f, 21f, 6f)
                        verticalLineToRelative(14f)
                        quadToRelative(0f, 0.83f, -0.59f, 1.41f)
                        quadTo(19.83f, 22f, 19f, 22f)
                        close()
                        // 内部可书写区域（挖空）
                        moveTo(5f, 20f)
                        horizontalLineToRelative(14f)
                        verticalLineTo(10f)
                        horizontalLineTo(5f)
                        close()
                        moveTo(5f, 8f)
                        horizontalLineToRelative(14f)
                        verticalLineTo(6f)
                        horizontalLineTo(5f)
                        close()
                    }
                }
                .build()
        return _calendarCheck!!
    }

private var _calendarCheck: ImageVector? = null
