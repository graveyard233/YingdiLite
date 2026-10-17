package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.designsystem.webview.RichContentReadingState
import com.graveyard.core.designsystem.webview.RichContentView
import com.graveyard.core.designsystem.webview.platform.WebNavigationDecision
import com.graveyard.core.designsystem.webview.platform.WebViewInteraction
import com.graveyard.feature.news.ui.articleDetail.NewsDetailTemplate
import com.graveyard.core.data.result.DataException
import com.graveyard.core.model.news.NewsAuthor
import com.graveyard.feature.news.ui.newsList.component.NewsErrorState
import com.graveyard.feature.news.ui.newsList.component.newsErrorMessage
import com.graveyard.feature.news.viewmodel.ArticleUiState
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.article_more
import yingdilite.feature.news.generated.resources.news_back
import yingdilite.feature.news.generated.resources.article_close_selection
import yingdilite.feature.news.generated.resources.article_comments_title
import yingdilite.feature.news.generated.resources.news_load_failed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArticleDetailPage(
    uiState: ArticleUiState,
    newsAuthor: NewsAuthor,
    onBack: () -> Unit,
    onMoreClicked: () -> Unit,
    readingState: RichContentReadingState,
    onRetry: () -> Unit,
    closeSelection: Boolean = false,
    onCommentsClick: (() -> Unit)? = null,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = uiState.articleTitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = (if (closeSelection) AppIcon.Close else AppIcon.ArrowBack).imageVector,
                            contentDescription = stringResource(if (closeSelection) Res.string.article_close_selection else Res.string.news_back),
                        )
                    }
                },
                actions = {
                    if (onCommentsClick != null) {
                        IconButton(onClick = onCommentsClick) {
                            Icon(AppIcon.Comment.imageVector, stringResource(Res.string.article_comments_title))
                        }
                    }
                    IconButton(onClick = onMoreClicked) {
                        Icon(
                            imageVector = AppIcon.MoreVert.imageVector,
                            contentDescription = stringResource(Res.string.article_more),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.error != null -> NewsErrorState(
                    title = stringResource(Res.string.news_load_failed),
                    message = DataException(uiState.error).newsErrorMessage(),
                    onRetry = onRetry,
                )
                uiState.content != null -> RichContentView(
                    template = NewsDetailTemplate,
                    contentKey = "news:${uiState.content.articleId}",
                    html = uiState.content.html,
                    baseUrl = uiState.content.baseUrl,
                    readingState = readingState,
                    templateData = buildJsonObject {
                        put("author", buildJsonObject {
                            put("id", newsAuthor.id)
                            put("username", newsAuthor.username)
                            put("head", newsAuthor.head)
                            put("credits", newsAuthor.credits)
                            put("level", newsAuthor.level)
                            put("badgeJson", newsAuthor.badgeJson)
                        })
                    },
                    modifier = Modifier.fillMaxSize(),
                    interaction = WebViewInteraction.NestedScroll,
                    onNavigation = { request ->
                        if (request.url.startsWith("https://", true) || request.url.startsWith("http://", true)) {
                            WebNavigationDecision.OpenExternal
                        } else WebNavigationDecision.Block
                    },
                )
            }
        }
    }
}
