package com.wifiscanner.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import com.wifiscanner.app.domain.SecurityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ConnectFailure { UNAVAILABLE, PASSWORD_REQUIRED, ERROR }

sealed interface ConnectState {
    data object Idle : ConnectState
    data class Requesting(val ssid: String) : ConnectState
    /** Reported only after Android confirms the network is available. */
    data class Connected(val ssid: String) : ConnectState
    /** This network type / Android version cannot be joined with the in-app request API. */
    data class UseSystemSettings(val ssid: String) : ConnectState
    data class Failed(val reason: ConnectFailure) : ConnectState
}

/**
 * Connects through Android's supported WifiNetworkSpecifier request (Android 10+). The system shows its
 * own confirmation dialog. The connection is tied to this app (it is not a saved system network).
 * No WPA/WPA2/WPA3 bypass or captive-portal bypass is attempted.
 */
class QuickConnector(context: Context) {
    private val connectivity = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _state = MutableStateFlow<ConnectState>(ConnectState.Idle)
    val state: StateFlow<ConnectState> = _state
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun connect(ssid: String, password: String?, security: SecurityType) {
        release()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            _state.value = ConnectState.UseSystemSettings(ssid)
            return
        }
        val builder = WifiNetworkSpecifier.Builder().setSsid(ssid)
        when (security) {
            SecurityType.OPEN -> Unit
            SecurityType.WPA2, SecurityType.WPA2_WPA3 -> {
                if (password.isNullOrEmpty()) return fail(ConnectFailure.PASSWORD_REQUIRED)
                builder.setWpa2Passphrase(password)
            }
            SecurityType.WPA3 -> {
                if (password.isNullOrEmpty()) return fail(ConnectFailure.PASSWORD_REQUIRED)
                builder.setWpa3Passphrase(password)
            }
            else -> {
                _state.value = ConnectState.UseSystemSettings(ssid)
                return
            }
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(builder.build())
            .build()

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _state.value = ConnectState.Connected(ssid)
            }

            override fun onUnavailable() {
                _state.value = ConnectState.Failed(ConnectFailure.UNAVAILABLE)
            }

            override fun onLost(network: Network) {
                _state.value = ConnectState.Idle
            }
        }
        callback = cb
        _state.value = ConnectState.Requesting(ssid)
        try {
            connectivity.requestNetwork(request, cb, REQUEST_TIMEOUT_MS)
        } catch (e: RuntimeException) {
            callback = null
            fail(ConnectFailure.ERROR)
        }
    }

    /** Cancels a pending request or releases an established app-level connection. */
    fun release() {
        callback?.let {
            try {
                connectivity.unregisterNetworkCallback(it)
            } catch (e: IllegalArgumentException) {
                // already unregistered
            }
        }
        callback = null
        _state.value = ConnectState.Idle
    }

    private fun fail(reason: ConnectFailure) {
        _state.value = ConnectState.Failed(reason)
    }

    private companion object {
        const val REQUEST_TIMEOUT_MS = 60_000
    }
}
