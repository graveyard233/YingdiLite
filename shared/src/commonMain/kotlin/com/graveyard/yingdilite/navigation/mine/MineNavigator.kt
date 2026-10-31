package com.graveyard.yingdilite.navigation.mine

import com.graveyard.core.designsystem.navigation.Navigator
import com.graveyard.core.data.auth.AuthState
import com.graveyard.feature.mine.model.LegalAgreement
import com.graveyard.feature.mine.navigation.MineAgreementRoute
import com.graveyard.feature.mine.navigation.MineLoginRoute
import com.graveyard.feature.mine.navigation.MineRoute

/** Keep feature-specific back-stack policy in the assembly layer, as in NewsNavigator. */
internal fun Navigator.openMineLogin(authState: AuthState) {
    if (authState != AuthState.Guest) return
    if (state.currentTopLevelKey != MineRoute || state.currentKey != MineRoute) return
    navigate(MineLoginRoute)
}

/** Auth completion also cleans an inactive/restored Mine stack without switching tabs. */
internal fun Navigator.finishMineLogin() {
    state.subStacks[MineRoute]?.removeAll { it == MineLoginRoute || it is MineAgreementRoute }
}

private fun Navigator.canInteractWithMineLogin(): Boolean =
    state.currentTopLevelKey == MineRoute &&
        (state.currentKey == MineLoginRoute ||
            (state.currentKey is MineAgreementRoute &&
                state.currentSubStack.getOrNull(state.currentSubStack.lastIndex - 1) == MineLoginRoute))

/** The login pane's back arrow returns to the guest profile, also closing an open agreement. */
internal fun Navigator.returnToMineProfile() {
    if (!canInteractWithMineLogin()) return
    if (state.currentKey is MineAgreementRoute) goBack()
    if (state.currentKey == MineLoginRoute) goBack()
}

internal fun Navigator.openMineAgreement(agreement: LegalAgreement) {
    if (!canInteractWithMineLogin()) return
    val route = MineAgreementRoute(agreement)
    when (val current = state.currentKey) {
        MineLoginRoute -> navigate(route)
        is MineAgreementRoute -> if (current != route) {
            // There is only one supporting agreement entry above the login entry.
            val stack = state.currentSubStack
            stack[stack.lastIndex] = route
        }
        else -> Unit
    }
}

/** The agreement's own back arrow and system back both dismiss only the agreement. */
internal fun Navigator.closeMineAgreement(route: MineAgreementRoute) {
    if (state.currentTopLevelKey == MineRoute && state.currentKey == route) goBack()
}
