package com.graveyard.yingdilite.navigation.auth

import androidx.compose.ui.unit.LayoutDirection
import com.graveyard.yingdilite.navigation.PaneBounds
import com.graveyard.yingdilite.navigation.availablePaneIntervals

internal data class LoginLayout(
    val single: PaneBounds,
    val login: PaneBounds? = null,
    val agreement: PaneBounds? = null,
    val showsNavigation: Boolean,
)

internal object LoginPaneMinimums {
    const val Login = 360f
    const val Agreement = 480f
    const val WithNavigation = 320f
    const val MaximumLogin = 480f
    const val Margin = 12f
    const val Gap = 16f
}

/** Reserve the potential rail first; reclaim its space if the remaining partition is too narrow. */
internal fun calculateLoginLayout(
    width: Float,
    height: Float,
    navigationWidth: Float,
    showsNavigation: Boolean,
    direction: LayoutDirection,
    verticalHinges: List<PaneBounds>,
    horizontalHinges: List<PaneBounds>,
): LoginLayout {
    fun measure(reserveNavigation: Boolean) = measureLoginLayout(
        width = width,
        height = height,
        navigationWidth = if (reserveNavigation) navigationWidth else 0f,
        showsNavigation = reserveNavigation,
        direction = direction,
        verticalHinges = verticalHinges,
        horizontalHinges = horizontalHinges,
    )
    val candidate = measure(showsNavigation)
    return if (showsNavigation && candidate.single.width < LoginPaneMinimums.WithNavigation) {
        measure(reserveNavigation = false)
    } else candidate
}

private fun measureLoginLayout(
    width: Float,
    height: Float,
    navigationWidth: Float,
    showsNavigation: Boolean,
    direction: LayoutDirection,
    verticalHinges: List<PaneBounds>,
    horizontalHinges: List<PaneBounds>,
): LoginLayout {
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
        if (columns[i].second - columns[i].first < LoginPaneMinimums.Login) continue
        val supporting = columns.drop(i + 1).firstOrNull { it.second - it.first >= LoginPaneMinimums.Agreement }
        if (supporting != null) {
            return LoginLayout(single, bounds(columns[i]), bounds(supporting), showsNavigation)
        }
    }
    val dualMinimum = LoginPaneMinimums.Login + LoginPaneMinimums.Agreement +
        LoginPaneMinimums.Margin * 2 + LoginPaneMinimums.Gap
    // Otherwise split one uninterrupted region: 360 + 480 + 24 + 16 = 880dp.
    if (single.width >= dualMinimum) {
        val available = single.width - LoginPaneMinimums.Margin * 2 - LoginPaneMinimums.Gap
        val loginWidth = (available * 0.38f).coerceIn(LoginPaneMinimums.Login, LoginPaneMinimums.MaximumLogin)
        val agreementWidth = available - loginWidth
        val start = single.left + LoginPaneMinimums.Margin
        val loginLeft = if (direction == LayoutDirection.Ltr) start else start + agreementWidth + LoginPaneMinimums.Gap
        val agreementLeft = if (direction == LayoutDirection.Ltr) start + loginWidth + LoginPaneMinimums.Gap else start
        return LoginLayout(
            single,
            PaneBounds(loginLeft, single.top, loginWidth, single.height),
            PaneBounds(agreementLeft, single.top, agreementWidth, single.height),
            showsNavigation,
        )
    }
    return LoginLayout(single = single, showsNavigation = showsNavigation)
}
