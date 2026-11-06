package com.graveyard.core.designsystem.navigation.auth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import yingdilite.core.designsystem.generated.resources.Res
import yingdilite.core.designsystem.generated.resources.auth_cancel
import yingdilite.core.designsystem.generated.resources.auth_go_to_login
import yingdilite.core.designsystem.generated.resources.auth_required_title

@Composable
fun AuthRequiredDialog(message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.auth_required_title)) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.auth_go_to_login)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.auth_cancel)) } },
    )
}
