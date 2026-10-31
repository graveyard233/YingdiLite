package com.graveyard.core.data.network.di

import com.graveyard.core.data.expect.getHttpClient
import com.graveyard.core.data.network.YingdiData
import com.graveyard.core.data.network.YingdiApiExecutor
import com.graveyard.core.data.network.YingdiTokenRefresher
import com.graveyard.core.data.network.networkData.YingdiAuthRemoteData
import com.graveyard.core.data.network.networkData.YingdiNetData
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

fun networkModule(enableDiagnosticBodies: Boolean = false) = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    single {
        getHttpClient(json = get(), enableDiagnosticBodies = enableDiagnosticBodies)
    }
    singleOf(::YingdiAuthRemoteData)
    singleOf(::YingdiTokenRefresher)
    singleOf(::YingdiApiExecutor)
    singleOf(::YingdiNetData).bind(YingdiData::class)
}
