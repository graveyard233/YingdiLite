package com.graveyard.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.graveyard.core.data.mapper.toNewsArticle
import com.graveyard.core.data.network.YingdiData
import com.graveyard.core.data.result.toDataException
import com.graveyard.core.model.news.NewsArticle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class NewsPagingSource(
    private val remoteDataSource: YingdiData,
    private val tagId: Int,
    private val pageSize: Int,
    private val onRefreshEvent: ((NewsRefreshEvent) -> Unit)? = null,
) : PagingSource<Int, NewsArticle>() {
    private var version: Long = 0L

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NewsArticle> {
        val page = params.key ?: 1
        val refreshRequest = if (params is LoadParams.Refresh) NewsRefreshRequest() else null
        var completed = false
        refreshRequest?.let { onRefreshEvent?.invoke(NewsRefreshEvent.Started(it)) }

        return try {
            // The API uses page numbers, so changing size would shift page boundaries.
            val response = remoteDataSource.getNewsList(
                page = page,
                size = pageSize,
                tagId = tagId,
                version = if (page == 1) 0L else version,
            )
            currentCoroutineContext().ensureActive()
            version = response.version
            val result = LoadResult.Page(
                data = response.list.map { it.toNewsArticle() },
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.list.size < pageSize) null else page + 1,
            )
            refreshRequest?.let { onRefreshEvent?.invoke(NewsRefreshEvent.Completed(it)) }
            completed = true
            result
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            val error = exception.toDataException()
            refreshRequest?.let {
                onRefreshEvent?.invoke(NewsRefreshEvent.Completed(it, error.error))
            }
            completed = true
            LoadResult.Error(error)
        } finally {
            if (!completed) {
                refreshRequest?.let { onRefreshEvent?.invoke(NewsRefreshEvent.Cancelled(it)) }
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, NewsArticle>): Int? {
        // A new source must fetch page 1 to obtain a version before loading later pages.
        return null
    }
}
