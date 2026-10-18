package com.graveyard.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.graveyard.core.data.local.preferences.requireValidTagInfos
import com.graveyard.core.data.mapper.toNewsBanner
import com.graveyard.core.data.mapper.toTagInfo
import com.graveyard.core.data.network.YingdiData
import com.graveyard.core.data.paging.NewsPagingSource
import com.graveyard.core.data.result.DataResult
import com.graveyard.core.data.result.toDataException
import com.graveyard.core.model.news.NewsArticle
import com.graveyard.core.model.news.NewsBanner
import com.graveyard.core.model.news.TagInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class DefaultNewsRepository(
    private val remoteDataSource: YingdiData,
) : NewsRepository {
    override suspend fun getBanners(tagId: Int): DataResult<List<NewsBanner>> {
        return try {
            val response = remoteDataSource.getBannerList(tagId)
            DataResult.Success(response.topContent.map { it.toNewsBanner() })
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            DataResult.Failure(exception.toDataException().error)
        }
    }

    override fun getNews(
        tagId: Int,
        pageSize: Int,
    ): Flow<PagingData<NewsArticle>> {
        require(pageSize > 0) { "pageSize must be greater than zero" }

        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                initialLoadSize = pageSize,
                enablePlaceholders = false,
                prefetchDistance = (pageSize / 2).coerceAtLeast(1),
            ),
            initialKey = 1,
            pagingSourceFactory = {
                NewsPagingSource(
                    remoteDataSource = remoteDataSource,
                    tagId = tagId,
                    pageSize = pageSize,
                )
            },
        ).flow
    }

    override suspend fun refreshTagInfos(): DataResult<List<TagInfo>> {
        return try {
            val response = remoteDataSource.getTagList()
            val tags = withContext(Dispatchers.Default) {
                response.map { it.toTagInfo() }.also(::requireValidTagInfos)
            }
            currentCoroutineContext().ensureActive()
            DataResult.Success(tags)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            DataResult.Failure(exception.toDataException().error)
        }
    }

    override suspend fun getArticleContent(postId: Long): DataResult<ArticleContent> {
        return try {
            require(postId > 0) { "postId must be positive" }
            val html = remoteDataSource.getArticleContentHtml(postId)
            currentCoroutineContext().ensureActive()
            DataResult.Success(ArticleContent(postId, html, "https://mob.iyingdi.com/post/$postId"))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            DataResult.Failure(exception.toDataException().error)
        }
    }

}
