package com.graveyard.yingdilite.navigation.news

import com.graveyard.feature.news.navigation.NewsLayoutMode

/** All coordinates are dp relative to the safe content area AFTER the application rail. */
internal data class PaneBounds(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right get() = left + width
    val bottom get() = top + height
}

internal data class NewsLayout(
    val mode: NewsLayoutMode,
    val list: PaneBounds? = null,
    val detail: PaneBounds? = null,
    val comments: PaneBounds? = null,
) {
    val showsNavigation get() = mode != NewsLayoutMode.ReadingSingle
    val article: PaneBounds?
        get() = detail?.let { it.copy(width = (comments?.right ?: it.right) - it.left) }
}

internal object NewsPaneMinimums {
    const val List = 320f
    const val Detail = 480f
    const val TripleDetail = 600f
    const val Comments = 320f
    const val Margin = 12f
    const val DualGap = 16f
    const val TripleGap = 8f
}

/**
 * Pure geometry policy. Separating/occluding hinges are exclusions, not just subtracted widths.
 * Horizontal folds choose the tallest usable band; vertical folds may separate adjacent panes.
 * A single pane is confined to one physical partition, never stretched across a hinge.
 */
internal fun calculateNewsLayout(
    width: Float,
    height: Float,
    reading: Boolean,
    navigationWidth: Float = 0f,
    verticalHinges: List<PaneBounds> = emptyList(),
    horizontalHinges: List<PaneBounds> = emptyList(),
    forceSingle: Boolean = false,
): NewsLayout {
    val w = (width - navigationWidth).coerceAtLeast(0f)
    val h = height.coerceAtLeast(0f)
    val columns = partitions(w, verticalHinges.map { (it.left - navigationWidth) to (it.right - navigationWidth) })
    val band = partitions(h, horizontalHinges.map { it.top to it.bottom }).maxByOrNull { it.second - it.first }
        ?: (0f to 0f)
    val widest = columns.maxByOrNull { it.second - it.first } ?: (0f to 0f)
    fun single(mode: NewsLayoutMode): NewsLayout {
        val available = (widest.second - widest.first).coerceAtLeast(0f)
        val contentWidth = if (reading) available else minOf(available, 760f)
        val bounds = PaneBounds(widest.first + (available - contentWidth) / 2, band.first, contentWidth, band.second - band.first)
        return if (reading) NewsLayout(mode, detail = bounds) else NewsLayout(mode, list = bounds)
    }
    if (!reading) return single(NewsLayoutMode.Feed)
    if (forceSingle) return single(NewsLayoutMode.ReadingSingle)
    val usable = columns.map { (start, end) ->
        (start + NewsPaneMinimums.Margin) to (end - NewsPaneMinimums.Margin)
    }.filter { it.second > it.first }
    fun place(widths: List<Float>, gap: Float): List<PaneBounds>? {
        val panes = mutableListOf<PaneBounds>()
        var column = 0
        var x = usable.firstOrNull()?.first ?: return null
        for (paneWidth in widths) {
            while (column < usable.size && x + paneWidth > usable[column].second + 0.01f) {
                column++
                x = usable.getOrNull(column)?.first ?: return null
            }
            panes += PaneBounds(x, band.first, paneWidth, band.second - band.first)
            x += paneWidth + gap
        }
        return panes
    }
    fun readingPanes(triple: Boolean): List<PaneBounds>? {
        val minimum = if (triple) NewsPaneMinimums.TripleDetail else NewsPaneMinimums.Detail
        val gap = if (triple) NewsPaneMinimums.TripleGap else NewsPaneMinimums.DualGap
        fun widths(detail: Float, comments: Float) =
            if (triple) listOf(NewsPaneMinimums.List, detail, comments) else listOf(detail, comments)
        if (place(widths(minimum, NewsPaneMinimums.Comments), gap) == null) return null
        // Give the article surplus space first, up to a comfortable reading width.
        var low = minimum
        var high = 760f
        repeat(16) {
            val middle = (low + high) / 2
            if (place(widths(middle, NewsPaneMinimums.Comments), gap) != null) low = middle else high = middle
        }
        return place(widths(low, NewsPaneMinimums.Comments), gap)
    }
    readingPanes(true)?.let { return NewsLayout(NewsLayoutMode.ReadingTriple, it[0], it[1], it[2]) }
    readingPanes(false)?.let { return NewsLayout(NewsLayoutMode.ReadingDual, detail = it[0], comments = it[1]) }
    return single(NewsLayoutMode.ReadingSingle)
}

internal fun calculateNewsSingleLayout(
    width: Float, height: Float, vertical: List<PaneBounds>, horizontal: List<PaneBounds>,
) = calculateNewsLayout(width, height, reading = true, verticalHinges = vertical,
    horizontalHinges = horizontal, forceSingle = true)

private fun partitions(size: Float, exclusions: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
    var start = 0f
    val result = mutableListOf<Pair<Float, Float>>()
    for ((left, right) in exclusions.sortedBy { it.first }) {
        val a = left.coerceIn(0f, size)
        val b = right.coerceIn(a, size)
        if (a > start) result += start to a
        start = maxOf(start, b)
    }
    if (start < size) result += start to size
    return result
}
