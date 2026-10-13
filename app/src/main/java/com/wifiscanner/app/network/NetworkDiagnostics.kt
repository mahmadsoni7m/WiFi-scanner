package com.wifiscanner.app.network

import com.wifiscanner.app.R
import com.wifiscanner.app.domain.Finding
import com.wifiscanner.app.domain.FindingKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * User-initiated, read-only diagnostics for a network the user owns or may assess.
 * Only the user's own gateway is contacted locally; DNS and HTTPS checks use well-known public hosts.
 */
class NetworkDiagnostics {

    suspend fun gateway(gateway: String?): List<Finding> = withContext(Dispatchers.IO) {
        if (gateway == null) {
            return@withContext listOf(Finding(FindingKind.UNAVAILABLE, R.string.diag_gateway_title, R.string.diag_gateway_none))
        }
        val address = InetAddress.getByName(gateway)
        val start = System.nanoTime()
        val reachable = try {
            address.isReachable(1500)
        } catch (e: IOException) {
            false
        }
        val openPorts = PORTS.filter { port ->
            try {
                Socket().use { it.connect(InetSocketAddress(address, port), 600) }
                true
            } catch (e: IOException) {
                false
            }
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        val responded = reachable || openPorts.isNotEmpty()

        val out = mutableListOf<Finding>()
        out += if (responded) {
            Finding(FindingKind.VERIFIED, R.string.diag_gateway_title, R.string.diag_gateway_ok, listOf(gateway, elapsedMs.toString()))
        } else {
            Finding(FindingKind.WARNING, R.string.diag_gateway_title, R.string.diag_gateway_fail, listOf(gateway))
        }
        if (openPorts.isEmpty()) {
            out += Finding(FindingKind.VERIFIED, R.string.diag_ports_title, R.string.diag_ports_none)
        } else if (80 in openPorts && 443 !in openPorts) {
            out += Finding(FindingKind.WARNING, R.string.diag_ports_title, R.string.diag_ports_http_only)
        } else {
            out += Finding(FindingKind.VERIFIED, R.string.diag_ports_title, R.string.diag_ports_detail, listOf(openPorts.joinToString(", ")))
        }
        out
    }

    suspend fun dns(): Finding = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        try {
            val addresses = InetAddress.getAllByName(DNS_TEST_HOST)
            val ms = (System.nanoTime() - start) / 1_000_000
            Finding(
                FindingKind.VERIFIED, R.string.diag_dns_title, R.string.diag_dns_ok,
                listOf(DNS_TEST_HOST, addresses.size.toString(), ms.toString())
            )
        } catch (e: IOException) {
            Finding(FindingKind.WARNING, R.string.diag_dns_title, R.string.diag_dns_fail, listOf(DNS_TEST_HOST))
        }
    }

    suspend fun internet(): Finding = withContext(Dispatchers.IO) {
        val conn = URL(INTERNET_TEST_URL).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.instanceFollowRedirects = false
            val code = conn.responseCode
            if (code == 204) {
                Finding(FindingKind.VERIFIED, R.string.diag_internet_title, R.string.diag_internet_ok, listOf(URL(INTERNET_TEST_URL).host))
            } else {
                Finding(FindingKind.WARNING, R.string.diag_internet_title, R.string.diag_internet_unexpected, listOf(code.toString()))
            }
        } catch (e: IOException) {
            Finding(FindingKind.WARNING, R.string.diag_internet_title, R.string.diag_internet_fail, listOf(URL(INTERNET_TEST_URL).host))
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val DNS_TEST_HOST = "example.com"
        const val INTERNET_TEST_URL = "https://www.gstatic.com/generate_204"
        private val PORTS = listOf(80, 443)
    }
}
