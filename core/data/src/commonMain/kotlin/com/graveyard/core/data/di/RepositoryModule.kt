package com.graveyard.core.data.di

import com.graveyard.core.data.repository.DefaultNewsRepository
import com.graveyard.core.data.repository.AuthRepository
import com.graveyard.core.data.repository.DefaultAuthRepository
import com.graveyard.core.data.repository.NewsRepository
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    single<AuthRepository>(createdAtStart = true) {
        DefaultAuthRepository(get(), get(), get(), get(named("authScope")))
    }
    singleOf(::DefaultNewsRepository).bind(NewsRepository::class)
}
