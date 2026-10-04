package com.graveyard.feature.news.di

import com.graveyard.feature.news.viewmodel.NewsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val newsModule = module {
    viewModelOf(::NewsViewModel)
}
