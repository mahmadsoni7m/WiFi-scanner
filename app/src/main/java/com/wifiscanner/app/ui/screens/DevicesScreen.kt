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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wifiscanner.app.R
import com.wifiscanner.app.domain.DeviceInfo
import com.wifiscanner.app.ui.components.Banner
import com.wifiscanner.app.ui.components.BannerKind
import com.wifiscanner.app.ui.components.EmptyState
import com.wifiscanner.app.ui.components.Pill
import com.wifiscanner.app.ui.components.ScreenHeader
import com.wifiscanner.app.ui.components.WsCard
import com.wifiscanner.app.viewmodel.DevicePhase
import com.wifiscanner.app.viewmodel.DeviceProblem
import com.wifiscanner.app.viewmodel.DevicesViewModel
import com.wifiscanner.app.viewmodel.appViewModelFactory

@Composable
fun DevicesScreen(vm: DevicesViewModel = viewModel(factory = appViewModelFactory())) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.checkNetwork() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.cancel() } // never keep scanning in the background

    val running = state.phase == DevicePhase.RUNNING

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader(stringResource(R.string.nav_devices), stringResource(R.string.devices_subtitle)) }
        item { Banner(BannerKind.INFO, stringResource(R.string.devices_authorized_notice)) }

        state.problem?.let { problem ->
            item {
                EmptyState(
                    icon = Icons.Filled.WifiOff,
                    title = stringResource(if (problem == DeviceProblem.NO_WIFI) R.string.devices_no_wifi_title else R.string.devices_not_private_title),
                    message = stringResource(if (problem == DeviceProblem.NO_WIFI) R.string.devices_no_wifi_message else R.string.devices_not_private_message),
                    actionLabel = stringResource(R.string.refresh),
                    onAction = { vm.checkNetwork() }
                )
            }
            return@LazyColumn
        }

        item {
            WsCard(Modifier.fillMaxWidth()) {
                val local = state.local
                Text(
                    stringResource(R.string.devices_network_line, local.ipv4 ?: "?", local.prefixLength ?: 24),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                if (running) {
                    Button(
                        onClick = { vm.cancel() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Text(stringResource(R.string.cancel), Modifier.padding(start = 6.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    val fraction = if (state.total > 0) state.done / state.total.toFloat() else 0f
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.devices_progress, state.done, state.total, state.devices.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Button(onClick = { vm.start() }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text(
                            stringResource(if (state.phase == DevicePhase.IDLE) R.string.devices_start else R.string.refresh),
                            Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }

        when (state.phase) {
            DevicePhase.CANCELLED -> item { Banner(BannerKind.INFO, stringResource(R.string.devices_cancelled)) }
            DevicePhase.TIMED_OUT -> item { Banner(BannerKind.WARNING, stringResource(R.string.devices_timed_out)) }
            DevicePhase.DONE -> item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Banner(BannerKind.SUCCESS, stringResource(R.string.devices_done, state.devices.size))
                    if (state.macsAvailable == false) {
                        Banner(BannerKind.INFO, stringResource(R.string.devices_mac_unavailable))
                    }
                }
            }
            else -> Unit
        }

        items(state.devices, key = { it.ip }) { device -> DeviceRow(device) }

        if (state.phase == DevicePhase.IDLE) {
            item {
                EmptyState(
                    icon = Icons.Filled.Devices,
                    title = stringResource(R.string.devices_idle_title),
                    message = stringResource(R.string.devices_idle_message)
                )
            }
        }

        item {
            Text(
                stringResource(R.string.devices_limitations),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceRow(device: DeviceInfo) {
    val unavailable = stringResource(R.string.unavailable)
    WsCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(device.ip, style = MaterialTheme.typography.titleMedium)
                Text(
                    device.hostname ?: stringResource(R.string.devices_hostname_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.devices_mac, device.mac ?: unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (device.isSelf) Pill(stringResource(R.string.devices_this_device), MaterialTheme.colorScheme.secondary)
                if (device.isGateway) Pill(stringResource(R.string.devices_router), MaterialTheme.colorScheme.primary)
            }
        }
    }
}
