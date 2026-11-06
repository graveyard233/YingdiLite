package com.graveyard.core.designsystem.navigation.auth

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

/** Feature-owned, serializable intent. Register each implementation in the app's serializer module. */
interface AuthContinuation

@Serializable
data class AuthRequest(
    val requestId: String,
    @Polymorphic val ownerTabKey: NavKey,
    @Polymorphic val sourceEntryKey: NavKey,
    @Polymorphic val continuation: AuthContinuation,
)

@Serializable
data class AuthResult(val request: AuthRequest, val userId: Long)

@Serializable
data class AuthPrompt(val request: AuthRequest, val message: String)
