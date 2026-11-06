package com.graveyard.feature.news.di

import com.graveyard.feature.news.viewmodel.ArticleViewModel
import com.graveyard.feature.news.viewmodel.ArticleRepliesViewModel
import com.graveyard.feature.news.viewmodel.CommentVoteCoordinator
import com.graveyard.feature.news.viewmodel.NewsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val newsModule = module {
    single { CommentVoteCoordinator(repository = get(), authRepository = get()) }
    viewModel { parameters ->
        ArticleRepliesViewModel(
            repository = get(),
            commentVoteCoordinator = get(),
            authRepository = get(),
            articleId = parameters[0],
            commentId = parameters[1],
            savedStateHandle = get(),
        )
    }
    viewModel { parameters ->
        ArticleViewModel(
            repository = get(),
            authRepository = get(),
            commentVoteCoordinator = get(),
            articleId = parameters[0],
            articleTitle = parameters[1],
            savedStateHandle = get(),
        )
    }
    viewModelOf(::NewsViewModel)
}
