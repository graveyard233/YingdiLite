package com.graveyard.yingdilite.navigation.news

import androidx.navigation3.runtime.NavKey
import com.graveyard.core.designsystem.navigation.Navigator
import com.graveyard.feature.news.navigation.ArticleRoute
import com.graveyard.feature.news.navigation.NewsRoute

/** Extension in the assembly layer avoids a core:designsystem -> feature:news dependency. */
internal fun Navigator.selectArticle(articleId: String, articleTitle: String) {
    if (state.currentTopLevelKey != NewsRoute) return
    replaceSelectedArticle(state.currentSubStack, ArticleRoute(articleId, articleTitle))
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
