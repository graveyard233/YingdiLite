package com.graveyard.yingdilite.navigation.auth

import com.graveyard.core.data.auth.AuthState
import com.graveyard.core.designsystem.navigation.Navigator
import com.graveyard.core.designsystem.navigation.auth.AuthFlowState
import com.graveyard.core.designsystem.navigation.auth.AuthRequest
import com.graveyard.feature.mine.model.LegalAgreement
import com.graveyard.feature.mine.navigation.MineAgreementRoute
import com.graveyard.feature.mine.navigation.MineLoginRoute

internal fun Navigator.openLogin(authState: AuthState, request: AuthRequest? = null) {
    if (authState != AuthState.Guest) return
    if (request != null && (request.ownerTabKey != state.currentTopLevelKey ||
            request.sourceEntryKey !in state.currentSubStack)) return
    if (state.currentSubStack.any { it is LoginRoute }) return
    navigate(LoginRoute(request))
}

/** Retain serializers for legacy keys, but use a single modern flow after restoration. */
internal fun Navigator.normaliseLegacyLogin() {
    state.subStacks.values.forEach { stack ->
        var login: LoginRoute? = null
        stack.indices.forEach { index ->
            when (val key = stack[index]) {
                MineLoginRoute -> {
                    val normalised = LoginRoute()
                    login = normalised
                    stack[index] = normalised
                }
                is MineAgreementRoute -> login?.let {
                    stack[index] = LoginAgreementRoute(it.instanceId, key.agreement)
                }
                else -> login = key as? LoginRoute
            }
        }
    }
}

/** Register results before removing their only saved source, including inactive tab flows. */
internal fun Navigator.finishLogin(userId: Long, authFlow: AuthFlowState) {
    state.subStacks.forEach { (owner, stack) ->
        val logins = stack.filterIsInstance<LoginRoute>()
        logins.forEach { login ->
            login.request?.takeIf { it.ownerTabKey == owner && it.sourceEntryKey in stack }?.let {
                authFlow.deliver(it, userId)
            }
        }
        val ids = logins.map { it.instanceId }.toSet()
        stack.removeAll { it is LoginRoute || (it is LoginAgreementRoute && it.loginInstanceId in ids) }
    }
}

private fun Navigator.activeLogin(): LoginRoute? = when (val key = state.currentKey) {
    is LoginRoute -> key
    is LoginAgreementRoute -> state.currentSubStack.filterIsInstance<LoginRoute>()
        .lastOrNull { it.instanceId == key.loginInstanceId }
    else -> null
}

/** Cancelling removes the saved request too; an in-flight login may still authenticate later. */
internal fun Navigator.closeLogin(expected: LoginRoute) {
    if (activeLogin() != expected) return
    val stack = state.currentSubStack
    if ((stack.lastOrNull() as? LoginAgreementRoute)?.loginInstanceId == expected.instanceId) goBack()
    if (state.currentKey == expected) goBack()
}

internal fun Navigator.openLoginAgreement(login: LoginRoute, agreement: LegalAgreement) {
    if (activeLogin() != login) return
    val route = LoginAgreementRoute(login.instanceId, agreement)
    if (state.currentKey is LoginAgreementRoute) {
        if (state.currentKey != route) state.currentSubStack[state.currentSubStack.lastIndex] = route
    } else navigate(route)
}

internal fun Navigator.closeLoginAgreement(route: LoginAgreementRoute) {
    if (state.currentKey == route) goBack()
}
