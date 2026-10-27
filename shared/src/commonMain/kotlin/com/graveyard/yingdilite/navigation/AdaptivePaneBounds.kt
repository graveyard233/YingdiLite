package com.graveyard.yingdilite.navigation

/** Coordinates in dp relative to the safe scene content area, after application navigation. */
internal data class PaneBounds(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right get() = left + width
    val bottom get() = top + height
}

/** Physical intervals left after removing separating or occluding hinges. */
internal fun availablePaneIntervals(size: Float, exclusions: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
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
