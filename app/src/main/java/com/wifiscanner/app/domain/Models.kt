package com.wifiscanner.app.domain

import androidx.annotation.StringRes

enum class SignalQuality { EXCELLENT, GOOD, FAIR, WEAK, POOR }

enum class WifiBand { GHZ_2_4, GHZ_5, GHZ_6, UNKNOWN }

enum class SecurityType { OPEN, WEP, WPA, WPA2, WPA3, WPA2_WPA3, OWE, ENTERPRISE }

enum class Transport { WIFI, CELLULAR, ETHERNET, OTHER, NONE }

data class WifiAccessPoint(
    val ssid: String?,
    val bssid: String?,
    val rssi: Int,
    val frequencyMhz: Int,
    val channel: Int?,
    val band: WifiBand,
    val capabilities: String,
    val security: SecurityType
)

/** Current Wi-Fi link. Any field Android does not expose is null. */
data class CurrentConnection(
    val connected: Boolean = false,
    val ssid: String? = null,
    val bssid: String? = null,
    val rssi: Int? = null,
    val frequencyMhz: Int? = null,
    val linkSpeedMbps: Int? = null
)

data class LocalNetworkInfo(
    val present: Boolean = false,
    val ipv4: String? = null,
    val prefixLength: Int? = null,
    val gateway: String? = null,
    val dns: List<String> = emptyList()
)

data class DeviceInfo(
    val ip: String,
    val mac: String? = null,
    val hostname: String? = null,
    val isSelf: Boolean = false,
    val isGateway: Boolean = false
)

enum class SpeedPhase { IDLE, LATENCY, DOWNLOAD, UPLOAD, DONE, ERROR }

data class SpeedResult(
    val timestamp: Long,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val latencyMs: Double,
    val jitterMs: Double,
    val server: String
)

enum class FindingKind { VERIFIED, WARNING, RECOMMENDATION, UNAVAILABLE }

/** A security/diagnostic finding. Text is resolved from string resources at display time. */
data class Finding(
    val kind: FindingKind,
    @StringRes val titleRes: Int,
    @StringRes val detailRes: Int? = null,
    val args: List<String> = emptyList()
)
