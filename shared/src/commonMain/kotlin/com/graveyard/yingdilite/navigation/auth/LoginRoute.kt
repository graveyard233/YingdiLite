package com.graveyard.yingdilite.navigation.auth

import androidx.navigation3.runtime.NavKey
import com.graveyard.core.designsystem.navigation.auth.AuthRequest
import com.graveyard.feature.mine.model.LegalAgreement
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
internal data class LoginRoute(
    val request: AuthRequest? = null,
    val instanceId: String = Uuid.random().toString(),
) : NavKey

@Serializable
internal data class LoginAgreementRoute(val loginInstanceId: String, val agreement: LegalAgreement) : NavKey
