package com.wifiscanner.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wifiscanner.app.R
import com.wifiscanner.app.domain.SecurityType
import com.wifiscanner.app.domain.Validators
import com.wifiscanner.app.network.ConnectFailure
import com.wifiscanner.app.network.ConnectState
import com.wifiscanner.app.ui.components.Banner
import com.wifiscanner.app.ui.components.BannerKind
import com.wifiscanner.app.ui.components.EmptyState
import com.wifiscanner.app.ui.components.LoadingRow
import com.wifiscanner.app.ui.components.PermissionCard
import com.wifiscanner.app.ui.components.Pill
import com.wifiscanner.app.ui.components.SignalBars
import com.wifiscanner.app.ui.components.SubScreenScaffold
import com.wifiscanner.app.ui.components.WsCard
import com.wifiscanner.app.ui.components.isWeak
import com.wifiscanner.app.ui.components.labelRes
import com.wifiscanner.app.ui.components.rememberWifiPermissionState
import com.wifiscanner.app.ui.theme.WsColors
import com.wifiscanner.app.utils.openWifiSettings
import com.wifiscanner.app.viewmodel.QuickConnectViewModel
import com.wifiscanner.app.viewmodel.appViewModelFactory

private fun needsPassword(type: SecurityType) =
    type == SecurityType.WPA2 || type == SecurityType.WPA3 || type == SecurityType.WPA2_WPA3

@Composable
fun QuickConnectScreen(
    initialSsid: String?,
    onBack: () -> Unit,
    vm: QuickConnectViewModel = viewModel(factory = appViewModelFactory())
) {
    val networks by vm.networks.collectAsStateWithLifecycle()
    val connectState by vm.connectState.collectAsStateWithLifecycle()
    val perm = rememberWifiPermissionState()
    val context = LocalContext.current

    var selected by remember { mutableStateOf(initialSsid) }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var save by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    LaunchedEffect(perm.granted) { if (perm.granted) vm.loadNetworks() }
    // A pending (not yet confirmed) request is cancelled when the screen is closed.
    DisposableEffect(Unit) {
        onDispose { if (vm.connectState.value is ConnectState.Requesting) vm.cancel() }
    }

    val selectedAp = networks.firstOrNull { it.ssid == selected }
    val security = selectedAp?.security
    val busy = connectState is ConnectState.Requesting

    SubScreenScaffold(title = stringResource(R.string.qc_title), onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Banner(BannerKind.INFO, stringResource(R.string.qc_explanation)) }
            if (!perm.granted) {
                item { PermissionCard(perm) }
                return@LazyColumn
            }
            if (networks.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.WifiOff,
                        title = stringResource(R.string.qc_no_networks_title),
                        message = stringResource(R.string.qc_no_networks_message),
                        actionLabel = stringResource(R.string.refresh),
                        onAction = { vm.loadNetworks() }
                    )
                }
            }
            items(networks, key = { it.ssid ?: it.bssid ?: "" }) { ap ->
                WsCard(Modifier.fillMaxWidth(), onClick = { selected = ap.ssid; password = ""; passwordError = false }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = ap.ssid == selected, onClick = { selected = ap.ssid; password = ""; passwordError = false })
                        Column(Modifier.weight(1f)) {
                            Text(ap.ssid ?: "", style = MaterialTheme.typography.titleMedium)
                            Pill(
                                stringResource(ap.security.labelRes()),
                                if (ap.security.isWeak()) WsColors.Danger else WsColors.Success
                            )
                        }
                        SignalBars(ap.rssi)
                    }
                }
            }

            if (selectedAp != null && security != null) {
                item {
                    WsCard(Modifier.fillMaxWidth()) {
                        Text(selectedAp.ssid ?: "", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        if (needsPassword(security)) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; passwordError = false },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.saved_password)) },
                                singleLine = true,
                                isError = passwordError,
                                supportingText = if (passwordError) {
                                    { Text(stringResource(R.string.saved_err_password)) }
                                } else null,
                                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { visible = !visible }) {
                                        Icon(
                                            if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = stringResource(R.string.saved_toggle_visibility)
                                        )
                                    }
                                }
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = save, onCheckedChange = { save = it })
                                Text(stringResource(R.string.qc_save_password), style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                stringResource(R.string.qc_authorized_only),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (security == SecurityType.OPEN) {
                            Text(stringResource(R.string.qc_open_network), style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(stringResource(R.string.qc_use_settings_type), style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val canRequest = needsPassword(security) || security == SecurityType.OPEN
                            if (canRequest) {
                                Button(
                                    enabled = !busy,
                                    onClick = {
                                        val ssid = selectedAp.ssid ?: return@Button
                                        if (needsPassword(security) && !Validators.isValidPassphrase(password)) {
                                            passwordError = true
                                        } else {
                                            vm.connect(ssid, password.ifEmpty { null }, security, save)
                                        }
                                    }
                                ) { Text(stringResource(R.string.qc_connect)) }
                            }
                            OutlinedButton(onClick = { openWifiSettings(context) }) {
                                Text(stringResource(R.string.open_wifi_settings))
                            }
                        }
                    }
                }
            }

            item {
                when (val s = connectState) {
                    is ConnectState.Idle -> Unit
                    is ConnectState.Requesting -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LoadingRow(stringResource(R.string.qc_requesting, s.ssid))
                        OutlinedButton(onClick = { vm.cancel() }) { Text(stringResource(R.string.cancel)) }
                    }
                    is ConnectState.Connected -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Banner(BannerKind.SUCCESS, stringResource(R.string.qc_connected, s.ssid))
                        Text(
                            stringResource(R.string.qc_connected_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = { vm.cancel() }) { Text(stringResource(R.string.qc_disconnect)) }
                    }
                    is ConnectState.UseSystemSettings -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Banner(BannerKind.INFO, stringResource(R.string.qc_use_settings, s.ssid))
                        Button(onClick = { openWifiSettings(context) }) { Text(stringResource(R.string.open_wifi_settings)) }
                    }
                    is ConnectState.Failed -> Banner(
                        BannerKind.ERROR,
                        stringResource(
                            when (s.reason) {
                                ConnectFailure.UNAVAILABLE -> R.string.qc_failed_unavailable
                                ConnectFailure.PASSWORD_REQUIRED -> R.string.qc_failed_password
                                ConnectFailure.ERROR -> R.string.qc_failed_error
                            }
                        )
                    )
                }
            }
        }
    }
}
