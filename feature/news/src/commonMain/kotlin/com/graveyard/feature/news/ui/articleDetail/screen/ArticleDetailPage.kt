package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomAppBarState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.designsystem.platform.plainTextClipEntry
import com.graveyard.core.designsystem.webview.RichContentReadingState
import com.graveyard.core.designsystem.webview.RichContentView
import com.graveyard.core.designsystem.webview.platform.WebNavigationDecision
import com.graveyard.core.designsystem.webview.platform.WebViewInteraction
import com.graveyard.feature.news.ui.articleDetail.NEWS_CARD_MESSAGE
import com.graveyard.feature.news.ui.articleDetail.NEWS_DECK_MESSAGE
import com.graveyard.feature.news.ui.articleDetail.NewsDeckMessage
import com.graveyard.feature.news.ui.articleDetail.NewsDetailTemplate
import com.graveyard.feature.news.ui.articleDetail.asNewsCardOpen
import com.graveyard.feature.news.ui.articleDetail.asNewsDeckMessage
import com.graveyard.core.data.result.DataException
import com.graveyard.core.designsystem.webview.RichContentEvent
import com.graveyard.feature.news.ui.articleDetail.component.ArticleInteractionBar
import com.graveyard.feature.news.ui.newsList.component.NewsErrorState
import com.graveyard.feature.news.ui.newsList.component.newsErrorMessage
import com.graveyard.feature.news.viewmodel.ArticleUiState
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.article_deck_copied
import yingdilite.feature.news.generated.resources.article_deck_copy_failed
import yingdilite.feature.news.generated.resources.article_more
import yingdilite.feature.news.generated.resources.news_back
import yingdilite.feature.news.generated.resources.article_close_selection
import yingdilite.feature.news.generated.resources.news_load_failed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArticleDetailPage(
    uiState: ArticleUiState,
    onBack: () -> Unit,
    onMoreClicked: () -> Unit,
    readingState: RichContentReadingState,
    onRetry: () -> Unit,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    onDeckLinkClick: (type: String?, game: String?, id: String?) -> Unit = { _, _, _ -> },
    onCardOpen: (id: String, title: String) -> Unit = { _, _ -> },
    closeSelection: Boolean = false,
    onCommentsClick: (() -> Unit)? = null,
    contentVisible: Boolean = true,
    replyNum: String = "",
    likeNum: String = "",
    onWriteCommentClick: () -> Unit = {},
    onFireClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val showToolbar = !uiState.isLoading && uiState.error == null && uiState.content != null
    val toolbarState = rememberArticleBottomAppBarState(
        articleId = uiState.articleId,
        enabled = contentVisible && showToolbar,
    )
    val toolbarScrollEnabled by rememberUpdatedState(contentVisible && showToolbar)
    val toolbarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior(
        state = toolbarState,
        canScroll = { toolbarScrollEnabled },
        snapAnimationSpec = tween(150),
    )
    val toolbarInteractive = contentVisible && showToolbar && toolbarState.collapsedFraction < 1f
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val deckCopiedMessage = stringResource(Res.string.article_deck_copied)
    val deckCopyFailedMessage = stringResource(Res.string.article_deck_copy_failed)
    // Deck card actions arrive as templateMessage("newsDeck", {...}); see news_detail.html.
    fun onDeckMessage(message: NewsDeckMessage) {
        when (message) {
            is NewsDeckMessage.Copy -> coroutineScope.launch {
                val copied = runCatching {
                    clipboard.setClipEntry(plainTextClipEntry(message.code))
                }.isSuccess
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(if (copied) deckCopiedMessage else deckCopyFailedMessage)
            }
            is NewsDeckMessage.Link -> {
                // TODO: open the deck detail for type/game/id from the article's deck card.
                onDeckLinkClick(message.type, message.game, message.id)
            }
        }
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(if (contentVisible && showToolbar) {
                Modifier.nestedScroll(toolbarScrollBehavior.nestedScrollConnection)
            } else Modifier)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
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
                    IconButton(onClick = onMoreClicked) {
                        Icon(
                            imageVector = AppIcon.MoreVert.imageVector,
                            contentDescription = stringResource(Res.string.article_more),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (showToolbar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BottomAppBarDefaults.containerColor)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                        .clipToBounds(),
                ) {
                    ArticleInteractionBar(
                        replyNum = replyNum,
                        likeNum = likeNum,
                        enabled = toolbarInteractive,
                        onCommentsClick = onCommentsClick,
                        onWriteCommentClick = onWriteCommentClick,
                        onFireClick = onFireClick,
                        onFavoriteClick = onFavoriteClick,
                        scrollBehavior = toolbarScrollBehavior,
                        modifier = if (toolbarInteractive) Modifier else Modifier.clearAndSetSemantics {},
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        // The bottom bar overlays the document; only system insets reserve space below it.
        val contentPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(layoutDirection),
            top = paddingValues.calculateTopPadding(),
            end = paddingValues.calculateEndPadding(layoutDirection),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .clipToBounds(),
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
                    isVisible = contentVisible,
                    templateData = buildJsonObject {
                        val newsAuthor = uiState.newsAuthor
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
                    onEvent = { event ->
                        when (event) {
                            is RichContentEvent.ImageClick -> onImageClick(event.urls, event.index)
                            is RichContentEvent.TemplateMessage -> {
                                when (event.name) {
                                    NEWS_DECK_MESSAGE -> event.asNewsDeckMessage()?.let(::onDeckMessage)
                                    NEWS_CARD_MESSAGE -> event.asNewsCardOpen()?.let { onCardOpen(it.id, it.title) }
                                    else -> Unit
                                }
                            }
                            else -> Unit
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberArticleBottomAppBarState(articleId: String, enabled: Boolean): BottomAppBarState {
    val savedState = rememberSaveable(articleId, saver = BottomAppBarState.Saver) {
        BottomAppBarState(-Float.MAX_VALUE, 0f, 0f)
    }
    val currentEnabled by rememberUpdatedState(enabled)
    return remember(savedState) {
        object : BottomAppBarState by savedState {
            override var heightOffsetLimit: Float
                get() = savedState.heightOffsetLimit
                set(value) {
                    if (value == savedState.heightOffsetLimit) return
                    // Keep the same reading state when Material measures a different bar height.
                    val fraction = savedState.collapsedFraction.coerceIn(0f, 1f)
                    savedState.heightOffsetLimit = value
                    savedState.heightOffset = value * fraction
                }

            override var heightOffset: Float
                get() = savedState.heightOffset
                set(value) {
                    if (currentEnabled) savedState.heightOffset = value
                }

            override var contentOffset: Float
                get() = savedState.contentOffset
                set(value) {
                    if (currentEnabled) savedState.contentOffset = value
                }
        }
    }
}
