package com.graveyard.yingdilite.navigation.news

import androidx.navigation3.runtime.NavKey
import com.graveyard.core.designsystem.navigation.Navigator
import com.graveyard.feature.news.navigation.ArticleRoute
import com.graveyard.feature.news.navigation.ArticleRepliesRoute
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
    source: ArticleRoute? = null,
) {
    if (state.currentTopLevelKey != NewsRoute) return
    val current = source ?: state.currentKey as? ArticleRoute ?: return
    if (!canInteractWithArticle(current)) return
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
    source: NavKey = state.currentKey,
) {
    if (state.currentTopLevelKey != NewsRoute) return
    if (urls.isEmpty() || index !in urls.indices) return
    // A second tap while the gallery is already open must not stack another copy.
    if (state.currentSubStack.lastOrNull() is ImageGalleryRoute) return
    if (source is ArticleRoute) {
        if (!canInteractWithArticle(source)) return
    } else if (source !is ArticleRepliesRoute || state.currentKey != source) return
    navigate(ImageGalleryRoute(urls, index))
}

internal fun Navigator.canInteractWithArticle(article: ArticleRoute): Boolean =
    state.currentTopLevelKey == NewsRoute &&
        (state.currentKey == article || (state.currentKey as? ArticleRepliesRoute)?.article == article)

internal fun Navigator.openReplies(article: ArticleRoute, commentId: Long) {
    if (commentId <= 0L || state.currentTopLevelKey != NewsRoute || state.currentKey != article) return
    navigate(ArticleRepliesRoute(article, commentId, Uuid.random().toString()))
}

internal fun replaceSelectedArticle(stack: MutableList<NavKey>, article: ArticleRoute) {
    // Only the feed's selection segment is replaceable. Do not erase unrelated child flows.
    if (stack.firstOrNull() != NewsRoute || stack.drop(1).any { it !is ArticleRoute && it !is ArticleRepliesRoute }) return
    val current = when (val top = stack.lastOrNull()) {
        is ArticleRoute -> top
        is ArticleRepliesRoute -> top.article
        else -> null
    }
    val selection = current?.takeIf { it.articleId == article.articleId } ?: article
    if (stack.size == 2 && selection === current) return
    if (current != null && selection === current) {
        // Keep the selected entry's decorators, reading state and comments presenter alive.
        val selectedIndex = stack.indexOf(current)
        if (selectedIndex < 1) return
        while (stack.lastOrNull() != current) stack.removeAt(stack.lastIndex)
        repeat(selectedIndex - 1) { stack.removeAt(1) }
        return
    }
    while (stack.size > 1) stack.removeAt(stack.lastIndex)
    stack.add(selection)
}
