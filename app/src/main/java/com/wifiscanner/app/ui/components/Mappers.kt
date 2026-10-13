package com.wifiscanner.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.wifiscanner.app.R
import com.wifiscanner.app.domain.SecurityType
import com.wifiscanner.app.domain.SignalQuality
import com.wifiscanner.app.domain.WifiBand
import com.wifiscanner.app.ui.theme.WsColors

fun SignalQuality.color(): Color = when (this) {
    SignalQuality.EXCELLENT -> WsColors.Success
    SignalQuality.GOOD -> Color(0xFF8BE04A)
    SignalQuality.FAIR -> Color(0xFFFFD24D)
    SignalQuality.WEAK -> Color(0xFFFF9A3D)
    SignalQuality.POOR -> WsColors.Danger
}

@StringRes
fun SignalQuality.labelRes(): Int = when (this) {
    SignalQuality.EXCELLENT -> R.string.quality_excellent
    SignalQuality.GOOD -> R.string.quality_good
    SignalQuality.FAIR -> R.string.quality_fair
    SignalQuality.WEAK -> R.string.quality_weak
    SignalQuality.POOR -> R.string.quality_poor
}

@StringRes
fun SignalQuality.guidanceRes(): Int = when (this) {
    SignalQuality.EXCELLENT -> R.string.guidance_excellent
    SignalQuality.GOOD -> R.string.guidance_good
    SignalQuality.FAIR -> R.string.guidance_fair
    SignalQuality.WEAK -> R.string.guidance_weak
    SignalQuality.POOR -> R.string.guidance_poor
}

@StringRes
fun SecurityType.labelRes(): Int = when (this) {
    SecurityType.OPEN -> R.string.sectype_open
    SecurityType.WEP -> R.string.sectype_wep
    SecurityType.WPA -> R.string.sectype_wpa
    SecurityType.WPA2 -> R.string.sectype_wpa2
    SecurityType.WPA3 -> R.string.sectype_wpa3
    SecurityType.WPA2_WPA3 -> R.string.sectype_wpa2_wpa3
    SecurityType.OWE -> R.string.sectype_owe
    SecurityType.ENTERPRISE -> R.string.sectype_enterprise
}

fun SecurityType.isWeak(): Boolean =
    this == SecurityType.OPEN || this == SecurityType.WEP || this == SecurityType.WPA

@StringRes
fun WifiBand.labelRes(): Int = when (this) {
    WifiBand.GHZ_2_4 -> R.string.band_24
    WifiBand.GHZ_5 -> R.string.band_5
    WifiBand.GHZ_6 -> R.string.band_6
    WifiBand.UNKNOWN -> R.string.band_unknown
}
