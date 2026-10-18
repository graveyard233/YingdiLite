package com.graveyard.core.designsystem.media

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import net.engawapg.lib.zoomable.ZoomState
import kotlin.math.abs

internal enum class ImageGalleryDismissPhase { Idle, Dragging, Returning, Dismissing }

@Stable
internal class ImageGalleryDismissState(
    private val scope: CoroutineScope,
    density: Density,
    private val onClose: () -> Unit,
) {
    private val chromeFadeDistance = with(density) { 80.dp.toPx() }
    private val minimumFlingDistance = with(density) { 32.dp.toPx() }
    private val minimumFlingVelocity = with(density) { 1000.dp.toPx() }
    private var animationJob: Job? = null
    private var animationGeneration = 0L

    var phase by mutableStateOf(ImageGalleryDismissPhase.Idle)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    var imageAlpha by mutableFloatStateOf(1f)
        private set
    var viewportSize by mutableStateOf(IntSize.Zero)
        private set
    var hasMultiplePointers by mutableStateOf(false)

    val isIdle: Boolean get() = phase == ImageGalleryDismissPhase.Idle
    val progress: Float get() = if (viewportSize.height > 0) {
        (abs(offset.y) / (0.5f * viewportSize.height)).coerceIn(0f, 1f)
    } else 0f
    val chromeAlpha: Float get() = 1f - (abs(offset.y) / chromeFadeDistance).coerceIn(0f, 1f)

    fun updateViewport(size: IntSize) {
        if (viewportSize == size) return
        reset()
        viewportSize = size
    }

    fun startDrag(initialOffset: Offset): Boolean {
        if (!isIdle) return false
        cancelAnimation()
        phase = ImageGalleryDismissPhase.Dragging
        offset = initialOffset
        return true
    }

    fun resumeDrag(): Boolean {
        if (phase != ImageGalleryDismissPhase.Returning) return false
        cancelAnimation()
        phase = ImageGalleryDismissPhase.Dragging
        return true
    }

    fun dragBy(delta: Offset) {
        if (phase == ImageGalleryDismissPhase.Dragging) {
            offset += delta
        }
    }

    fun release(velocity: Velocity) {
        if (phase != ImageGalleryDismissPhase.Dragging) return
        val distance = abs(offset.y)
        val outwardVelocity = if (offset.y > 0f) velocity.y else -velocity.y
        val shouldDismiss = distance >= viewportSize.height * 0.2f ||
            (distance >= minimumFlingDistance && outwardVelocity >= minimumFlingVelocity)
        if (!shouldDismiss) {
            returnToOrigin(velocity)
            return
        }

        cancelAnimation()
        phase = ImageGalleryDismissPhase.Dismissing
        val generation = animationGeneration
        val startOffset = offset
        val direction = if (startOffset.y > 0f) 1f else -1f
        val targetY = direction * maxOf(distance, viewportSize.height.toFloat())
        animationJob = scope.launch {
            animate(0f, 1f, animationSpec = tween(200)) { fraction, _ ->
                if (generation == animationGeneration && phase == ImageGalleryDismissPhase.Dismissing) {
                    offset = startOffset.copy(y = startOffset.y + (targetY - startOffset.y) * fraction)
                    imageAlpha = 1f - fraction
                }
            }
            coroutineContext.ensureActive()
            if (generation == animationGeneration && phase == ImageGalleryDismissPhase.Dismissing) {
                animationJob = null
                onClose()
            }
        }
    }

    fun returnToOrigin(velocity: Velocity = Velocity.Zero) {
        if (phase != ImageGalleryDismissPhase.Dragging) return
        cancelAnimation()
        phase = ImageGalleryDismissPhase.Returning
        val generation = animationGeneration
        val startOffset = offset
        animationJob = scope.launch {
            Animatable(startOffset, Offset.VectorConverter).animateTo(
                targetValue = Offset.Zero,
                animationSpec = spring(
                    dampingRatio = 1f,
                    stiffness = 400f,
                    visibilityThreshold = Offset.VisibilityThreshold,
                ),
                initialVelocity = Offset(velocity.x, velocity.y),
            ) {
                if (generation == animationGeneration && phase == ImageGalleryDismissPhase.Returning) {
                    offset = value
                }
            }
            coroutineContext.ensureActive()
            if (generation == animationGeneration && phase == ImageGalleryDismissPhase.Returning) {
                offset = Offset.Zero
                phase = ImageGalleryDismissPhase.Idle
                animationJob = null
            }
        }
    }

    fun close() {
        if (!isIdle) return
        phase = ImageGalleryDismissPhase.Dismissing
        onClose()
    }

    fun reset() {
        cancelAnimation()
        phase = ImageGalleryDismissPhase.Idle
        offset = Offset.Zero
        imageAlpha = 1f
    }

    private fun cancelAnimation() {
        // Invalidates callbacks before cancellation, including the old animation's completion.
        animationGeneration++
        animationJob?.cancel()
        animationJob = null
    }
}

/** This detector stays outside the transformed image so its coordinates never move with the drag. */
@OptIn(ExperimentalZoomableApi::class)
internal fun Modifier.imageGalleryDismissGestures(
    zoomState: ZoomState,
    dismissState: ImageGalleryDismissState,
    canStart: () -> Boolean,
): Modifier = pointerInput(zoomState, dismissState, dismissState.viewportSize) {
    awaitEachGesture {
        // Returning can be caught before Zoomable sees down; resting gestures leave down untouched.
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val returning = dismissState.phase == ImageGalleryDismissPhase.Returning
        if (down.isConsumed || down.type != PointerType.Touch || currentEvent.changes.size != 1 ||
            (!dismissState.isIdle && !returning) || dismissState.viewportSize.height == 0 ||
            dismissState.hasMultiplePointers || zoomState.scale != 1f || zoomState.isActive || !canStart()
        ) return@awaitEachGesture

        val velocityTracker = VelocityTracker()
        velocityTracker.addPosition(down.uptimeMillis, down.position)
        var dragging = false
        try {
            if (returning) {
                dragging = dismissState.resumeDrag()
                if (!dragging) return@awaitEachGesture
                down.consume()
            }
            // Zoomable normally consumes this down in Main; it does not establish drag ownership.
            awaitPointerEvent(PointerEventPass.Main)
            while (true) {
                // Owned gestures consume even up in Initial, preventing taps during a quick re-grab.
                val event = awaitPointerEvent(
                    if (dragging) PointerEventPass.Initial else PointerEventPass.Main,
                )
                val change = event.changes.singleOrNull()
                if (change == null || change.id != down.id || change.isConsumed ||
                    dismissState.hasMultiplePointers || !canStart() ||
                    zoomState.scale != 1f || zoomState.isActive
                ) {
                    if (dragging) {
                        dismissState.returnToOrigin()
                        event.changes.forEach { it.consume() }
                        consumeGalleryGestureUntilUp()
                    }
                    break
                }

                velocityTracker.addPosition(change.uptimeMillis, change.position)
                if (!change.pressed) {
                    if (dragging) {
                        dismissState.dragBy(change.positionChange())
                        change.consume()
                        dismissState.release(velocityTracker.calculateVelocity())
                    }
                    break
                }

                if (dragging) {
                    if (dismissState.phase != ImageGalleryDismissPhase.Dragging) {
                        change.consume()
                        consumeGalleryGestureUntilUp()
                        break
                    }
                    dismissState.dragBy(change.positionChange())
                    change.consume()
                } else {
                    val distance = change.position - down.position
                    val length = distance.getDistance()
                    if (length > viewConfiguration.touchSlop) {
                        if (abs(distance.y) <= abs(distance.x) * 1.2f) break
                        val overSlop = distance * (1f - viewConfiguration.touchSlop / length)
                        dragging = dismissState.startDrag(overSlop)
                        if (!dragging) break
                        change.consume()
                    } else {
                        // Pager runs after this page in Main; observe whether it took the gesture.
                        if (awaitPointerEvent(PointerEventPass.Final).changes.any { it.isConsumed }) break
                    }
                }
            }
        } finally {
            if (dragging) dismissState.returnToOrigin()
        }
    }
}

private suspend fun AwaitPointerEventScope.consumeGalleryGestureUntilUp() {
    while (currentEvent.changes.any { it.pressed }) {
        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
    }
}

/** Track pointers across the chrome, blocking dismissal and cancelled multi-touch sequences. */
internal fun Modifier.guardImageGalleryInput(state: ImageGalleryDismissState): Modifier =
    pointerInput(state) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            state.hasMultiplePointers = false
            var blocked = state.phase == ImageGalleryDismissPhase.Dismissing
            while (true) {
                val event = currentEvent
                if (event.changes.count { it.pressed } > 1) state.hasMultiplePointers = true
                if (state.hasMultiplePointers && !state.isIdle) {
                    blocked = true
                    state.returnToOrigin()
                }
                blocked = blocked || state.phase == ImageGalleryDismissPhase.Dismissing
                if (blocked) event.changes.forEach { it.consume() }
                if (event.changes.none { it.pressed }) break
                awaitPointerEvent(PointerEventPass.Initial)
            }
        }
    }
