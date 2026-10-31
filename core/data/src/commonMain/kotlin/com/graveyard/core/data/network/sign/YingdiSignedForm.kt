package com.graveyard.core.data.network.sign

/**
 * 一次签名请求的业务参数（例如 `tag_id=99`）。
 *
 * 这里只保存有序的结构化字段，时间戳、签名和最终的 form body 由 [YingdiSignPlugin] 补齐。
 */
internal class YingdiSignedForm(
    internal val fields: List<Pair<String, String>>,
    internal val loginToken: String = "nologin",
) {

    /** 每次发送从业务字段重新构造，绝不复用已签名的请求体。 */
    fun withLoginToken(loginToken: String): YingdiSignedForm = YingdiSignedForm(fields, loginToken)

    companion object {
        fun of(vararg params: Pair<String, String>): YingdiSignedForm =
            YingdiSignedForm(params.toList())
    }
}
