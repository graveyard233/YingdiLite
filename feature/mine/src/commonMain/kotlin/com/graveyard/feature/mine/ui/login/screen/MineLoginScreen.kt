package com.graveyard.feature.mine.ui.login.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.feature.mine.model.LegalAgreement
import com.graveyard.feature.mine.ui.login.viewmodel.MineLoginViewModel
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.mine.generated.resources.Res
import yingdilite.feature.mine.generated.resources.mine_login_account
import yingdilite.feature.mine.generated.resources.mine_login_account_method
import yingdilite.feature.mine.generated.resources.mine_login_account_placeholder
import yingdilite.feature.mine.generated.resources.mine_login_agreement_notice
import yingdilite.feature.mine.generated.resources.mine_login_back
import yingdilite.feature.mine.generated.resources.mine_login_description
import yingdilite.feature.mine.generated.resources.mine_login_help
import yingdilite.feature.mine.generated.resources.mine_login_password
import yingdilite.feature.mine.generated.resources.mine_login_password_placeholder
import yingdilite.feature.mine.generated.resources.mine_login_phone
import yingdilite.feature.mine.generated.resources.mine_login_phone_method
import yingdilite.feature.mine.generated.resources.mine_login_phone_placeholder
import yingdilite.feature.mine.generated.resources.mine_login_region_label
import yingdilite.feature.mine.generated.resources.mine_login_select_region
import yingdilite.feature.mine.generated.resources.mine_login_title
import yingdilite.feature.mine.generated.resources.mine_login_welcome
import yingdilite.feature.mine.generated.resources.mine_region_hong_kong
import yingdilite.feature.mine.generated.resources.mine_region_macao
import yingdilite.feature.mine.generated.resources.mine_region_mainland_china
import yingdilite.feature.mine.generated.resources.mine_region_taiwan

private enum class LoginMethod {
    Phone,
    Account,
}

private enum class LoginRegion(val dialCode: String, val title: StringResource) {
    MainlandChina("+86", Res.string.mine_region_mainland_china),
    HongKong("+852", Res.string.mine_region_hong_kong),
    Macao("+853", Res.string.mine_region_macao),
    Taiwan("+886", Res.string.mine_region_taiwan),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineLoginScreen(
    onBack: () -> Unit,
    onOpenAgreement: (LegalAgreement) -> Unit,
    modifier: Modifier = Modifier,
) {
    var method by rememberSaveable { mutableStateOf(LoginMethod.Phone) }
    var region by rememberSaveable { mutableStateOf(LoginRegion.MainlandChina) }
    var phone by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable { mutableStateOf("") }
    // Entry-scoped memory survives an agreement visit; the password is never saved to disk.
    val loginViewModel = viewModel { MineLoginViewModel() }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.mine_login_title)) },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = AppIcon.ArrowBack.imageVector,
                            contentDescription = stringResource(Res.string.mine_login_back),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding).imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(Res.string.mine_login_welcome),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.mine_login_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                LoginMethodSwitcher(
                    selectedMethod = method,
                    onMethodSelected = { method = it },
                )

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (method == LoginMethod.Phone) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            LoginRegionSelector(
                                selectedRegion = region,
                                onRegionSelected = { region = it },
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(Res.string.mine_login_phone)) },
                                placeholder = { Text(stringResource(Res.string.mine_login_phone_placeholder)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = MaterialTheme.shapes.large,
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = account,
                            onValueChange = { account = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(Res.string.mine_login_account)) },
                            placeholder = { Text(stringResource(Res.string.mine_login_account_placeholder)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            shape = MaterialTheme.shapes.large,
                        )
                    }

                    OutlinedTextField(
                        value = loginViewModel.password,
                        onValueChange = { loginViewModel.password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.mine_login_password)) },
                        placeholder = { Text(stringResource(Res.string.mine_login_password_placeholder)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = MaterialTheme.shapes.large,
                    )

                    Button(
                        onClick = {
                            // TODO: Connect the selected login method to the authentication service.
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Text(stringResource(Res.string.mine_login_title))
                    }

                    TextButton(
                        onClick = {
                            // TODO: Open existing-account recovery guidance.
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(Res.string.mine_login_help))
                    }
                }

                LoginAgreements(
                    onOpenAgreement = { agreement ->
                        focusManager.clearFocus()
                        onOpenAgreement(agreement)
                    },
                )
            }
        }
    }
}

@Composable
private fun LoginRegionSelector(
    selectedRegion: LoginRegion,
    onRegionSelected: (LoginRegion) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val highlighted = expanded || focused
    val regionName = stringResource(selectedRegion.title)
    val selectRegionDescription = stringResource(Res.string.mine_login_select_region)
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "Login region arrow",
    )

    Box(
        modifier = modifier
            .widthIn(min = 104.dp)
            .width(IntrinsicSize.Max)
            .padding(top = 8.dp),
    ) {
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .onFocusChanged { focused = it.isFocused }
                .semantics {
                    contentDescription = selectRegionDescription
                    stateDescription = "$regionName ${selectedRegion.dialCode}"
                },
            contentPadding = PaddingValues(horizontal = 8.dp),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) colors.primary else colors.outline,
            ),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedRegion.dialCode,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
                Icon(
                    imageVector = AppIcon.ExpandMore.imageVector,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = arrowRotation },
                    tint = colors.onSurfaceVariant,
                )
            }
        }
        Text(
            text = stringResource(Res.string.mine_login_region_label),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 12.dp, y = (-8).dp)
                .background(colors.background)
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (highlighted) colors.primary else colors.onSurfaceVariant,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(232.dp),
            offset = DpOffset(x = 0.dp, y = 4.dp),
            shape = MaterialTheme.shapes.medium,
            containerColor = colors.surfaceContainer,
        ) {
            LoginRegion.entries.forEach { option ->
                val isSelected = option == selectedRegion
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = stringResource(option.title), modifier = Modifier.weight(1f))
                            Text(text = option.dialCode, color = colors.onSurfaceVariant)
                        }
                    },
                    onClick = {
                        expanded = false
                        if (!isSelected) onRegionSelected(option)
                    },
                    modifier = Modifier
                        .background(if (isSelected) colors.secondaryContainer else Color.Transparent)
                        .semantics { selected = isSelected },
                    trailingIcon = {
                        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                Icon(
                                    imageVector = AppIcon.Check.imageVector,
                                    contentDescription = null,
                                    tint = colors.primary,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LoginAgreements(onOpenAgreement: (LegalAgreement) -> Unit) {
    val agreements = LegalAgreement.entries.map { it to stringResource(it.title) }
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
        focusedStyle = SpanStyle(background = MaterialTheme.colorScheme.secondaryContainer),
        pressedStyle = SpanStyle(background = MaterialTheme.colorScheme.secondaryContainer),
    )
    val links = buildAnnotatedString {
        agreements.forEachIndexed { index, (agreement, title) ->
            if (index > 0) append(" · ")
            withLink(
                LinkAnnotation.Clickable(
                    tag = agreement.name,
                    styles = linkStyles,
                    linkInteractionListener = { onOpenAgreement(agreement) },
                ),
            ) { append(title) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.mine_login_agreement_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        // Inline links wrap with the text, including long translated titles and large fonts.
        Text(
            text = links,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoginMethodSwitcher(
    selectedMethod: LoginMethod,
    onMethodSelected: (LoginMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val darkTheme = colors.surface.luminance() < 0.5f
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val switchState = remember { LoginMethodSwitchState(selectedMethod) }
    val coroutineScope = rememberCoroutineScope()
    val currentMethod by rememberUpdatedState(selectedMethod)
    val selectMethod by rememberUpdatedState(onMethodSelected)

    LaunchedEffect(selectedMethod) {
        // A drag already starts its own spring before publishing the new selection.
        if (!switchState.isDragging && switchState.targetMethod != selectedMethod) {
            switchState.animateTo(selectedMethod, coroutineScope)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.surfaceContainer)
            .padding(4.dp),
    ) {
        // This layer follows the tab row's measured height without affecting its size.
        Box(modifier = Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = size.width * switchState.indicatorProgress * direction
                    }
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .background(
                        color = if (darkTheme) colors.surfaceContainerHigh else colors.surfaceContainerLowest,
                        shape = CircleShape,
                    )
                    .then(
                        if (darkTheme) Modifier.border(
                            width = 1.dp,
                            color = colors.outlineVariant.copy(alpha = 0.45f),
                            shape = CircleShape,
                        ) else Modifier,
                    ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .selectableGroup()
                .pointerInput(direction) {
                    val flingThreshold = 500.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val velocityTracker = VelocityTracker()
                        var trackedPointer = down.id
                        velocityTracker.addPosition(down.uptimeMillis, down.position)
                        var overSlop = 0f
                        // Leave taps to selectable and vertical drags to the page scroll.
                        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, amount ->
                            change.consume()
                            overSlop = amount
                        } ?: return@awaitEachGesture

                        val travel = (size.width / 2f).coerceAtLeast(1f)
                        switchState.startDrag()
                        try {
                            switchState.dragBy(overSlop * direction / travel)
                            if (drag.id != trackedPointer) {
                                velocityTracker.resetTracking()
                                trackedPointer = drag.id
                            }
                            velocityTracker.addPosition(drag.uptimeMillis, drag.position)
                            val released = horizontalDrag(drag.id) { change ->
                                if (change.id != trackedPointer) {
                                    velocityTracker.resetTracking()
                                    trackedPointer = change.id
                                }
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                switchState.dragBy(change.positionChange().x * direction / travel)
                                change.consume()
                            }

                            if (released) {
                                // Include the up event so pausing before release is not a fling.
                                currentEvent.changes.firstOrNull { it.previousPressed && !it.pressed }
                                    ?.let {
                                        if (it.id != trackedPointer) velocityTracker.resetTracking()
                                        velocityTracker.addPosition(it.uptimeMillis, it.position)
                                        switchState.dragBy(it.positionChange().x * direction / travel)
                                    }
                                val velocity = velocityTracker.calculateVelocity().x * direction
                                val target = when {
                                    velocity >= flingThreshold -> LoginMethod.Account
                                    velocity <= -flingThreshold -> LoginMethod.Phone
                                    switchState.progress >= 0.5f -> LoginMethod.Account
                                    else -> LoginMethod.Phone
                                }
                                switchState.animateTo(
                                    method = target,
                                    scope = coroutineScope,
                                    releaseVelocity = velocity / travel,
                                )
                                if (target != currentMethod) selectMethod(target)
                            } else {
                                switchState.animateTo(currentMethod, coroutineScope, releaseVelocity = 0f)
                            }
                        } finally {
                            // Restore the committed selection if the gesture coroutine is cancelled.
                            if (switchState.isDragging) {
                                switchState.animateTo(currentMethod, coroutineScope, releaseVelocity = 0f)
                            }
                        }
                    }
                },
        ) {
            LoginMethod.entries.forEach { method ->
                val selected = method == selectedMethod
                val contentColor by animateColorAsState(
                    targetValue = if (selected) colors.primary else colors.secondary,
                    animationSpec = tween(durationMillis = 180),
                    label = "Login method content",
                )
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .selectable(
                            selected = selected,
                            interactionSource = interactionSource,
                            // Switching uses the moving capsule; only reselecting shows a ripple.
                            indication = if (selected && !switchState.isDragging) {
                                ripple(color = colors.primary)
                            } else null,
                            role = Role.Tab,
                            onClick = { if (!selected) onMethodSelected(method) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (method == LoginMethod.Phone) loginPhoneIcon else loginAccountIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = contentColor,
                        )
                        Text(
                            text = stringResource(
                                if (method == LoginMethod.Phone) Res.string.mine_login_phone_method
                                else Res.string.mine_login_account_method,
                            ),
                            color = contentColor,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

private class LoginMethodSwitchState(initialMethod: LoginMethod) {
    var progress by mutableFloatStateOf(if (initialMethod == LoginMethod.Phone) 0f else 1f)
        private set
    var isDragging by mutableStateOf(false)
        private set
    var targetMethod = initialMethod
        private set

    private var animationJob: Job? = null

    val indicatorProgress: Float
        get() {
            val bounded = progress.coerceIn(0f, 1f)
            val overflow = progress - bounded
            // Apply the same resistance to dragging and settling, so re-grabbing never jumps.
            // Overscroll approaches, but never exceeds, 8% of one tab's width.
            return bounded + overflow / (1f + abs(overflow) / 0.08f)
        }

    fun startDrag() {
        animationJob?.cancel()
        isDragging = true
    }

    fun dragBy(delta: Float) {
        progress += delta
    }

    fun animateTo(method: LoginMethod, scope: CoroutineScope, releaseVelocity: Float? = null) {
        animationJob?.cancel()
        isDragging = false
        targetMethod = method
        val target = if (method == LoginMethod.Phone) 0f else 1f
        val start = progress
        if (start == target) return

        animationJob = scope.launch {
            animate(
                initialValue = start,
                targetValue = target,
                initialVelocity = releaseVelocity?.coerceIn(-4f, 4f) ?: 0f,
                animationSpec = if (releaseVelocity != null) {
                    spring(dampingRatio = 0.86f, stiffness = 500f, visibilityThreshold = 0.001f)
                } else {
                    tween(
                        durationMillis = 380,
                        easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1.06f),
                    )
                },
            ) { value, _ -> progress = value }
            progress = target
        }
    }
}

// Local to this control; no changes to the shared icon registry are needed.
private val loginPhoneIcon = loginMethodIcon(
    name = "LoginPhone",
    path = "M8 2h8a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Z M11 18h2",
)

private val loginAccountIcon = loginMethodIcon(
    name = "LoginAccount",
    path = "M9 6H5a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-4 " +
        "M9 3h6v5H9Z M9 11a2 2 0 1 0 0 4 2 2 0 0 0 0-4Z M6 18a3 3 0 0 1 6 0 M15 12h3 M15 16h3",
)

private fun loginMethodIcon(name: String, path: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(path),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
