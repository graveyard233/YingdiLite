package com.graveyard.feature.mine.auth

import com.graveyard.core.data.auth.AuthState
import com.graveyard.core.data.repository.AuthRepository
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Application-scoped submission; removing a login NavEntry does not cancel it. */
class MineLoginCoordinator(
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(MineLoginState())
    val state = mutableState.asStateFlow()
    val authState = authRepository.state

    internal fun loginWithPhone(countryCode: String, phone: String, password: String): Boolean =
        submit { authRepository.loginWithPhone(countryCode, phone, password) }

    internal fun loginWithAccount(account: String, password: String): Boolean =
        submit { authRepository.loginWithAccount(account, password) }

    /** Acknowledge only the failure that the feedback host is about to display. */
    internal fun consumeFailure(attemptId: Long): Boolean {
        val current = mutableState.value
        return current.attemptId == attemptId && current.error != null &&
            mutableState.compareAndSet(current, current.copy(error = null))
    }

    private fun submit(request: suspend () -> DataResult<Unit>): Boolean {
        val submission = reserveSubmission() ?: return false
        // The captured credentials belong only to this operation, never to observable/saved state.
        scope.launch {
            try {
                val error = when (val result = request()) {
                    is DataResult.Success -> null
                    is DataResult.Failure -> result.error
                }
                mutableState.update { current ->
                    if (current.attemptId == submission.attemptId) {
                        current.copy(isSubmitting = false, error = error)
                    } else current
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableState.update { current ->
                    if (current.attemptId == submission.attemptId) {
                        current.copy(isSubmitting = false, error = DataError.Unknown)
                    } else current
                }
            } finally {
                mutableState.update { current ->
                    if (current.attemptId == submission.attemptId) current.copy(isSubmitting = false) else current
                }
            }
        }
        return true
    }

    private fun reserveSubmission(): MineLoginState? {
        while (true) {
            val current = mutableState.value
            if (current.isSubmitting || authState.value != AuthState.Guest) return null
            val next = MineLoginState(attemptId = current.attemptId + 1, isSubmitting = true)
            if (mutableState.compareAndSet(current, next)) return next
        }
    }
}
