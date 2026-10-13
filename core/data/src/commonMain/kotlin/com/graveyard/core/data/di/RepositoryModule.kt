package com.graveyard.core.data.di

import com.graveyard.core.data.repository.ArticleRepository
import com.graveyard.core.data.repository.DefaultArticleRepository
import com.graveyard.core.data.repository.DefaultNewsRepository
import com.graveyard.core.data.repository.NewsRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::DefaultArticleRepository).bind(ArticleRepository::class)
    singleOf(::DefaultNewsRepository).bind(NewsRepository::class)
}
