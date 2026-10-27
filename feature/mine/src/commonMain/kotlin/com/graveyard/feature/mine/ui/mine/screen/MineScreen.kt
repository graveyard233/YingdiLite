package com.graveyard.feature.mine.ui.mine.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.graveyard.core.designsystem.icons.AppIcon
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.mine.generated.resources.Res
import yingdilite.feature.mine.generated.resources.mine_application
import yingdilite.feature.mine.generated.resources.mine_follow_system
import yingdilite.feature.mine.generated.resources.mine_guest_footer
import yingdilite.feature.mine.generated.resources.mine_preferences
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

private data class MineSetting(
    val title: StringResource,
    val subtitle: StringResource,
    val icon: AppIcon,
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
    onOpenLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        // The app navigation host already applies safe drawing insets.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.mine_title)) },
                windowInsets = WindowInsets(0, 0, 0, 0),
//                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item(key = "login-prompt") { LoginPromptCard(onOpenLogin = onOpenLogin) }
                item(key = "preferences") {
                    SettingsSection(title = stringResource(Res.string.mine_preferences), settings = preferenceSettings)
                }
                item(key = "application") {
                    SettingsSection(title = stringResource(Res.string.mine_application), settings = applicationSettings)
                }
                item(key = "footer") {
                    Text(
                        text = stringResource(Res.string.mine_guest_footer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginPromptCard(onOpenLogin: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.mine_welcome_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.mine_welcome_description),
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
                Text(stringResource(Res.string.mine_sign_in))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    settings: List<MineSetting>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
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
            .clickable(role = Role.Button) {
                // TODO: Connect this setting to its destination or action.
            }
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(setting.title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(setting.subtitle),
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
