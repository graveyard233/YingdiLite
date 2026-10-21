package com.graveyard.yingdilite.navigation.news

import androidx.navigation3.runtime.NavKey
import com.graveyard.core.designsystem.navigation.Navigator
import com.graveyard.feature.news.navigation.ArticleRoute
import com.graveyard.feature.news.navigation.NewsRoute
import com.graveyard.yingdilite.navigation.ImageGalleryRoute
import kotlin.uuid.Uuid

/** Extension in the assembly layer avoids a core:designsystem -> feature:news dependency. */
internal fun Navigator.selectArticle(
    articleId: String,
    articleTitle: String,
    replyNum: String = "",
    likeNum: String = "",
) {
    if (state.currentTopLevelKey != NewsRoute) return
    replaceSelectedArticle(
        state.currentSubStack,
        ArticleRoute(
            articleId = articleId,
            articleTitle = articleTitle,
            replyNum = replyNum,
            likeNum = likeNum,
        ),
    )
}

/** Pushes an article opened from a card, preserving the source article in the back stack. */
internal fun Navigator.openLinkedArticle(
    articleId: String,
    articleTitle: String,
) {
    if (state.currentTopLevelKey != NewsRoute) return
    val current = state.currentKey as? ArticleRoute ?: return
    if (current.articleId == articleId) return

    state.currentSubStack.add(
        ArticleRoute(
            articleId = articleId,
            articleTitle = articleTitle,
            instanceId = Uuid.random().toString(),
        ),
    )
}

/** Opens the article image gallery on top of the reading scene. */
internal fun Navigator.openImages(
    urls: List<String>,
    index: Int,
) {
    if (state.currentTopLevelKey != NewsRoute) return
    if (urls.isEmpty() || index !in urls.indices) return
    // A second tap while the gallery is already open must not stack another copy.
    if (state.currentSubStack.lastOrNull() is ImageGalleryRoute) return
    if (state.currentSubStack.lastOrNull() !is ArticleRoute) return
    navigate(ImageGalleryRoute(urls, index))
}

internal fun replaceSelectedArticle(stack: MutableList<NavKey>, article: ArticleRoute) {
    // Only the feed's selection segment is replaceable. Do not erase unrelated child flows.
    if (stack.firstOrNull() != NewsRoute || stack.drop(1).any { it !is ArticleRoute }) return
    val current = stack.lastOrNull() as? ArticleRoute
    val selection = current?.takeIf { it.articleId == article.articleId } ?: article
    if (stack.size == 2 && selection === current) return
    while (stack.lastOrNull() is ArticleRoute) stack.removeAt(stack.lastIndex)
    stack.add(selection)
}
