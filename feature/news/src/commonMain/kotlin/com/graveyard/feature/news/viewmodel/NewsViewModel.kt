package com.graveyard.feature.news.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.graveyard.core.data.repository.NewsRepository
import com.graveyard.core.data.result.DataResult
import com.graveyard.core.model.news.NewsArticle
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewsViewModel(
    private val repository: NewsRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = mutableUiState.asStateFlow()

    private val newsFlows = mutableMapOf<Int, Flow<PagingData<NewsArticle>>>()
    private val bannerJobs = mutableMapOf<Int, Job>()

    @MainThread
    fun selectTag(tagId: Int) {
        if (mutableUiState.value.selectedTagId == tagId) return

        mutableUiState.update { it.copy(selectedTagId = tagId) }
        newsFor(tagId)
        if (tagId !in mutableUiState.value.bannersByTag) {
            reloadBanners(tagId)
        }
    }

    @MainThread
    fun newsFor(tagId: Int): Flow<PagingData<NewsArticle>> =
        newsFlows.getOrPut(tagId) {
            repository.getNews(tagId).cachedIn(viewModelScope)
        }

    @MainThread
    fun reloadBanners(tagId: Int) {
        bannerJobs[tagId]?.cancel()
        mutableUiState.update { state ->
            val previous = state.bannersByTag[tagId] ?: BannerUiState()
            state.copy(
                bannersByTag = state.bannersByTag + (
                    tagId to previous.copy(isLoading = true, error = null)
                ),
            )
        }

        // Register the job before it can complete or suspend in the repository.
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val result = repository.getBanners(tagId)
            currentCoroutineContext().ensureActive()
            mutableUiState.update { state ->
                val bannerState = when (result) {
                    is DataResult.Success -> BannerUiState(items = result.data)
                    is DataResult.Failure -> {
                        val previous = state.bannersByTag[tagId] ?: BannerUiState()
                        previous.copy(isLoading = false, error = result.error)
                    }
                }
                state.copy(bannersByTag = state.bannersByTag + (tagId to bannerState))
            }
        }
        bannerJobs[tagId] = job
        job.start()
    }

    override fun onCleared() {
        bannerJobs.values.forEach { it.cancel() }
        bannerJobs.clear()
        newsFlows.clear()
        super.onCleared()
    }
}
