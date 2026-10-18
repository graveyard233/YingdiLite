package com.graveyard.core.designsystem.webview

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

data class RichContentTheme(val background: String, val foreground: String, val link: String, val dark: Boolean)

sealed interface RichContentEvent {
    data object Rendered : RichContentEvent
    data object Error : RichContentEvent
    data class LinkClick(val url: String) : RichContentEvent
    data class ImageClick(val urls: List<String>, val index: Int) : RichContentEvent
    data class TemplateMessage(val name: String, val payloadJson: String) : RichContentEvent
}

internal data class ReadingKey(val templateId: String, val revision: Int, val protocol: Int, val contentKey: String)

/** Route-owned positions. Only compact JSON snapshots cross the saved-state boundary. */
class RichContentReadingState(
    savedSnapshot: String? = null,
    private val onSnapshotChanged: (String) -> Unit = {},
) {
    private data class Position(var value: JsonObject = positionAtTop(), var owner: Long = 0L)
    private val positions = mutableMapOf<ReadingKey, Position>()
    private var serial = 0L

    init { savedSnapshot?.let(::restoreSnapshot) }

    fun exportSnapshot(): String = buildJsonObject {
        put("version", 1)
        put("positions", JsonArray(positions.map { (key, position) ->
            buildJsonObject {
                put("templateId", key.templateId)
                put("revision", key.revision)
                put("protocol", key.protocol)
                put("contentKey", key.contentKey)
                put("position", position.value)
            }
        }))
    }.toString()

    /** Restore before mounting a view; malformed or incompatible snapshots are ignored. */
    fun restoreSnapshot(snapshot: String) {
        val root = runCatching { Json.parseToJsonElement(snapshot) as? JsonObject }.getOrNull() ?: return
        if (root.int("version") != 1) return
        val entries = root["positions"] as? JsonArray ?: return
        positions.clear()
        entries.forEach { entry ->
            val item = entry as? JsonObject ?: return@forEach
            val key = ReadingKey(
                item.text("templateId") ?: return@forEach,
                item.int("revision") ?: return@forEach,
                item.int("protocol") ?: return@forEach,
                item.text("contentKey") ?: return@forEach,
            )
            val value = (item["position"] as? JsonObject)?.normalizedPosition() ?: return@forEach
            positions[key] = Position(value)
        }
    }

    internal inner class Binding(private val key: ReadingKey, private val token: Long) {
        val value: JsonObject get() = positions[key]?.value ?: positionAtTop()
        val offset: Double get() = value.double("scrollY") ?: 0.0
        var generation: Long = 0L
            private set

        fun update(value: JsonObject) {
            val position = positions[key] ?: return
            if (position.owner != token) return
            val normalized = value.normalizedPosition() ?: return
            if (position.value == normalized) return
            position.value = normalized
            generation++
            onSnapshotChanged(exportSnapshot())
        }
    }

    internal fun bind(key: ReadingKey): Binding {
        val token = ++serial
        positions.getOrPut(key) { Position() }.owner = token
        return Binding(key, token)
    }
}

private fun positionAtTop(): JsonObject = buildJsonObject { put("scrollY", 0.0) }
private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.int(key: String) = (this[key] as? JsonPrimitive)?.intOrNull
private fun JsonObject.double(key: String) = (this[key] as? JsonPrimitive)?.doubleOrNull

private fun JsonObject.normalizedPosition(): JsonObject? {
    val scrollY = double("scrollY")?.takeIf { it.isFinite() } ?: return null
    return buildJsonObject {
        put("scrollY", scrollY.coerceAtLeast(0.0))
        val anchor = this@normalizedPosition["anchor"] as? JsonObject
        val path = anchor?.get("path") as? JsonArray
        val marker = anchor?.text("marker")
        val fraction = anchor?.double("fraction")
        if (path != null && path.size in 1..64 && path.all {
                (it as? JsonPrimitive)?.intOrNull?.let { index -> index >= 0 } == true
            } && !marker.isNullOrEmpty() && marker.length <= 128 && fraction != null && fraction.isFinite()
        ) {
            put("anchor", buildJsonObject {
                put("path", path)
                put("marker", marker)
                put("fraction", fraction.coerceIn(0.0, 1.0))
            })
        }
    }
}
