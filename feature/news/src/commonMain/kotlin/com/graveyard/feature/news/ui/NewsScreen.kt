package com.graveyard.feature.news.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.graveyard.core.data.result.DataError
import com.graveyard.core.model.news.NewsArticle
import com.graveyard.core.model.news.NewsBanner
import com.graveyard.feature.news.viewmodel.BannerUiState
import com.graveyard.feature.news.viewmodel.NewsViewModel
import org.koin.compose.viewmodel.koinViewModel

private val testTagIds = listOf(17, 18, 859)
private const val defaultTagId = 17

@Composable
fun NewsScreen(
    viewModel: NewsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeTagId = uiState.selectedTagId ?: defaultTagId
    val newsItems = remember(activeTagId) {
        viewModel.newsFor(activeTagId)
    }.collectAsLazyPagingItems()
    val bannerState = uiState.bannersByTag[activeTagId] ?: BannerUiState()

    LaunchedEffect(viewModel) {
        viewModel.selectTag(defaultTagId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "News ViewModel 网络测试",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "切换 tagId，观察 Banner 和分页请求状态",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            testTagIds.forEach { tagId ->
                if (tagId == activeTagId) {
                    Button(
                        onClick = { viewModel.selectTag(tagId) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("tag $tagId")
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.selectTag(tagId) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("tag $tagId")
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text(
                text = "当前 tagId: $activeTagId",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = {
                    viewModel.reloadBanners(activeTagId)
                    newsItems.refresh()
                },
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("刷新")
            }
        }

        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            state = rememberLazyListState(),
        ) {
            bannerSection(bannerState)
            newsSection(newsItems)
        }
    }
}

private fun LazyListScope.bannerSection(state: BannerUiState) {
    item {
        Text(
            text = "Banner (${state.items.size})",
            style = MaterialTheme.typography.titleMedium,
        )
    }
    when {
        state.isLoading -> item { LoadingRow("Banner 请求中...") }
        state.error != null -> item {
            ErrorRow(
                message = "Banner 请求失败：${state.error.asMessage()}",
                onRetry = null,
            )
        }
        state.items.isEmpty() -> item {
            Text(
                text = "暂无 Banner 数据",
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> items(state.items) { banner ->
            BannerRow(banner)
        }
    }
    item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }
}

private fun LazyListScope.newsSection(newsItems: LazyPagingItems<NewsArticle>) {
    item {
        Text(
            text = "新闻列表 (${newsItems.itemCount})",
            style = MaterialTheme.typography.titleMedium,
        )
    }

    when (val refresh = newsItems.loadState.refresh) {
        LoadState.Loading -> item { LoadingRow("新闻首屏请求中...") }
        is LoadState.Error -> item {
            ErrorRow(
                message = "新闻请求失败：${refresh.error.message ?: "未知错误"}",
                onRetry = newsItems::retry,
            )
        }
        is LoadState.NotLoading -> if (newsItems.itemCount == 0) {
            item {
                Text(
                    text = "暂无新闻数据",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    items(count = newsItems.itemCount) { index ->
        newsItems[index]?.let { article ->
            ArticleRow(article)
        }
    }

    when (val append = newsItems.loadState.append) {
        LoadState.Loading -> item { LoadingRow("正在加载更多...") }
        is LoadState.Error -> item {
            ErrorRow(
                message = "加载更多失败：${append.error.message ?: "未知错误"}",
                onRetry = newsItems::retry,
            )
        }
        is LoadState.NotLoading -> if (append.endOfPaginationReached && newsItems.itemCount > 0) {
            item {
                Text(
                    text = "已加载全部数据",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BannerRow(banner: NewsBanner) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = banner.title.ifBlank { "(无标题)" },
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "adId=${banner.adId}  ${banner.url.ifBlank { "(无 URL)" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ArticleRow(article: NewsArticle) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = article.title.ifBlank { "(无标题)" },
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "id=${article.id}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (article.url.isNotBlank()) {
            Text(
                text = article.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LoadingRow(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(message, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ErrorRow(
    message: String,
    onRetry: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        onRetry?.let {
            OutlinedButton(
                onClick = it,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("重试")
            }
        }
    }
}

private fun DataError.asMessage(): String = when (this) {
    DataError.Network -> "网络错误"
    DataError.Timeout -> "请求超时"
    is DataError.Http -> "HTTP ${statusCode}"
    is DataError.Business -> listOfNotNull(retCode, retMsg).joinToString("：")
        .ifBlank { "业务错误" }
    DataError.Parsing -> "数据解析错误"
    DataError.Unknown -> "未知错误"
}
