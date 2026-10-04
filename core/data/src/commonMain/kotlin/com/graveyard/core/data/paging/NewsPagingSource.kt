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
) : PagingSource<Int, NewsArticle>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NewsArticle> {
        val page = params.key ?: 1

        return try {
            // The API uses page numbers, so changing size would shift page boundaries.
            val response = remoteDataSource.getNewsList(
                page = page,
                size = pageSize,
                tagId = tagId,
            )
            LoadResult.Page(
                data = response.list.map { it.toNewsArticle() },
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.list.size < pageSize) null else page + 1,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            LoadResult.Error(exception.toDataException())
        }
    }

    override fun getRefreshKey(state: PagingState<Int, NewsArticle>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchorPosition) ?: return null
        return page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
    }
}
