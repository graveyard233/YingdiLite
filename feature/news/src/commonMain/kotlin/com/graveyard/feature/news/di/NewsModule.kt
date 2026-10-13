package com.graveyard.feature.news.di

import com.graveyard.feature.news.viewmodel.ArticleViewModel
import com.graveyard.feature.news.viewmodel.NewsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val newsModule = module {
    viewModel { parameters ->
        ArticleViewModel(
            repository = get(),
            articleId = parameters[0],
            articleTitle = parameters[1],
        )
    }
    viewModelOf(::NewsViewModel)
}
