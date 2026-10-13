package com.wifiscanner.app.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.wifiscanner.app.domain.WifiAccessPoint
import com.wifiscanner.app.domain.WifiMath
import com.wifiscanner.app.utils.PermissionUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [fresh] is false when Android refused the scan (throttling) or it timed out;
 * the list is then the system's cached results.
 */
data class ScanOutcome(val results: List<WifiAccessPoint>, val fresh: Boolean, val timestampMs: Long)

/** Real Wi-Fi scanning through WifiManager. Android throttles foreground apps to 4 scans per 2 minutes. */
class WifiScanner(context: Context) {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @SuppressLint("MissingPermission")
    fun cachedResults(): List<WifiAccessPoint> {
        if (!PermissionUtils.hasWifiPermissions(appContext)) return emptyList()
        return try {
            wifiManager.scanResults.map { it.toAccessPoint() }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /** @throws SecurityException when the permissions are missing. */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    suspend fun scan(): ScanOutcome {
        if (!PermissionUtils.hasWifiPermissions(appContext)) {
            throw SecurityException("Wi-Fi permissions not granted")
        }
        val updated = CompletableDeferred<Boolean>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updated.complete(intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false))
            }
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        try {
            val started = wifiManager.startScan()
            val fresh = started && withTimeoutOrNull(SCAN_TIMEOUT_MS) { updated.await() } == true
            return ScanOutcome(cachedResults(), fresh, System.currentTimeMillis())
        } finally {
            appContext.unregisterReceiver(receiver)
        }
    }

    @Suppress("DEPRECATION")
    private fun ScanResult.toAccessPoint(): WifiAccessPoint {
        val caps = capabilities ?: ""
        return WifiAccessPoint(
            ssid = SSID?.takeIf { it.isNotBlank() },
            bssid = BSSID,
            rssi = level,
            frequencyMhz = frequency,
            channel = WifiMath.channelOf(frequency),
            band = WifiMath.bandOf(frequency),
            capabilities = caps,
            security = WifiMath.parseSecurity(caps)
        )
    }

    private companion object {
        const val SCAN_TIMEOUT_MS = 12_000L
    }
}
