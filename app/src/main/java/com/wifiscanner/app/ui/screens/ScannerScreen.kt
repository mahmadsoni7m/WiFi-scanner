@file:OptIn(ExperimentalMaterial3Api::class)

package com.wifiscanner.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wifiscanner.app.R
import com.wifiscanner.app.domain.WifiAccessPoint
import com.wifiscanner.app.domain.WifiBand
import com.wifiscanner.app.ui.components.Banner
import com.wifiscanner.app.ui.components.BannerKind
import com.wifiscanner.app.ui.components.EmptyState
import com.wifiscanner.app.ui.components.InfoRow
import com.wifiscanner.app.ui.components.LoadingRow
import com.wifiscanner.app.ui.components.PermissionCard
import com.wifiscanner.app.ui.components.Pill
import com.wifiscanner.app.ui.components.ScreenHeader
import com.wifiscanner.app.ui.components.SignalBars
import com.wifiscanner.app.ui.components.SignalMeter
import com.wifiscanner.app.ui.components.StatTile
import com.wifiscanner.app.ui.components.WsCard
import com.wifiscanner.app.ui.components.isWeak
import com.wifiscanner.app.ui.components.labelRes
import com.wifiscanner.app.ui.components.rememberWifiPermissionState
import com.wifiscanner.app.ui.theme.WsColors
import com.wifiscanner.app.utils.formatTime
import com.wifiscanner.app.utils.openLocationSettings
import com.wifiscanner.app.viewmodel.BandFilter
import com.wifiscanner.app.viewmodel.ScanNotice
import com.wifiscanner.app.viewmodel.ScannerViewModel
import com.wifiscanner.app.viewmodel.appViewModelFactory
import kotlinx.coroutines.delay

@Composable
fun ScannerScreen(
    onOpenSignal: () -> Unit,
    onConnect: (String?) -> Unit,
    vm: ScannerViewModel = viewModel(factory = appViewModelFactory())
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val autoSeconds by vm.autoRefreshSeconds.collectAsStateWithLifecycle()
    val perm = rememberWifiPermissionState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(perm.granted) {
        if (perm.granted) {
            vm.loadCached()
            vm.scan()
        }
    }
    // Optional automatic refresh. Only runs while the screen is visible; Android may still throttle scans.
    LaunchedEffect(autoSeconds, perm.granted) {
        if (autoSeconds > 0 && perm.granted) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(autoSeconds * 1000L)
                    vm.scan()
                }
            }
        }
    }

    val visible = state.visible

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(stringResource(R.string.nav_scanner), stringResource(R.string.scanner_subtitle)) {
                IconButton(onClick = onOpenSignal) {
                    Icon(Icons.Filled.SignalWifi4Bar, contentDescription = stringResource(R.string.signal_title))
                }
                IconButton(onClick = { vm.scan() }, enabled = perm.granted && !state.loading) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
                }
            }
        }

        if (!perm.granted) {
            item { PermissionCard(perm) }
            return@LazyColumn
        }

        item {
            val noticeText = when (state.notice) {
                ScanNotice.NONE -> null
                ScanNotice.THROTTLED -> R.string.scanner_throttled
                ScanNotice.PERMISSION -> R.string.scanner_permission_error
                ScanNotice.WIFI_OFF -> R.string.scanner_wifi_off
                ScanNotice.LOCATION_OFF -> R.string.scanner_location_off
            }
            if (noticeText != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Banner(
                        if (state.notice == ScanNotice.THROTTLED) BannerKind.INFO else BannerKind.WARNING,
                        stringResource(noticeText)
                    )
                    if (state.notice == ScanNotice.LOCATION_OFF) {
                        Button(onClick = { openLocationSettings(context) }) { Text(stringResource(R.string.open_location_settings)) }
                    }
                }
            }
        }

        item {
            Text(
                state.lastScanMs?.let { stringResource(R.string.scanner_last_scan, formatTime(it)) }
                    ?: stringResource(R.string.scanner_never_scanned),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(stringResource(R.string.scanner_total), state.all.size.toString(), Modifier.weight(1f))
                StatTile(stringResource(R.string.band_24), state.all.count { it.band == WifiBand.GHZ_2_4 }.toString(), Modifier.weight(1f), WsColors.Blue)
                StatTile(stringResource(R.string.band_5), state.all.count { it.band == WifiBand.GHZ_5 }.toString(), Modifier.weight(1f), WsColors.Success)
            }
        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.scanner_search)) }
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BandFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.band == filter,
                        onClick = { vm.setBand(filter) },
                        label = {
                            Text(
                                stringResource(
                                    when (filter) {
                                        BandFilter.ALL -> R.string.filter_all
                                        BandFilter.GHZ_2_4 -> R.string.band_24
                                        BandFilter.GHZ_5 -> R.string.band_5
                                        BandFilter.GHZ_6 -> R.string.band_6
                                    }
                                )
                            )
                        }
                    )
                }
            }
        }

        if (state.loading) {
            item { LoadingRow(stringResource(R.string.scanner_scanning)) }
        }

        if (visible.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    icon = Icons.Filled.WifiOff,
                    title = stringResource(R.string.scanner_empty_title),
                    message = stringResource(R.string.scanner_empty_message),
                    actionLabel = stringResource(R.string.refresh),
                    onAction = { vm.scan() }
                )
            }
        }

        items(visible, key = { (it.bssid ?: it.ssid ?: "") + it.frequencyMhz }) { ap ->
            NetworkCard(ap, onConnect = { onConnect(ap.ssid) })
        }
    }
}

@Composable
private fun NetworkCard(ap: WifiAccessPoint, onConnect: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val unavailable = stringResource(R.string.unavailable)
    WsCard(Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ap.ssid ?: stringResource(R.string.hidden_network), style = MaterialTheme.typography.titleMedium)
                Text(
                    ap.channel?.let { stringResource(R.string.fmt_channel, it) } ?: stringResource(R.string.channel_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                SignalBars(ap.rssi)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.fmt_dbm, ap.rssi), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        SignalMeter(ap.rssi)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(stringResource(ap.band.labelRes()), MaterialTheme.colorScheme.secondary)
            Pill(
                stringResource(ap.security.labelRes()),
                if (ap.security.isWeak()) WsColors.Danger else WsColors.Success
            )
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            InfoRow(stringResource(R.string.scanner_bssid), ap.bssid ?: unavailable)
            InfoRow(stringResource(R.string.scanner_frequency), stringResource(R.string.fmt_mhz, ap.frequencyMhz))
            InfoRow(stringResource(R.string.scanner_capabilities), ap.capabilities.ifBlank { unavailable })
            Spacer(Modifier.height(8.dp))
            if (ap.ssid != null) {
                Button(onClick = onConnect) { Text(stringResource(R.string.scanner_connect)) }
            }
        }
    }
}
