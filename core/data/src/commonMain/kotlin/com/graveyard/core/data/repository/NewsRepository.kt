package com.graveyard.core.data.repository

import androidx.paging.PagingData
import com.graveyard.core.data.paging.NewsRefreshEvent
import com.graveyard.core.data.result.DataResult
import com.graveyard.core.model.news.NewsArticle
import com.graveyard.core.model.news.NewsBanner
import com.graveyard.core.model.news.TagInfo
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    suspend fun getBanners(tagId: Int): DataResult<List<NewsBanner>>

    /** The caller selects the tag and owns caching this flow in its lifecycle scope. */
    fun getNews(
        tagId: Int,
        pageSize: Int = 20,
        onRefreshEvent: ((NewsRefreshEvent) -> Unit)? = null,
    ): Flow<PagingData<NewsArticle>>

    /**
     * 获取并校验远程全量标签，不写入缓存。
     * 调用方等待编辑完成后，通过 UserSettings 原子提交目录和订阅元信息。
     */
    suspend fun refreshTagInfos(): DataResult<List<TagInfo>>

    suspend fun getArticleContent(postId: Long): DataResult<ArticleContent>
}
