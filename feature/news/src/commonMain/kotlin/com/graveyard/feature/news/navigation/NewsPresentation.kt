package com.graveyard.feature.news.navigation

import androidx.compose.runtime.staticCompositionLocalOf

/** Shared presentation contract; the app owns window/scene decisions. */
enum class NewsLayoutMode { Feed, ReadingSingle, ReadingDual, ReadingTriple }

data class NewsPresentation(
    val layoutMode: NewsLayoutMode = NewsLayoutMode.Feed,
    val selectedArticleId: String? = null,
    val listVisible: Boolean = true,
    val interactive: Boolean = true,
    // Pane dimensions relative to the article container, in dp (including hinge gaps).
    val detailWidth: Float = 0f,
    val commentsStart: Float = 0f,
) {
    val panesSideBySide: Boolean
        get() = layoutMode == NewsLayoutMode.ReadingDual || layoutMode == NewsLayoutMode.ReadingTriple
}

val LocalNewsPresentation = staticCompositionLocalOf { NewsPresentation() }
