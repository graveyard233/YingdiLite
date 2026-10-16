package com.graveyard.core.designsystem.webview.template

import com.graveyard.core.designsystem.webview.ReadingKey
import com.graveyard.core.designsystem.webview.RichContentEvent
import com.graveyard.core.designsystem.webview.RichContentReadingState
import com.graveyard.core.designsystem.webview.RichContentTheme
import com.graveyard.core.designsystem.webview.platform.WebLoadEvent
import com.graveyard.core.designsystem.webview.platform.WebNavigationDecision
import com.graveyard.core.designsystem.webview.platform.WebViewPort
import com.graveyard.core.designsystem.webview.pool.PooledWebViewEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64

internal data class RichContentDocument(val key: String, val html: String, val baseUrl: String)

internal class TemplateRuntime(
    override val port: WebViewPort,
    private val instanceId: Long,
    private val template: RichContentTemplate,
    parentScope: CoroutineScope,
    private val loadBridge: suspend () -> String,
) : PooledWebViewEntry {
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]))
    private val ready = CompletableDeferred<Unit>()
    private var started = false
    private var destroyed = false
    private var leaseId = 0L
    private var version = 0L
    private var requestId = 0L
    private var renderRequest = 0L
    private var document: RichContentDocument? = null
    private var theme: RichContentTheme? = null
    private var readingState: RichContentReadingState? = null
    private var reading: RichContentReadingState.Binding? = null
    private var callback: ((RichContentEvent) -> Unit)? = null
    private var renderTimeout: Job? = null
    private data class Pending(val identity: JsonObject, val result: CompletableDeferred<JsonObject>)
    private val pending = mutableMapOf<Long, Pending>()
    private var snapshot: Pair<RichContentReadingState.Binding, CompletableDeferred<JsonObject>>? = null
    override var broken = false
        private set

    init {
        port.onMessage = ::receive
        port.onLoad = { if (it is WebLoadEvent.Failed || it == WebLoadEvent.ProcessTerminated) fail() }
        port.onNavigation = { WebNavigationDecision.Block }
    }

    fun bind(id: Long, state: RichContentReadingState, onEvent: (RichContentEvent) -> Unit) {
        check(leaseId == 0L && !broken && !destroyed)
        leaseId = id
        readingState = state
        callback = onEvent
        if (!started) {
            started = true
            scope.launch {
                try {
                    val html = withTimeout(15_000) { assembleTemplate(template, template.loadHtml(), loadBridge(), instanceId) }
                    port.loadHtml(html, template.baseUrl)
                    withTimeout(15_000) { ready.await() }
                } catch (failure: CancellationException) {
                    if (!destroyed) fail()
                    throw failure
                } catch (_: Exception) { fail() }
            }
        }
    }

    fun update(content: RichContentDocument, colors: RichContentTheme) {
        if (leaseId == 0L || broken || destroyed) return
        val changed = document != content
        val themeChanged = theme != colors
        if (document?.key != content.key) {
            reading = readingState?.bind(ReadingKey(template.id, template.revision, template.protocolVersion, content.key))
        }
        document = content
        theme = colors
        if (changed) {
            version++
            renderTimeout?.cancel()
            if (ready.isCompleted) render()
        } else if (themeChanged && ready.isCompleted) {
            send("updateTheme", themeJson(colors))
        }
    }

    private fun render() {
        val content = document ?: return
        val colors = theme ?: return
        renderRequest = send("render", buildJsonObject {
            put("html", content.html)
            put("baseUrl", content.baseUrl)
            put("scrollY", reading?.offset ?: 0.0)
            put("theme", themeJson(colors))
        })
        renderTimeout?.cancel()
        renderTimeout = scope.launch { delay(15_000); fail() }
    }

    private fun envelope(command: String, payload: JsonObject, id: Long) = buildJsonObject {
        put("protocol", template.protocolVersion)
        put("instance", instanceId)
        put("templateId", template.id)
        put("revision", template.revision)
        put("lease", leaseId)
        put("version", version)
        put("contentKey", document?.key ?: "")
        put("request", id)
        put("command", command)
        put("payload", payload)
    }

    private fun dispatch(message: JsonObject) {
        val encoded = Base64.encode(message.toString().encodeToByteArray())
        port.evaluate("window.AppContentHost.dispatch('$encoded');")
    }

    private fun send(command: String, payload: JsonObject = JsonObject(emptyMap())): Long {
        val id = ++requestId
        dispatch(envelope(command, payload, id))
        return id
    }

    private fun request(command: String): CompletableDeferred<JsonObject> {
        val id = ++requestId
        val message = envelope(command, JsonObject(emptyMap()), id)
        val result = CompletableDeferred<JsonObject>()
        pending[id] = Pending(message, result)
        dispatch(message)
        return result
    }

    private fun receive(raw: String) {
        if (broken || destroyed) return
        val message = runCatching { Json.parseToJsonElement(raw) as? JsonObject }.getOrNull() ?: return
        if (message.number("instance") != instanceId || message.number("protocol") != template.protocolVersion.toLong() ||
            message.text("templateId") != template.id || message.number("revision") != template.revision.toLong()
        ) return
        val event = message.text("event") ?: return
        if (event == "ready") {
            if (ready.complete(Unit) && leaseId != 0L) render()
            return
        }
        if (event == "protocolError") { fail(); return }
        val operation = message.number("request")?.let { pending[it] }
        if (operation != null && listOf("lease", "version", "contentKey").all { message[it] == operation.identity[it] }) {
            val expected = if (operation.identity.text("command") == "captureState") "state" else "reset"
            if (event == expected || event == "error") {
                pending.remove(message.number("request"))
                if (event == "error") operation.result.completeExceptionally(IllegalStateException("Template hook failed"))
                else operation.result.complete(message["data"] as? JsonObject ?: JsonObject(emptyMap()))
                return
            }
        }
        if (leaseId == 0L || message.number("lease") != leaseId || message.number("version") != version ||
            message.text("contentKey") != document?.key || message.number("request") != renderRequest
        ) return
        val data = message["data"] as? JsonObject ?: JsonObject(emptyMap())
        when (event) {
            "rendered" -> { renderTimeout?.cancel(); callback?.invoke(RichContentEvent.Rendered) }
            "scroll" -> (data["y"] as? JsonPrimitive)?.doubleOrNull?.let { reading?.update(it) }
            "linkClick" -> data.text("url")?.let { callback?.invoke(RichContentEvent.LinkClick(it)) }
            "imageClick" -> {
                val urls = (data["urls"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: return
                val index = (data["index"] as? JsonPrimitive)?.intOrNull ?: return
                if (index in urls.indices) callback?.invoke(RichContentEvent.ImageClick(urls, index))
            }
            "templateMessage" -> data.text("name")?.let {
                callback?.invoke(RichContentEvent.TemplateMessage(it, data["payload"]?.toString() ?: "null"))
            }
            "error" -> fail()
        }
    }

    private fun fail() {
        if (broken || destroyed) return
        broken = true
        renderTimeout?.cancel()
        callback?.invoke(RichContentEvent.Error)
    }

    override fun unbind() {
        if (leaseId == 0L) return
        if (ready.isCompleted && !broken && document != null) reading?.let { snapshot = it to request("captureState") }
        leaseId = 0L
        callback = null
        reading = null
        readingState = null
        document = null
        theme = null
        renderTimeout?.cancel()
        port.detach()
    }

    override suspend fun recycle(): Boolean {
        if (broken || destroyed || !ready.isCompleted) return false
        return withTimeoutOrNull(2_000) {
            snapshot?.let { (binding, result) ->
                (result.await()["scrollY"] as? JsonPrimitive)?.doubleOrNull?.let(binding::update)
            }
            snapshot = null
            request("reset").await()
            !broken && !destroyed
        } ?: false
    }

    override fun destroy() {
        if (destroyed) return
        destroyed = true
        callback = null
        readingState = null
        reading = null
        snapshot = null
        pending.values.forEach { it.result.cancel() }
        pending.clear()
        scope.cancel()
        port.onMessage = null
        port.onLoad = null
        port.onNavigation = null
        port.stopLoading()
        port.detach()
        port.destroy()
    }
}

private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.number(key: String) = (this[key] as? JsonPrimitive)?.longOrNull
private fun themeJson(theme: RichContentTheme) = buildJsonObject {
    put("background", theme.background)
    put("foreground", theme.foreground)
    put("link", theme.link)
    put("dark", theme.dark)
}
