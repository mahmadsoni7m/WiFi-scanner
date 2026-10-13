package com.wifiscanner.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wifiscanner.app.R
import com.wifiscanner.app.domain.Transport
import com.wifiscanner.app.domain.WifiMath
import com.wifiscanner.app.navigation.Routes
import com.wifiscanner.app.network.NetworkSnapshot
import com.wifiscanner.app.ui.components.Banner
import com.wifiscanner.app.ui.components.BannerKind
import com.wifiscanner.app.ui.components.GradientCard
import com.wifiscanner.app.ui.components.InfoRow
import com.wifiscanner.app.ui.components.PermissionCard
import com.wifiscanner.app.ui.components.Pill
import com.wifiscanner.app.ui.components.ScreenHeader
import com.wifiscanner.app.ui.components.SectionTitle
import com.wifiscanner.app.ui.components.SignalBars
import com.wifiscanner.app.ui.components.StatTile
import com.wifiscanner.app.ui.components.WsCard
import com.wifiscanner.app.ui.components.color
import com.wifiscanner.app.ui.components.labelRes
import com.wifiscanner.app.ui.components.rememberWifiPermissionState
import com.wifiscanner.app.ui.theme.WsColors
import com.wifiscanner.app.utils.formatDateTime
import com.wifiscanner.app.utils.openLocationSettings
import com.wifiscanner.app.utils.openWifiSettings
import com.wifiscanner.app.viewmodel.HomeViewModel
import com.wifiscanner.app.viewmodel.appViewModelFactory
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    vm: HomeViewModel = viewModel(factory = appViewModelFactory())
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val perm = rememberWifiPermissionState()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    LaunchedEffect(perm.granted) { vm.refresh() }

    val snap = state.live.snapshot
    val unavailable = stringResource(R.string.unavailable)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader(stringResource(R.string.app_name), stringResource(R.string.home_subtitle)) }

        item {
            GradientCard(Modifier.fillMaxWidth()) {
                when {
                    snap == null -> HeroText(stringResource(R.string.loading), big = true)
                    !snap.wifiEnabled -> {
                        HeroText(stringResource(R.string.home_wifi_off_title), big = true)
                        Spacer(Modifier.height(4.dp))
                        HeroText(stringResource(R.string.home_wifi_off_message))
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { openWifiSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = WsColors.GradientStart)
                        ) { Text(stringResource(R.string.open_wifi_settings)) }
                    }
                    !snap.connection.connected -> {
                        HeroText(stringResource(R.string.home_not_connected), big = true)
                        Spacer(Modifier.height(4.dp))
                        HeroText(stringResource(R.string.home_not_connected_message))
                    }
                    else -> ConnectedHero(snap, unavailable)
                }
            }
        }

        if (snap != null && snap.wifiEnabled && !perm.granted) {
            item { PermissionCard(perm) }
        } else if (snap != null && snap.wifiEnabled && !state.live.locationOn) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Banner(BannerKind.WARNING, stringResource(R.string.home_location_off))
                    Button(onClick = { openLocationSettings(context) }) { Text(stringResource(R.string.open_location_settings)) }
                }
            }
        }

        item { InternetCard(snap) }

        item { SectionTitle(stringResource(R.string.home_quick_actions)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(Icons.Filled.Wifi, stringResource(R.string.nav_scanner), { onNavigate(Routes.SCANNER) }, Modifier.weight(1f))
                    QuickAction(Icons.Filled.Speed, stringResource(R.string.nav_speed), { onNavigate(Routes.SPEED) }, Modifier.weight(1f))
                    QuickAction(Icons.Filled.Security, stringResource(R.string.nav_security), { onNavigate(Routes.SECURITY) }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(Icons.Filled.SignalWifi4Bar, stringResource(R.string.signal_title), { onNavigate(Routes.SIGNAL) }, Modifier.weight(1f))
                    QuickAction(Icons.Filled.Key, stringResource(R.string.saved_title), { onNavigate(Routes.SAVED) }, Modifier.weight(1f))
                    QuickAction(Icons.Filled.Link, stringResource(R.string.qc_title), { onNavigate(Routes.QUICK_CONNECT) }, Modifier.weight(1f))
                }
            }
        }

        item { SectionTitle(stringResource(R.string.home_recent_scan)) }
        item {
            WsCard(Modifier.fillMaxWidth()) {
                val top = state.live.top
                if (top.isEmpty()) {
                    Text(
                        stringResource(if (perm.granted) R.string.home_no_scan else R.string.home_scan_needs_permission),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    top.forEach { ap ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(ap.ssid ?: stringResource(R.string.hidden_network), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.fmt_dbm, ap.rssi), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(10.dp))
                            SignalBars(ap.rssi)
                        }
                    }
                }
                state.lastScan?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.home_last_scan, formatDateTime(it.timestamp), it.networkCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onNavigate(Routes.SCANNER) }) { Text(stringResource(R.string.home_open_scanner)) }
            }
        }

        item { SectionTitle(stringResource(R.string.home_last_speed)) }
        item {
            WsCard(Modifier.fillMaxWidth()) {
                val speed = state.lastSpeed
                if (speed == null) {
                    Text(stringResource(R.string.home_no_speed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(stringResource(R.string.speed_download), stringResource(R.string.fmt_mbps_1, speed.downloadMbps), Modifier.weight(1f))
                        StatTile(stringResource(R.string.speed_upload), stringResource(R.string.fmt_mbps_1, speed.uploadMbps), Modifier.weight(1f), WsColors.Blue)
                        StatTile(stringResource(R.string.speed_latency), stringResource(R.string.fmt_ms_0, speed.latencyMs), Modifier.weight(1f), WsColors.Success)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(formatDateTime(speed.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onNavigate(Routes.SPEED) }) { Text(stringResource(R.string.speed_start)) }
            }
        }

        item { SectionTitle(stringResource(R.string.home_security_summary)) }
        item {
            WsCard(Modifier.fillMaxWidth()) {
                val sec = state.lastSecurity
                if (sec == null) {
                    Text(stringResource(R.string.home_no_security), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(stringResource(R.string.home_sec_verified, sec.verified), WsColors.Success)
                        Pill(stringResource(R.string.home_sec_warnings, sec.warnings), WsColors.Warning)
                        Pill(stringResource(R.string.home_sec_unavailable, sec.unavailable), MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(formatDateTime(sec.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onNavigate(Routes.SECURITY) }) { Text(stringResource(R.string.sec_run_check)) }
            }
        }
    }
}

@Composable
private fun HeroText(text: String, big: Boolean = false) {
    Text(
        text,
        style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = if (big) 1f else 0.85f)
    )
}

@Composable
private fun ConnectedHero(snap: NetworkSnapshot, unavailable: String) {
    val c = snap.connection
    val quality = c.rssi?.let { WifiMath.classify(it) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            HeroText(stringResource(R.string.home_connected_to), big = false)
            Text(
                c.ssid ?: unavailable,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
        }
        SignalBars(c.rssi, Modifier.size(40.dp, 32.dp))
    }
    Spacer(Modifier.height(10.dp))
    if (quality != null) {
        Pill(stringResource(quality.labelRes()), Color.White)
        Spacer(Modifier.height(8.dp))
    }
    HeroRow(stringResource(R.string.home_signal), c.rssi?.let { stringResource(R.string.fmt_dbm, it) } ?: unavailable)
    HeroRow(
        stringResource(R.string.home_band),
        c.frequencyMhz?.let {
            stringResource(R.string.fmt_band_freq, stringResource(WifiMath.bandOf(it).labelRes()), it)
        } ?: unavailable
    )
    HeroRow(stringResource(R.string.home_link_speed), c.linkSpeedMbps?.let { stringResource(R.string.fmt_mbps_0, it) } ?: unavailable)
    HeroRow(stringResource(R.string.home_local_ip), snap.local.ipv4 ?: unavailable)
    if (c.ssid == null || c.rssi == null || snap.local.ipv4 == null) {
        Spacer(Modifier.height(8.dp))
        HeroText(stringResource(R.string.home_unavailable_note))
    }
}

@Composable
private fun HeroRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.End)
    }
}

@Composable
private fun InternetCard(snap: NetworkSnapshot?) {
    WsCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_internet), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            when {
                snap == null -> Unit
                snap.captivePortal -> Pill(stringResource(R.string.home_internet_portal), WsColors.Warning)
                snap.internetValidated -> Pill(stringResource(R.string.home_internet_ok), WsColors.Success)
                else -> Pill(stringResource(R.string.home_internet_none), WsColors.Danger)
            }
        }
        if (snap != null) {
            val transport = when (snap.transport) {
                Transport.WIFI -> R.string.transport_wifi
                Transport.CELLULAR -> R.string.transport_cellular
                Transport.ETHERNET -> R.string.transport_ethernet
                Transport.OTHER -> R.string.transport_other
                Transport.NONE -> R.string.transport_none
            }
            InfoRow(stringResource(R.string.home_connection_type), stringResource(transport))
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    WsCard(modifier, onClick = onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}
