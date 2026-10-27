package com.graveyard.feature.mine.navigation

import androidx.navigation3.runtime.NavKey
import com.graveyard.feature.mine.model.LegalAgreement
import kotlinx.serialization.Serializable

@Serializable
data object MineRoute : NavKey

@Serializable
data object MineLoginRoute : NavKey

@Serializable
data class MineAgreementRoute(val agreement: LegalAgreement) : NavKey
