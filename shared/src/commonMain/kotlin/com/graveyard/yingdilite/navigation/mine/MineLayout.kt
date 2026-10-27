package com.graveyard.yingdilite.navigation.mine

import androidx.compose.ui.unit.LayoutDirection
import com.graveyard.yingdilite.navigation.PaneBounds
import com.graveyard.yingdilite.navigation.availablePaneIntervals

internal data class MineLayout(
    val single: PaneBounds,
    val login: PaneBounds? = null,
    val agreement: PaneBounds? = null,
    val showsNavigation: Boolean,
)

internal object MinePaneMinimums {
    const val Login = 360f
    const val Agreement = 480f
    const val WithNavigation = 320f
    const val MaximumLogin = 480f
    const val Margin = 12f
    const val Gap = 16f
}

/** Reserve the potential rail first; reclaim its space if the remaining partition is too narrow. */
internal fun calculateMineLayout(
    width: Float,
    height: Float,
    navigationWidth: Float,
    showsNavigation: Boolean,
    direction: LayoutDirection,
    verticalHinges: List<PaneBounds>,
    horizontalHinges: List<PaneBounds>,
): MineLayout {
    fun measure(reserveNavigation: Boolean) = measureMineLayout(
        width = width,
        height = height,
        navigationWidth = if (reserveNavigation) navigationWidth else 0f,
        showsNavigation = reserveNavigation,
        direction = direction,
        verticalHinges = verticalHinges,
        horizontalHinges = horizontalHinges,
    )
    val candidate = measure(showsNavigation)
    return if (showsNavigation && candidate.single.width < MinePaneMinimums.WithNavigation) {
        measure(reserveNavigation = false)
    } else candidate
}

private fun measureMineLayout(
    width: Float,
    height: Float,
    navigationWidth: Float,
    showsNavigation: Boolean,
    direction: LayoutDirection,
    verticalHinges: List<PaneBounds>,
    horizontalHinges: List<PaneBounds>,
): MineLayout {
    val w = (width - navigationWidth).coerceAtLeast(0f)
    val h = height.coerceAtLeast(0f)
    val origin = if (direction == LayoutDirection.Ltr) navigationWidth else 0f
    val columns = availablePaneIntervals(w, verticalHinges.map { it.left - origin to it.right - origin })
        .let { if (direction == LayoutDirection.Rtl) it.reversed() else it }
    val band = availablePaneIntervals(h, horizontalHinges.map { it.top to it.bottom })
        .maxByOrNull { it.second - it.first } ?: (0f to 0f)
    val widest = columns.maxByOrNull { it.second - it.first } ?: (0f to 0f)
    fun bounds(interval: Pair<Float, Float>) =
        PaneBounds(interval.first, band.first, interval.second - interval.first, band.second - band.first)
    val single = bounds(widest)
    // Prefer separate physical panels when the hinge divides the window.
    for (i in columns.indices) {
        if (columns[i].second - columns[i].first < MinePaneMinimums.Login) continue
        val supporting = columns.drop(i + 1).firstOrNull { it.second - it.first >= MinePaneMinimums.Agreement }
        if (supporting != null) {
            return MineLayout(single, bounds(columns[i]), bounds(supporting), showsNavigation)
        }
    }
    val dualMinimum = MinePaneMinimums.Login + MinePaneMinimums.Agreement +
        MinePaneMinimums.Margin * 2 + MinePaneMinimums.Gap
    // Otherwise split one uninterrupted region: 360 + 480 + 24 + 16 = 880dp.
    if (single.width >= dualMinimum) {
        val available = single.width - MinePaneMinimums.Margin * 2 - MinePaneMinimums.Gap
        val loginWidth = (available * 0.38f).coerceIn(MinePaneMinimums.Login, MinePaneMinimums.MaximumLogin)
        val agreementWidth = available - loginWidth
        val start = single.left + MinePaneMinimums.Margin
        val loginLeft = if (direction == LayoutDirection.Ltr) start else start + agreementWidth + MinePaneMinimums.Gap
        val agreementLeft = if (direction == LayoutDirection.Ltr) start + loginWidth + MinePaneMinimums.Gap else start
        return MineLayout(
            single,
            PaneBounds(loginLeft, single.top, loginWidth, single.height),
            PaneBounds(agreementLeft, single.top, agreementWidth, single.height),
            showsNavigation,
        )
    }
    return MineLayout(single = single, showsNavigation = showsNavigation)
}
