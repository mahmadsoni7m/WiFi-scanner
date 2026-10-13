package com.wifiscanner.app.domain

/** Pure helpers (no Android dependencies) so they can be unit tested. */
object WifiMath {

    fun classify(rssi: Int): SignalQuality = when {
        rssi >= -50 -> SignalQuality.EXCELLENT
        rssi >= -60 -> SignalQuality.GOOD
        rssi >= -70 -> SignalQuality.FAIR
        rssi >= -80 -> SignalQuality.WEAK
        else -> SignalQuality.POOR
    }

    /** Rough 0..100 percentage: -100 dBm = 0, -50 dBm = 100. */
    fun percent(rssi: Int): Int = ((rssi + 100) * 2).coerceIn(0, 100)

    fun bandOf(frequencyMhz: Int): WifiBand = when (frequencyMhz) {
        in 2400..2500 -> WifiBand.GHZ_2_4
        in 5150..5895 -> WifiBand.GHZ_5
        in 5925..7125 -> WifiBand.GHZ_6
        else -> WifiBand.UNKNOWN
    }

    fun channelOf(frequencyMhz: Int): Int? = when (frequencyMhz) {
        2484 -> 14
        in 2412..2472 -> (frequencyMhz - 2407) / 5
        in 5150..5895 -> (frequencyMhz - 5000) / 5
        in 5955..7115 -> (frequencyMhz - 5950) / 5
        else -> null
    }

    fun parseSecurity(capabilities: String): SecurityType {
        val c = capabilities.uppercase()
        return when {
            "SAE" in c && "PSK" in c -> SecurityType.WPA2_WPA3
            "SAE" in c -> SecurityType.WPA3
            "OWE" in c -> SecurityType.OWE
            "EAP" in c -> SecurityType.ENTERPRISE
            "WPA2" in c || "RSN" in c -> SecurityType.WPA2
            "WPA" in c -> SecurityType.WPA
            "WEP" in c -> SecurityType.WEP
            else -> SecurityType.OPEN
        }
    }
}

object Validators {
    fun isValidSsid(ssid: String): Boolean =
        ssid.isNotBlank() && ssid.toByteArray(Charsets.UTF_8).size <= 32

    /** WPA/WPA2/WPA3 personal passphrase: 8..63 printable characters. */
    fun isValidPassphrase(p: String): Boolean = p.length in 8..63

    fun isValidHttpsUrl(url: String): Boolean {
        val u = url.trim()
        if (!u.startsWith("https://") || u.length <= 8) return false
        val host = u.removePrefix("https://").substringBefore('/')
        return host.isNotBlank() && !host.contains(' ')
    }
}

object IpUtils {
    fun toLong(ip: String): Long? {
        val parts = ip.split('.')
        if (parts.size != 4) return null
        var v = 0L
        for (p in parts) {
            val n = p.toIntOrNull() ?: return null
            if (n !in 0..255) return null
            v = (v shl 8) or n.toLong()
        }
        return v
    }

    fun fromLong(v: Long): String =
        "${(v shr 24) and 255}.${(v shr 16) and 255}.${(v shr 8) and 255}.${v and 255}"

    /** Host addresses of the local subnet (limited to /24 .. /30 to avoid huge sweeps). */
    fun hostRange(ip: String, prefixLength: Int): List<String> {
        val v = toLong(ip) ?: return emptyList()
        val prefix = prefixLength.coerceIn(24, 30)
        val mask = (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        val network = v and mask
        val broadcast = network or (mask.inv() and 0xFFFFFFFFL)
        return ((network + 1) until broadcast).map { fromLong(it) }
    }

    fun isPrivate(ip: String): Boolean {
        val v = toLong(ip) ?: return false
        val a = (v shr 24) and 255
        val b = (v shr 16) and 255
        return a == 10L || (a == 172L && b in 16..31) || (a == 192L && b == 168L)
    }
}
