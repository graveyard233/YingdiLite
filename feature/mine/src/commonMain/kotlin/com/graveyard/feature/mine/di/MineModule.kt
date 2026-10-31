package com.graveyard.feature.mine.di

import com.graveyard.feature.mine.auth.MineLoginCoordinator
import com.graveyard.feature.mine.viewmodel.MineLoginViewModel
import com.graveyard.feature.mine.viewmodel.MineViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val mineModule = module {
    single { MineLoginCoordinator(get(), get(named("authScope"))) }
    viewModelOf(::MineLoginViewModel)
    viewModelOf(::MineViewModel)
}
