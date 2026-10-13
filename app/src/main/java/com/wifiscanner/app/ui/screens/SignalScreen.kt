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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wifiscanner.app.R
import com.wifiscanner.app.domain.SignalQuality
import com.wifiscanner.app.domain.WifiMath
import com.wifiscanner.app.ui.components.Banner
import com.wifiscanner.app.ui.components.BannerKind
import com.wifiscanner.app.ui.components.ChartSeries
import com.wifiscanner.app.ui.components.EmptyState
import com.wifiscanner.app.ui.components.InfoRow
import com.wifiscanner.app.ui.components.LineChart
import com.wifiscanner.app.ui.components.PermissionCard
import com.wifiscanner.app.ui.components.Pill
import com.wifiscanner.app.ui.components.SignalBars
import com.wifiscanner.app.ui.components.SignalMeter
import com.wifiscanner.app.ui.components.SubScreenScaffold
import com.wifiscanner.app.ui.components.WsCard
import com.wifiscanner.app.ui.components.color
import com.wifiscanner.app.ui.components.guidanceRes
import com.wifiscanner.app.ui.components.labelRes
import com.wifiscanner.app.ui.components.rememberWifiPermissionState
import com.wifiscanner.app.viewmodel.SignalViewModel
import com.wifiscanner.app.viewmodel.appViewModelFactory
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.WifiOff

@Composable
fun SignalScreen(
    onBack: () -> Unit,
    vm: SignalViewModel = viewModel(factory = appViewModelFactory())
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val perm = rememberWifiPermissionState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Samples every 2 seconds while visible; nothing runs in the background.
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                vm.sample()
                delay(2_000)
            }
        }
    }

    SubScreenScaffold(
        title = stringResource(R.string.signal_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = { vm.sample() }) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
            }
        }
    ) { padding ->
        val snap = state.snapshot
        val conn = snap?.connection
        val rssi = conn?.rssi
        val unavailable = stringResource(R.string.unavailable)

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (snap != null && (!snap.wifiEnabled || !snap.connection.connected)) {
                item {
                    EmptyState(
                        icon = Icons.Filled.WifiOff,
                        title = stringResource(R.string.signal_not_connected_title),
                        message = stringResource(R.string.signal_not_connected_message)
                    )
                }
                return@LazyColumn
            }
            if (!perm.granted) item { PermissionCard(perm) }

            item {
                WsCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                rssi?.let { stringResource(R.string.fmt_dbm, it) } ?: unavailable,
                                style = MaterialTheme.typography.headlineMedium,
                                color = rssi?.let { WifiMath.classify(it).color() } ?: MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                conn?.ssid ?: unavailable,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SignalBars(rssi)
                    }
                    if (rssi != null) {
                        val quality = WifiMath.classify(rssi)
                        Spacer(Modifier.height(10.dp))
                        SignalMeter(rssi)
                        Spacer(Modifier.height(10.dp))
                        Pill(stringResource(quality.labelRes()), quality.color())
                    }
                    Spacer(Modifier.height(10.dp))
                    InfoRow(
                        stringResource(R.string.home_band),
                        conn?.frequencyMhz?.let {
                            stringResource(R.string.fmt_band_freq, stringResource(WifiMath.bandOf(it).labelRes()), it)
                        } ?: unavailable
                    )
                    InfoRow(
                        stringResource(R.string.home_link_speed),
                        conn?.linkSpeedMbps?.let { stringResource(R.string.fmt_mbps_0, it) } ?: unavailable
                    )
                }
            }

            item {
                WsCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.signal_history), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { vm.clearHistory() }, enabled = state.history.isNotEmpty()) {
                            Text(stringResource(R.string.signal_clear))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.history.isEmpty()) {
                        Text(
                            stringResource(R.string.signal_no_samples),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LineChart(
                            series = listOf(ChartSeries(state.history.map { it.toFloat() }, MaterialTheme.colorScheme.secondary)),
                            yMin = -100f,
                            yMax = -30f,
                            guides = listOf(-50f, -60f, -70f, -80f),
                            labelFormat = { it.toInt().toString() }
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.signal_chart_caption, state.history.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (rssi != null) {
                item {
                    val quality: SignalQuality = WifiMath.classify(rssi)
                    WsCard(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.signal_guidance_title), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(quality.guidanceRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Banner(BannerKind.INFO, stringResource(R.string.signal_disclaimer)) }
        }
    }
}
