@file:OptIn(ExperimentalMaterial3Api::class)

package com.wifiscanner.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wifiscanner.app.BuildConfig
import com.wifiscanner.app.R
import com.wifiscanner.app.domain.Validators
import com.wifiscanner.app.ui.components.ConfirmDialog
import com.wifiscanner.app.ui.components.InfoRow
import com.wifiscanner.app.ui.components.Pill
import com.wifiscanner.app.ui.components.ScreenHeader
import com.wifiscanner.app.ui.components.SectionTitle
import com.wifiscanner.app.ui.components.WsCard
import com.wifiscanner.app.ui.theme.ThemeMode
import com.wifiscanner.app.ui.theme.WsColors
import com.wifiscanner.app.utils.PermissionUtils
import com.wifiscanner.app.utils.openAppSettings
import com.wifiscanner.app.viewmodel.SettingsViewModel
import com.wifiscanner.app.viewmodel.appViewModelFactory

private enum class DataAction { SCANS, SPEED, SECURITY, CREDENTIALS }

@Composable
fun SettingsScreen(
    onOpenSaved: () -> Unit,
    vm: SettingsViewModel = viewModel(factory = appViewModelFactory())
) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    val autoRefresh by vm.autoRefresh.collectAsStateWithLifecycle()
    val server by vm.speedServer.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var language by remember { mutableStateOf(currentLanguage()) }
    var serverText by remember(server) { mutableStateOf(server) }
    var pending by remember { mutableStateOf<DataAction?>(null) }
    var locationGranted by remember { mutableStateOf(PermissionUtils.isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)) }
    var nearbyGranted by remember { mutableStateOf(hasNearbyPermission(context)) }
    var locationServiceOn by remember { mutableStateOf(PermissionUtils.isLocationServiceEnabled(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        locationGranted = PermissionUtils.isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        nearbyGranted = hasNearbyPermission(context)
        locationServiceOn = PermissionUtils.isLocationServiceEnabled(context)
    }
    val serverValid = Validators.isValidHttpsUrl(serverText)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader(stringResource(R.string.nav_settings), stringResource(R.string.settings_subtitle)) }

        item { SectionTitle(stringResource(R.string.settings_language)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("tg" to R.string.language_tajik, "en" to R.string.language_english).forEach { (tag, label) ->
                    FilterChip(
                        selected = language == tag,
                        onClick = {
                            language = tag
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                        },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
        }

        item { SectionTitle(stringResource(R.string.settings_theme)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ThemeMode.SYSTEM to R.string.theme_system,
                    ThemeMode.LIGHT to R.string.theme_light,
                    ThemeMode.DARK to R.string.theme_dark
                ).forEach { (mode, label) ->
                    FilterChip(selected = theme == mode, onClick = { vm.setTheme(mode) }, label = { Text(stringResource(label)) })
                }
            }
        }

        item { SectionTitle(stringResource(R.string.settings_auto_refresh)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to R.string.refresh_off, 30 to R.string.refresh_30, 60 to R.string.refresh_60, 120 to R.string.refresh_120)
                        .forEach { (seconds, label) ->
                            FilterChip(selected = autoRefresh == seconds, onClick = { vm.setAutoRefresh(seconds) }, label = { Text(stringResource(label)) })
                        }
                }
                Text(
                    stringResource(R.string.settings_auto_refresh_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { SectionTitle(stringResource(R.string.settings_speed_server)) }
        item {
            WsCard(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = serverText,
                    onValueChange = { serverText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !serverValid,
                    supportingText = { Text(stringResource(if (serverValid) R.string.settings_speed_server_hint else R.string.speed_err_invalid_url)) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.setSpeedServer(serverText) }, enabled = serverValid && serverText.trim().trimEnd('/') != server) {
                        Text(stringResource(R.string.save))
                    }
                    OutlinedButton(onClick = { vm.resetSpeedServer() }) { Text(stringResource(R.string.settings_reset)) }
                }
            }
        }

        item { SectionTitle(stringResource(R.string.settings_data)) }
        item {
            WsCard(Modifier.fillMaxWidth()) {
                DataRow(R.string.settings_clear_scans) { pending = DataAction.SCANS }
                DataRow(R.string.settings_clear_speed) { pending = DataAction.SPEED }
                DataRow(R.string.settings_clear_security) { pending = DataAction.SECURITY }
                DataRow(R.string.settings_delete_credentials, destructive = true) { pending = DataAction.CREDENTIALS }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = onOpenSaved) {
                    Icon(Icons.Filled.Key, contentDescription = null)
                    Text(stringResource(R.string.saved_title), Modifier.padding(start = 6.dp))
                }
            }
        }

        item { SectionTitle(stringResource(R.string.settings_permissions)) }
        item {
            WsCard(Modifier.fillMaxWidth()) {
                PermissionRow(R.string.settings_perm_location, locationGranted)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionRow(R.string.settings_perm_nearby, nearbyGranted)
                }
                PermissionRow(R.string.settings_perm_location_service, locationServiceOn)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { openAppSettings(context) }) { Text(stringResource(R.string.perm_open_settings)) }
            }
        }

        item { SectionTitle(stringResource(R.string.settings_privacy)) }
        item { TextCard(R.string.settings_privacy_text) }

        item { SectionTitle(stringResource(R.string.settings_troubleshooting)) }
        item { TextCard(R.string.settings_troubleshooting_text) }

        item { SectionTitle(stringResource(R.string.settings_about)) }
        item {
            WsCard(Modifier.fillMaxWidth()) {
                InfoRow(stringResource(R.string.settings_version), BuildConfig.VERSION_NAME)
                InfoRow(stringResource(R.string.settings_app_id), BuildConfig.APPLICATION_ID)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.settings_about_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { SectionTitle(stringResource(R.string.settings_licenses)) }
        item { TextCard(R.string.settings_licenses_text) }
    }

    pending?.let { action ->
        val (message, run) = when (action) {
            DataAction.SCANS -> R.string.confirm_clear_scans to { vm.clearScanHistory() }
            DataAction.SPEED -> R.string.confirm_clear_speed to { vm.clearSpeedResults() }
            DataAction.SECURITY -> R.string.confirm_clear_security to { vm.clearSecurityResults() }
            DataAction.CREDENTIALS -> R.string.confirm_delete_credentials to { vm.deleteAllCredentials() }
        }
        ConfirmDialog(
            title = stringResource(R.string.confirm_title),
            message = stringResource(message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                run()
                pending = null
            },
            onDismiss = { pending = null }
        )
    }
}

private fun currentLanguage(): String {
    val locales = AppCompatDelegate.getApplicationLocales()
    return if (locales.isEmpty) "tg" else locales[0]?.language ?: "tg"
}

private fun hasNearbyPermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        PermissionUtils.isGranted(context, Manifest.permission.NEARBY_WIFI_DEVICES)

@Composable
private fun DataRow(labelRes: Int, destructive: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(labelRes), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onClick) {
            Text(
                stringResource(R.string.delete),
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PermissionRow(labelRes: Int, granted: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(labelRes), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Pill(
            stringResource(if (granted) R.string.perm_status_granted else R.string.perm_status_denied),
            if (granted) WsColors.Success else WsColors.Danger
        )
    }
}

@Composable
private fun TextCard(textRes: Int) {
    WsCard(Modifier.fillMaxWidth()) {
        Text(stringResource(textRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
