package com.graveyard.core.data.network

import com.graveyard.core.data.local.preferences.UserSettings
import com.graveyard.core.data.network.networkData.YingdiAuthRemoteData
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class YingdiTokenRefresher(
    private val remote: YingdiAuthRemoteData,
    private val settings: UserSettings,
) {
    private val mutex = Mutex()

    suspend fun refresh(failedToken: String): String? = mutex.withLock {
        val session = settings.session.first() ?: return@withLock null
        if (session.loginToken != failedToken) return@withLock session.loginToken
        val response = try {
            remote.refreshLogin(session.refreshToken)
        } catch (exception: DataException) {
            if ((exception.error as? DataError.Business)?.retCode == "200022") settings.clearSession()
            throw exception
        }
        if (response.loginToken.isBlank()) throw DataException(DataError.Parsing)
        settings.updateLoginToken(response.loginToken)
        response.loginToken
    }
}
