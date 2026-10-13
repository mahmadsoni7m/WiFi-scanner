package com.wifiscanner.app.security

import com.wifiscanner.app.R
import com.wifiscanner.app.domain.CurrentConnection
import com.wifiscanner.app.domain.Finding
import com.wifiscanner.app.domain.FindingKind
import com.wifiscanner.app.domain.LocalNetworkInfo
import com.wifiscanner.app.domain.SecurityType
import com.wifiscanner.app.domain.Transport
import com.wifiscanner.app.domain.WifiAccessPoint

/**
 * Builds a list of findings from what Android genuinely exposes. VERIFIED = observed fact,
 * WARNING = observed weakness, UNAVAILABLE = Android did not provide the data,
 * RECOMMENDATION = general advice that the app cannot verify.
 */
object SecurityAnalyzer {

    fun analyze(
        transport: Transport,
        hasPermission: Boolean,
        conn: CurrentConnection,
        local: LocalNetworkInfo,
        scan: List<WifiAccessPoint>
    ): List<Finding> {
        val out = mutableListOf<Finding>()

        out += when (transport) {
            Transport.WIFI -> Finding(FindingKind.VERIFIED, R.string.sec_transport_wifi)
            Transport.CELLULAR -> Finding(FindingKind.VERIFIED, R.string.sec_transport_cellular)
            Transport.ETHERNET, Transport.OTHER -> Finding(FindingKind.VERIFIED, R.string.sec_transport_other)
            Transport.NONE -> Finding(FindingKind.WARNING, R.string.sec_transport_none)
        }

        out += if (hasPermission) Finding(FindingKind.VERIFIED, R.string.sec_perm_ok)
        else Finding(FindingKind.WARNING, R.string.sec_perm_missing, R.string.sec_perm_missing_detail)

        if (conn.connected) {
            val ap = findCurrentAp(conn, scan)
            if (ap == null) {
                out += Finding(FindingKind.UNAVAILABLE, R.string.sec_cap_unavailable, R.string.sec_cap_unavailable_detail)
            } else {
                out += when (ap.security) {
                    SecurityType.OPEN -> Finding(FindingKind.WARNING, R.string.sec_open, R.string.sec_open_detail)
                    SecurityType.WEP -> Finding(FindingKind.WARNING, R.string.sec_wep, R.string.sec_legacy_detail)
                    SecurityType.WPA -> Finding(FindingKind.WARNING, R.string.sec_wpa_legacy, R.string.sec_legacy_detail)
                    SecurityType.WPA2 -> Finding(FindingKind.VERIFIED, R.string.sec_wpa2, R.string.sec_caps_detail, listOf(ap.capabilities))
                    SecurityType.WPA3, SecurityType.WPA2_WPA3 ->
                        Finding(FindingKind.VERIFIED, R.string.sec_wpa3, R.string.sec_caps_detail, listOf(ap.capabilities))
                    SecurityType.OWE -> Finding(FindingKind.VERIFIED, R.string.sec_owe, R.string.sec_caps_detail, listOf(ap.capabilities))
                    SecurityType.ENTERPRISE ->
                        Finding(FindingKind.VERIFIED, R.string.sec_enterprise, R.string.sec_caps_detail, listOf(ap.capabilities))
                }
                if (ap.security == SecurityType.WPA2 && ap.capabilities.uppercase().contains("TKIP")) {
                    out += Finding(FindingKind.WARNING, R.string.sec_tkip, R.string.sec_tkip_detail)
                }
            }
        }

        if (local.present) {
            val gw = local.gateway
            out += if (gw != null) Finding(FindingKind.VERIFIED, R.string.sec_gateway, R.string.sec_value_detail, listOf(gw))
            else Finding(FindingKind.UNAVAILABLE, R.string.sec_gateway_unavailable)

            local.ipv4?.let { ip ->
                out += Finding(
                    FindingKind.VERIFIED, R.string.sec_local_ip, R.string.sec_value_detail,
                    listOf("$ip/${local.prefixLength ?: "?"}")
                )
            }
            if (local.dns.isNotEmpty()) {
                out += Finding(FindingKind.VERIFIED, R.string.sec_dns, R.string.sec_value_detail, listOf(local.dns.joinToString(", ")))
            } else {
                out += Finding(FindingKind.UNAVAILABLE, R.string.sec_dns_unavailable)
            }
        }

        out += listOf(
            R.string.rec_wpa3,
            R.string.rec_password,
            R.string.rec_admin,
            R.string.rec_wps,
            R.string.rec_firmware,
            R.string.rec_remote,
            R.string.rec_guest
        ).map { Finding(FindingKind.RECOMMENDATION, it) }

        return out
    }

    private fun findCurrentAp(conn: CurrentConnection, scan: List<WifiAccessPoint>): WifiAccessPoint? {
        conn.bssid?.let { b -> scan.firstOrNull { it.bssid.equals(b, ignoreCase = true) }?.let { return it } }
        conn.ssid?.let { s -> return scan.firstOrNull { it.ssid == s } }
        return null
    }
}
