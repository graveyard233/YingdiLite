package com.graveyard.feature.news.viewmodel

import androidx.lifecycle.ViewModel
import com.graveyard.core.data.repository.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ArticleViewModel(
    private val repository: ArticleRepository,
    articleId: String,
    articleTitle: String,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        ArticleUiState(
            articleId = articleId,
            articleTitle = articleTitle,
        ),
    )
    val uiState: StateFlow<ArticleUiState> = mutableUiState.asStateFlow()

    fun loadArticle() {
        // Reserved for the article detail request.
    }

    fun loadComments() {
        // Reserved for the comments request.
    }

    fun onMoreClicked() {
        // Reserved for the article actions menu.
    }
}
