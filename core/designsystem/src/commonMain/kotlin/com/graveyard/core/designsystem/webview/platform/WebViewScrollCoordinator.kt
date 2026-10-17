package com.graveyard.core.designsystem.webview.platform

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** All distances and velocities use Compose pixels, with positive Y pointing down. */
internal class WebViewScrollCoordinator(
    private val dispatcher: NestedScrollDispatcher,
    private val scope: CoroutineScope,
    private val decay: DecayAnimationSpec<Float>,
) {
    private var fling: Job? = null
    private var generation = 0L
    var isFlinging: Boolean = false
        private set

    fun cancel() {
        // The same job owns dispatchPostFling, including any suspending parent snap animation.
        fling?.cancel()
        fling = null
        generation++
        isFlinging = false
    }

    fun preScroll(delta: Float, source: NestedScrollSource): Float =
        dispatcher.dispatchPreScroll(Offset(0f, delta), source).y

    fun postScroll(consumed: Float, available: Float, source: NestedScrollSource): Float =
        dispatcher.dispatchPostScroll(Offset(0f, consumed), Offset(0f, available), source).y

    fun scroll(delta: Float, source: NestedScrollSource, consumeContent: (Float) -> Float): Float {
        val remaining = delta - preScroll(delta, source)
        val consumed = consumeContent(remaining)
        val available = remaining - consumed
        return available - postScroll(consumed, available, source)
    }

    fun startFling(velocity: Float, consumeContent: (Float) -> Float) {
        cancel()
        val currentGeneration = generation
        isFlinging = true
        fling = scope.launch {
            try {
                val remaining = velocity - dispatcher.dispatchPreFling(Velocity(0f, velocity)).y
                var availableVelocity = remaining
                if (remaining != 0f) {
                    var previous = 0f
                    var hitBoundary = false
                    AnimationState(initialValue = 0f, initialVelocity = remaining).animateDecay(decay) {
                        val delta = value - previous
                        previous = value
                        val available = scroll(delta, NestedScrollSource.SideEffect, consumeContent)
                        availableVelocity = this.velocity
                        // Do not hand a whole frame's velocity to a parent for subpixel noise.
                        if (abs(available) > 0.5f) {
                            hitBoundary = true
                            cancelAnimation()
                        }
                    }
                    if (!hitBoundary) availableVelocity = 0f
                }
                isFlinging = false
                dispatcher.dispatchPostFling(
                    consumed = Velocity(0f, remaining - availableVelocity),
                    available = Velocity(0f, availableVelocity),
                )
            } finally {
                if (generation == currentGeneration) isFlinging = false
            }
        }
    }
}
