package com.graveyard.core.data.di

import com.graveyard.core.data.repository.DefaultNewsRepository
import com.graveyard.core.data.repository.NewsRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::DefaultNewsRepository).bind(NewsRepository::class)
}
