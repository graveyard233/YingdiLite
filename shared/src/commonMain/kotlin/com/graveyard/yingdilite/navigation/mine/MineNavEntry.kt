package com.graveyard.yingdilite.navigation.mine

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.graveyard.core.designsystem.navigation.Navigator
import com.graveyard.feature.mine.navigation.MineAgreementRoute
import com.graveyard.feature.mine.navigation.MineLoginRoute
import com.graveyard.feature.mine.navigation.MineRoute
import com.graveyard.feature.mine.ui.agreement.screen.MineAgreementScreen
import com.graveyard.feature.mine.ui.login.screen.MineLoginScreen
import com.graveyard.feature.mine.ui.mine.screen.MineScreen

@Composable
internal fun MineNavEntry(key: NavKey, navigator: Navigator, interactive: Boolean) {
    when (key) {
        MineRoute -> MineScreen(
            onOpenLogin = { if (interactive) navigator.openMineLogin() },
        )
        MineLoginRoute -> MineLoginScreen(
            onBack = { if (interactive) navigator.returnToMineProfile() },
            onOpenAgreement = { if (interactive) navigator.openMineAgreement(it) },
        )
        is MineAgreementRoute -> MineAgreementScreen(
            agreement = key.agreement,
            onBack = { if (interactive) navigator.closeMineAgreement(key) },
        )
        else -> error("Unknown mine navigation key: $key")
    }
}
