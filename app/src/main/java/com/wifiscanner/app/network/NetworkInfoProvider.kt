package com.wifiscanner.app.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.wifiscanner.app.domain.CurrentConnection
import com.wifiscanner.app.domain.LocalNetworkInfo
import com.wifiscanner.app.domain.Transport
import com.wifiscanner.app.utils.PermissionUtils
import java.net.Inet4Address

/** Everything the app can learn about the current connection. Missing data stays null/empty. */
data class NetworkSnapshot(
    val wifiEnabled: Boolean = false,
    val hasPermission: Boolean = false,
    val transport: Transport = Transport.NONE,
    val internetValidated: Boolean = false,
    val captivePortal: Boolean = false,
    val connection: CurrentConnection = CurrentConnection(),
    val local: LocalNetworkInfo = LocalNetworkInfo()
)

class NetworkInfoProvider(context: Context) {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivity = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled

    @SuppressLint("MissingPermission")
    fun snapshot(): NetworkSnapshot {
        val enabled = wifiManager.isWifiEnabled
        val hasPermission = PermissionUtils.hasWifiPermissions(appContext)
        val active = connectivity.activeNetwork
        val caps = active?.let { connectivity.getNetworkCapabilities(it) }
        val transport = when {
            caps == null -> Transport.NONE
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Transport.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Transport.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Transport.ETHERNET
            else -> Transport.OTHER
        }
        val wifiNetwork = findWifiNetwork(active, caps)
        val connection = if (enabled && wifiNetwork != null) readConnection(hasPermission) else CurrentConnection()
        return NetworkSnapshot(
            wifiEnabled = enabled,
            hasPermission = hasPermission,
            transport = transport,
            internetValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            captivePortal = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true,
            connection = connection,
            local = wifiNetwork?.let { readLocal(it) } ?: LocalNetworkInfo()
        )
    }

    /** The Wi-Fi network, even when the active (default) network is currently cellular. */
    @Suppress("DEPRECATION")
    private fun findWifiNetwork(active: Network?, activeCaps: NetworkCapabilities?): Network? {
        if (active != null && activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) return active
        return connectivity.allNetworks.firstOrNull {
            connectivity.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun readConnection(hasPermission: Boolean): CurrentConnection {
        val info = try {
            wifiManager.connectionInfo
        } catch (e: SecurityException) {
            null
        } ?: return CurrentConnection(connected = true)

        val ssid = if (hasPermission) {
            info.ssid?.trim('"')?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
        } else null
        val bssid = if (hasPermission) info.bssid?.takeIf { it != "02:00:00:00:00:00" } else null
        return CurrentConnection(
            connected = true,
            ssid = ssid,
            bssid = bssid,
            rssi = info.rssi.takeIf { it in -126..-1 },
            frequencyMhz = info.frequency.takeIf { it > 0 },
            linkSpeedMbps = info.linkSpeed.takeIf { it > 0 }
        )
    }

    private fun readLocal(network: Network): LocalNetworkInfo {
        val lp = connectivity.getLinkProperties(network) ?: return LocalNetworkInfo()
        val addr = lp.linkAddresses.firstOrNull { it.address is Inet4Address }
        val gateway = lp.routes
            .firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
            ?.gateway?.hostAddress
        return LocalNetworkInfo(
            present = true,
            ipv4 = addr?.address?.hostAddress,
            prefixLength = addr?.prefixLength,
            gateway = gateway,
            dns = lp.dnsServers.mapNotNull { it.hostAddress }
        )
    }
}
