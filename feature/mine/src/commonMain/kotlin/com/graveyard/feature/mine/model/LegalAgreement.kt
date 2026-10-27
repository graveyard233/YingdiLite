package com.graveyard.feature.mine.model

import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import yingdilite.feature.mine.generated.resources.Res
import yingdilite.feature.mine.generated.resources.mine_legal_children
import yingdilite.feature.mine.generated.resources.mine_legal_community
import yingdilite.feature.mine.generated.resources.mine_legal_privacy
import yingdilite.feature.mine.generated.resources.mine_legal_terms

@Serializable
enum class LegalAgreement(val url: String, val title: StringResource) {
    Terms(
        "https://mob.iyingdi.com/spread/term",
        Res.string.mine_legal_terms,
    ),
    Privacy(
        "https://mob.iyingdi.com/spread/policy",
        Res.string.mine_legal_privacy,
    ),
    Community(
        "https://mob.iyingdi.com/spread/comnorms",
        Res.string.mine_legal_community,
    ),
    Children(
        "https://mob.iyingdi.com/spread/privacyChildren",
        Res.string.mine_legal_children,
    ),
}
