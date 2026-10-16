package com.graveyard.core.designsystem.webview.pool

import com.graveyard.core.designsystem.webview.platform.WebViewConfiguration
import com.graveyard.core.designsystem.webview.platform.WebViewPort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class WebViewReuseKey(
    val mode: String,
    val configuration: WebViewConfiguration,
    val templateId: String? = null,
    val revision: Int = 0,
    val protocolVersion: Int = 0,
    val baseUrl: String? = null,
)

internal interface PooledWebViewEntry {
    val port: WebViewPort
    val broken: Boolean
    fun unbind()
    suspend fun recycle(): Boolean
    fun destroy()
}

/** Confined to the UI thread. Cleanup owns an entry until it explicitly confirms reuse. */
internal class WebViewPool(
    val scope: CoroutineScope,
    private val factory: (WebViewConfiguration) -> WebViewPort,
) {
    internal data class Lease(val id: Long, val key: WebViewReuseKey, val entry: PooledWebViewEntry)
    private var serial = 0L
    private var idle: Lease? = null
    private val active = mutableMapOf<Long, Lease>()
    private val cleaning = mutableSetOf<Lease>()
    private var closed = false

    fun acquire(key: WebViewReuseKey, create: (WebViewPort, Long) -> PooledWebViewEntry): Lease {
        check(!closed) { "WebViewPool is closed" }
        val cached = idle
        idle = null
        val id = ++serial
        val entry = if (cached != null && cached.key == key && !cached.entry.broken) cached.entry else {
            cached?.entry?.destroy()
            val port = factory(key.configuration)
            try { create(port, id) } catch (failure: Throwable) { port.destroy(); throw failure }
        }
        return Lease(id, key, entry).also { active[id] = it }
    }

    fun release(lease: Lease) {
        if (active[lease.id] !== lease) return
        active.remove(lease.id)
        lease.entry.unbind()
        if (closed || lease.entry.broken) { lease.entry.destroy(); return }
        cleaning += lease
        scope.launch {
            val reusable = try { lease.entry.recycle() } catch (cancelled: CancellationException) {
                if (cleaning.remove(lease)) lease.entry.destroy()
                throw cancelled
            } catch (_: Exception) { false }
            if (!cleaning.remove(lease)) return@launch
            if (reusable && !closed && !lease.entry.broken && idle == null) idle = lease
            else lease.entry.destroy()
        }
    }

    fun close() {
        if (closed) return
        closed = true
        (active.values + cleaning + listOfNotNull(idle)).map { it.entry }.toSet().forEach { it.destroy() }
        active.clear()
        cleaning.clear()
        idle = null
    }
}
