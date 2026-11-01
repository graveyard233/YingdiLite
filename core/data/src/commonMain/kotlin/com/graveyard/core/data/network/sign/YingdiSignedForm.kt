package com.graveyard.core.data.network.sign

import com.graveyard.core.data.crypto.YingdiSignProfile

/**
 * 一次签名请求的业务参数（例如 `tag_id=99`）。
 *
 * 这里只保存有序的结构化字段和签名配置 [profile]，时间戳、签名和最终的 form body 由 [YingdiSignPlugin] 补齐。
 */
internal class YingdiSignedForm(
    internal val fields: List<Pair<String, String>>,
    internal val loginToken: String = "nologin",
    internal val profile: YingdiSignProfile = YingdiSignProfile.Pc,
) {

    /** 每次发送从业务字段重新构造，绝不复用已签名的请求体；签名配置随之保留，刷新后重放不会退回 PC。 */
    fun withLoginToken(loginToken: String): YingdiSignedForm = YingdiSignedForm(fields, loginToken, profile)

    companion object {
        fun of(
            vararg params: Pair<String, String>,
            profile: YingdiSignProfile = YingdiSignProfile.Pc,
        ): YingdiSignedForm = YingdiSignedForm(params.toList(), profile = profile)
    }
}
