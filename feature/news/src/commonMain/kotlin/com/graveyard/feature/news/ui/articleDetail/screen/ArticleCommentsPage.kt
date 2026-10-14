package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.feature.news.viewmodel.ArticleUiState
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.article_back_to_detail
import yingdilite.feature.news.generated.resources.article_comments_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArticleCommentsPage(
    uiState: ArticleUiState,
    onBack: () -> Unit,
    showBack: Boolean = true,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.article_comments_title)) },
                navigationIcon = {
                    if (showBack) IconButton(onClick = onBack) {
                        Icon(
                            imageVector = AppIcon.ArrowBack.imageVector,
                            contentDescription = stringResource(Res.string.article_back_to_detail),
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
            Text(uiState.commentsPlaceholder)
        }
    }
}
