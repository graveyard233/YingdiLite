package com.graveyard.feature.mine.ui.mine.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.graveyard.core.data.auth.AuthState
import com.graveyard.core.data.auth.AuthUser
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.model.user.UserLevelProgress
import com.graveyard.core.model.user.calculateUserLevelProgress
import com.graveyard.feature.mine.ui.mine.component.DailyTaskSection
import com.graveyard.feature.mine.viewmodel.DailyTasksUiState
import com.graveyard.feature.mine.viewmodel.MineUiState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.mine.generated.resources.Res
import yingdilite.feature.mine.generated.resources.mine_login_submitting
import yingdilite.feature.mine.generated.resources.mine_application
import yingdilite.feature.mine.generated.resources.mine_balance
import yingdilite.feature.mine.generated.resources.mine_content
import yingdilite.feature.mine.generated.resources.mine_experience
import yingdilite.feature.mine.generated.resources.mine_experience_label
import yingdilite.feature.mine.generated.resources.mine_data_unavailable
import yingdilite.feature.mine.generated.resources.mine_follow_system
import yingdilite.feature.mine.generated.resources.mine_followers
import yingdilite.feature.mine.generated.resources.mine_guest_footer
import yingdilite.feature.mine.generated.resources.mine_level_max
import yingdilite.feature.mine.generated.resources.mine_level_progress
import yingdilite.feature.mine.generated.resources.mine_logout
import yingdilite.feature.mine.generated.resources.mine_logout_cancel
import yingdilite.feature.mine.generated.resources.mine_logout_confirm
import yingdilite.feature.mine.generated.resources.mine_logout_message
import yingdilite.feature.mine.generated.resources.mine_logout_title
import yingdilite.feature.mine.generated.resources.mine_likes
import yingdilite.feature.mine.generated.resources.mine_my_favorites
import yingdilite.feature.mine.generated.resources.mine_my_favorites_summary
import yingdilite.feature.mine.generated.resources.mine_my_history
import yingdilite.feature.mine.generated.resources.mine_my_history_summary
import yingdilite.feature.mine.generated.resources.mine_my_posts
import yingdilite.feature.mine.generated.resources.mine_my_posts_summary
import yingdilite.feature.mine.generated.resources.mine_no_bio
import yingdilite.feature.mine.generated.resources.mine_notifications
import yingdilite.feature.mine.generated.resources.mine_preferences
import yingdilite.feature.mine.generated.resources.mine_profile_avatar
import yingdilite.feature.mine.generated.resources.mine_profile_edit
import yingdilite.feature.mine.generated.resources.mine_profile_id
import yingdilite.feature.mine.generated.resources.mine_profile_location
import yingdilite.feature.mine.generated.resources.mine_profile_unknown
import yingdilite.feature.mine.generated.resources.mine_profile_footer
import yingdilite.feature.mine.generated.resources.mine_setting_about
import yingdilite.feature.mine.generated.resources.mine_setting_about_summary
import yingdilite.feature.mine.generated.resources.mine_setting_appearance
import yingdilite.feature.mine.generated.resources.mine_setting_cache
import yingdilite.feature.mine.generated.resources.mine_setting_cache_summary
import yingdilite.feature.mine.generated.resources.mine_setting_content
import yingdilite.feature.mine.generated.resources.mine_setting_content_summary
import yingdilite.feature.mine.generated.resources.mine_setting_language
import yingdilite.feature.mine.generated.resources.mine_sign_in
import yingdilite.feature.mine.generated.resources.mine_title
import yingdilite.feature.mine.generated.resources.mine_welcome_description
import yingdilite.feature.mine.generated.resources.mine_welcome_title
import yingdilite.feature.mine.generated.resources.mine_following

private data class MineSetting(
    val title: StringResource,
    val subtitle: StringResource,
    val icon: AppIcon,
    val subtitleArgs: List<Any> = emptyList(),
)

private val preferenceSettings = listOf(
    MineSetting(Res.string.mine_setting_appearance, Res.string.mine_follow_system, AppIcon.Palette),
    MineSetting(Res.string.mine_setting_language, Res.string.mine_follow_system, AppIcon.Language),
    MineSetting(Res.string.mine_setting_content, Res.string.mine_setting_content_summary, AppIcon.Tune),
)

private val applicationSettings = listOf(
    MineSetting(Res.string.mine_setting_cache, Res.string.mine_setting_cache_summary, AppIcon.Storage),
    MineSetting(Res.string.mine_setting_about, Res.string.mine_setting_about_summary, AppIcon.Info),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(
    uiState: MineUiState,
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
    onRetryDailyTasks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    val isAuthenticated = uiState.authState is AuthState.Authenticated
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) showLogoutConfirmation = false
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        // 使用 Scaffold / TopAppBar 默认的系统栏 insets：TopAppBar 会延伸到状态栏后面，
        // 滚动变色时与状态栏一起变化。水平与底部导航栏的 insets 已由外层 AppNavigation 消费。
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.mine_title)) },
                scrollBehavior = scrollBehavior,
                actions = {
                    if (isAuthenticated) {
                        IconButton(onClick = { /* TODO: Open notifications. */ }) {
                            Icon(
                                AppIcon.Notifications.imageVector,
                                contentDescription = stringResource(Res.string.mine_notifications),
                            )
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (val authState = uiState.authState) {
                AuthState.Restoring -> CircularProgressIndicator(Modifier.padding(top = 48.dp))
                AuthState.Guest -> GuestContent(onOpenLogin, uiState.isLoggingIn)
                is AuthState.Authenticated -> SignedInContent(
                    userId = authState.userId,
                    user = authState.user,
                    dailyTasks = uiState.dailyTasks,
                    isLoggingOut = uiState.isLoggingOut,
                    onRetryDailyTasks = onRetryDailyTasks,
                    onRequestLogout = { showLogoutConfirmation = true },
                )
            }
        }
    }

    if (showLogoutConfirmation && isAuthenticated) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isLoggingOut) showLogoutConfirmation = false },
            title = { Text(stringResource(Res.string.mine_logout_title)) },
            text = { Text(stringResource(Res.string.mine_logout_message)) },
            confirmButton = {
                TextButton(enabled = !uiState.isLoggingOut, onClick = onLogout) {
                    Text(
                        text = stringResource(Res.string.mine_logout_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !uiState.isLoggingOut,
                    onClick = { showLogoutConfirmation = false },
                ) { Text(stringResource(Res.string.mine_logout_cancel)) }
            },
        )
    }
}

@Composable
private fun GuestContent(onOpenLogin: () -> Unit, isLoggingIn: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "login-prompt") { LoginPromptCard(onOpenLogin = onOpenLogin, isLoggingIn = isLoggingIn) }
            item(key = "preferences") {
                SettingsSection(title = stringResource(Res.string.mine_preferences), settings = preferenceSettings)
            }
            item(key = "application") {
                SettingsSection(title = stringResource(Res.string.mine_application), settings = applicationSettings)
            }
            item(key = "footer") { MineFooter(Res.string.mine_guest_footer) }
        }
    }
}

@Composable
private fun SignedInContent(
    userId: Long,
    user: AuthUser?,
    dailyTasks: DailyTasksUiState,
    isLoggingOut: Boolean,
    onRetryDailyTasks: () -> Unit,
    onRequestLogout: () -> Unit,
) {
    val contentSettings = listOf(
        MineSetting(
            title = Res.string.mine_my_posts,
            subtitle = if (user == null) Res.string.mine_data_unavailable else Res.string.mine_my_posts_summary,
            icon = AppIcon.Edit,
            subtitleArgs = if (user == null) emptyList() else listOf(user.postTotal),
        ),
        MineSetting(Res.string.mine_my_favorites, Res.string.mine_my_favorites_summary, AppIcon.Favorite),
        MineSetting(Res.string.mine_my_history, Res.string.mine_my_history_summary, AppIcon.Refresh),
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "profile") { ProfileHeader(userId, user) }
            item(key = "balance") { AccountBalance(user) }
            // 接口返回空列表时整块隐藏；加载中与失败仍保留占位，避免高度跳动。
            if (dailyTasks.tasks?.isEmpty() != true) {
                item(key = "daily-tasks") { DailyTaskSection(dailyTasks, onRetry = onRetryDailyTasks) }
            }
            item(key = "content") {
                SettingsSection(title = stringResource(Res.string.mine_content), settings = contentSettings)
            }
            item(key = "preferences") {
                SettingsSection(title = stringResource(Res.string.mine_preferences), settings = preferenceSettings)
            }
            item(key = "application") {
                SettingsSection(title = stringResource(Res.string.mine_application), settings = applicationSettings)
            }
            item(key = "logout") { LogoutButton(isLoggingOut = isLoggingOut, onClick = onRequestLogout) }
            item(key = "footer") { MineFooter(Res.string.mine_profile_footer) }
        }
    }
}

@Composable
private fun ProfileHeader(userId: Long, user: AuthUser?) {
    val displayName = user?.nickname?.takeIf(String::isNotBlank)
        ?: stringResource(Res.string.mine_profile_unknown)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(144.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            ) {
                AsyncImage(
                    model = user?.backgroundImage?.takeIf(String::isNotBlank),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp).offset(y = 30.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                ) {
                    Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                        val avatarUrl = user?.avatar?.takeIf(String::isNotBlank)
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = stringResource(Res.string.mine_profile_avatar, displayName),
                                modifier = Modifier.fillMaxSize().padding(3.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                imageVector = AppIcon.AccountCircle.imageVector,
                                contentDescription = null,
                                modifier = Modifier.size(52.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                IconButton(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp),
                    onClick = { /* TODO: Open profile editor. */ },
                ) {
                    Icon(
                        imageVector = AppIcon.Edit.imageVector,
                        contentDescription = stringResource(Res.string.mine_profile_edit),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 38.dp)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.mine_profile_id, user?.userId ?: userId),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    user?.ipHome?.takeIf { it.isNotBlank() && it != "未知地点" }?.let { location ->
                        Text(
                            text = stringResource(Res.string.mine_profile_location, location),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = user?.personalRemark?.takeIf(String::isNotBlank)
                        ?: stringResource(Res.string.mine_no_bio),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProfileStat(user?.postTotal, Res.string.mine_my_posts, Modifier.weight(1f))
                ProfileStat(user?.following, Res.string.mine_following, Modifier.weight(1f))
                ProfileStat(user?.follower, Res.string.mine_followers, Modifier.weight(1f))
                ProfileStat(user?.likeTotal, Res.string.mine_likes, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProfileStat(value: Long?, label: StringResource, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.heightIn(min = 52.dp)
            .clickable(role = Role.Button) { /* TODO: Open this profile collection. */ }
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value?.toString() ?: stringResource(Res.string.mine_data_unavailable),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun AccountBalance(user: AuthUser?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (user != null) {
                LevelProgress(calculateUserLevelProgress(user.creditsScore), user.creditsScore, Modifier.weight(1f))
            } else {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.mine_data_unavailable), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(Res.string.mine_experience_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(Modifier.height(52.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Row(
                modifier = Modifier
                    .clickable(role = Role.Button) { /* TODO: Open campfire balance details. */ }
                    .padding(start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = AppIcon.Fire.imageVector,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Column {
                    Text(
                        text = user?.fireCoin?.toString() ?: stringResource(Res.string.mine_data_unavailable),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.mine_balance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelProgress(progress: UserLevelProgress, credits: Long, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Lv.${progress.level}", style = MaterialTheme.typography.labelMedium)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.mine_experience, credits),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = progress.remainingToNextLevel?.let {
                        stringResource(Res.string.mine_level_progress, it)
                    } ?: stringResource(Res.string.mine_level_max),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val fraction = progress.nextLevelRequirement?.let { requirement ->
            (progress.progressInLevel.toFloat() / requirement.coerceAtLeast(1L)).coerceIn(0f, 1f)
        } ?: 1f
        Box(
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(fraction).height(5.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun LoginPromptCard(onOpenLogin: () -> Unit, isLoggingIn: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcon.AccountCircle.imageVector,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(Res.string.mine_welcome_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(Res.string.mine_welcome_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onOpenLogin,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                if (isLoggingIn) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
                Text(
                    text = stringResource(if (isLoggingIn) Res.string.mine_login_submitting else Res.string.mine_sign_in),
                    modifier = Modifier.padding(start = if (isLoggingIn) 8.dp else 0.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, settings: List<MineSetting>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            settings.forEachIndexed { index, setting ->
                SettingRow(setting)
                if (index != settings.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingRow(setting: MineSetting) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button) { /* TODO: Connect this setting to its destination or action. */ }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = setting.icon.imageVector,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(setting.title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(setting.subtitle, *setting.subtitleArgs.toTypedArray()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = AppIcon.ChevronRight.imageVector,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LogoutButton(isLoggingOut: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoggingOut,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
    ) {
        if (isLoggingOut) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onError)
        } else {
            Icon(AppIcon.Logout.imageVector, contentDescription = null, tint = MaterialTheme.colorScheme.onError)
        }
        Text(
            text = stringResource(Res.string.mine_logout),
            modifier = Modifier.padding(start = 8.dp),
            color = MaterialTheme.colorScheme.onError,
        )
    }
}

@Composable
private fun MineFooter(text: StringResource) {
    Text(
        text = stringResource(text),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
