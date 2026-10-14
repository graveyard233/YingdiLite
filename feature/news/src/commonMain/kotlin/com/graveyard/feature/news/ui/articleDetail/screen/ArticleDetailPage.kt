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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.feature.news.viewmodel.ArticleUiState
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.article_more
import yingdilite.feature.news.generated.resources.news_back
import yingdilite.feature.news.generated.resources.article_close_selection
import yingdilite.feature.news.generated.resources.article_comments_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArticleDetailPage(
    uiState: ArticleUiState,
    onBack: () -> Unit,
    onMoreClicked: () -> Unit,
    closeSelection: Boolean = false,
    onCommentsClick: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
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
            Text(uiState.detailPlaceholder)
        }
    }
}
