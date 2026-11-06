package com.graveyard.core.designsystem.navigation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AuthFlowSnapshot(
    val prompt: AuthPrompt? = null,
    val results: List<AuthResult> = emptyList(),
)

/** Saved UI hand-off state, not a persistent job queue or an executor for feature actions. */
class AuthFlowState(initial: AuthFlowSnapshot = AuthFlowSnapshot()) {
    var snapshot by mutableStateOf(initial)
        private set

    fun showPrompt(request: AuthRequest, message: String) {
        if (snapshot.prompt != null || snapshot.results.any {
                it.request.ownerTabKey == request.ownerTabKey && it.request.sourceEntryKey == request.sourceEntryKey
            }) return
        snapshot = snapshot.copy(prompt = AuthPrompt(request, message))
    }

    fun dismissPrompt() {
        snapshot = snapshot.copy(prompt = null)
    }

    fun deliver(request: AuthRequest, userId: Long) {
        if (snapshot.results.any { it.request.requestId == request.requestId }) return
        snapshot = snapshot.copy(results = snapshot.results + AuthResult(request, userId))
    }

    fun resultFor(owner: NavKey, source: NavKey): AuthResult? = snapshot.results.firstOrNull {
        it.request.ownerTabKey == owner && it.request.sourceEntryKey == source
    }

    /** Claim synchronously before starting work; already claimed network operations are never replayed. */
    fun take(requestId: String, owner: NavKey, source: NavKey, userId: Long): AuthResult? {
        val result = resultFor(owner, source)?.takeIf {
            it.request.requestId == requestId && it.userId == userId
        } ?: return null
        snapshot = snapshot.copy(results = snapshot.results.filterNot { it.request.requestId == requestId })
        return result
    }

    fun retainValid(stacks: Map<NavKey, List<NavKey>>, userId: Long?, restoring: Boolean) {
        fun exists(request: AuthRequest) = request.sourceEntryKey in stacks[request.ownerTabKey].orEmpty()
        val next = snapshot.copy(
            prompt = snapshot.prompt?.takeIf { exists(it.request) },
            results = snapshot.results.filter {
                exists(it.request) && (restoring || it.userId == userId)
            },
        )
        if (next != snapshot) snapshot = next
    }
}

@Composable
fun rememberAuthFlowState(json: Json): AuthFlowState = rememberSaveable(
    saver = Saver(
        save = { state: AuthFlowState -> json.encodeToString(AuthFlowSnapshot.serializer(), state.snapshot) },
        restore = { saved: String -> AuthFlowState(json.decodeFromString(AuthFlowSnapshot.serializer(), saved)) },
    ),
) { AuthFlowState() }
