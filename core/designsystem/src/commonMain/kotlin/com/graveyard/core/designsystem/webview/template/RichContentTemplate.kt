package com.graveyard.core.designsystem.webview.template

import com.graveyard.core.designsystem.webview.platform.WebViewConfiguration
import com.graveyard.core.designsystem.webview.pool.WebViewReuseKey
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The HTML must contain exactly one <!-- APP_WEB_BRIDGE --> before its template script. */
abstract class RichContentTemplate(
    val id: String,
    val revision: Int,
    val baseUrl: String,
    val protocolVersion: Int = 1,
) {
    init {
        require(id.matches(Regex("[A-Za-z0-9._:-]+"))) { "Template id must be a stable ASCII identifier" }
        require(revision >= 0)
        require(protocolVersion == 1) { "Unsupported template protocol" }
    }

    abstract suspend fun loadHtml(): String

    internal val reuseKey get() = WebViewReuseKey(
        "template", WebViewConfiguration(templateBridge = true), id, revision, protocolVersion, baseUrl,
    )
}

internal fun assembleTemplate(template: RichContentTemplate, html: String, bridge: String, instanceId: Long): String {
    val marker = "<!-- APP_WEB_BRIDGE -->"
    require(html.indexOf(marker) >= 0 && html.indexOf(marker) == html.lastIndexOf(marker)) {
        "Template must contain exactly one $marker"
    }
    val bootstrap = buildJsonObject {
        put("protocol", template.protocolVersion)
        put("instance", instanceId)
        put("templateId", template.id)
        put("revision", template.revision)
    }
    return html.replace(marker, "<script>" + bridge.replace("__APP_BOOTSTRAP__", bootstrap.toString()) + "</script>")
}
