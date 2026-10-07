package com.graveyard.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonList(
    state: LazyListState,
    isRefreshing: Boolean,
    onRefresh: (() -> Unit)?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(0.dp),
    header: LazyListScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
    footer: LazyListScope.() -> Unit = {},
) {
    if (onRefresh == null) {
        CommonListContent(
            state = state,
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            header = header,
            content = content,
            footer = footer,
        )
    } else {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize(),
        ) {
            CommonListContent(
                state = state,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = verticalArrangement,
                header = header,
                content = content,
                footer = footer,
            )
        }
    }
}

@Composable
private fun CommonListContent(
    state: LazyListState,
    modifier: Modifier,
    contentPadding: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    header: LazyListScope.() -> Unit,
    content: LazyListScope.() -> Unit,
    footer: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
    ) {
        header()
        content()
        footer()
    }
}
