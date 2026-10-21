package com.graveyard.feature.news.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.graveyard.core.data.repository.NewsRepository
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataResult
import com.graveyard.core.designsystem.webview.RichContentReadingState
import com.graveyard.core.model.news.NewsAuthor
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ArticleViewModel(
    private val repository: NewsRepository,
    private val articleId: String,
    articleTitle: String,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val readingState = RichContentReadingState(
        savedSnapshot = savedStateHandle[READING_POSITION_KEY],
        onSnapshotChanged = { savedStateHandle[READING_POSITION_KEY] = it },
    )
    private val mutableUiState = MutableStateFlow(
        ArticleUiState(
            articleId = articleId,
            articleTitle = articleTitle,
        ),
    )
    val uiState: StateFlow<ArticleUiState> = mutableUiState.asStateFlow()
    private var request: Job? = null

    init { loadArticle() }

    fun loadArticle() {
        request?.cancel()
        mutableUiState.update { it.copy(isLoading = true, error = null) }
        request = viewModelScope.launch {
            val id = articleId.toLongOrNull()?.takeIf { it > 0 }
            val result = if (id == null) DataResult.Failure(DataError.Parsing) else repository.getArticleContent(id)
            currentCoroutineContext().ensureActive()
            mutableUiState.update {
                when (result) {
                    is DataResult.Success -> it.copy(
                        content = result.data,
                        newsAuthor = result.data.newsAuthor,
                        isLoading = false,
                        error = null,
                    )
                    is DataResult.Failure -> it.copy(
                        content = null,
                        newsAuthor = NewsAuthor(),
                        isLoading = false,
                        error = result.error,
                    )
                }
            }
        }
    }

    fun loadComments() {
        // Reserved for the comments request.
    }

    fun onMoreClicked() {
        // Reserved for the article actions menu.
    }

    private companion object {
        const val READING_POSITION_KEY = "article.readingPosition"
    }
}
