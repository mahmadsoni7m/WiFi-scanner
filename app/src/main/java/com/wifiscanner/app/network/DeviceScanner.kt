package com.wifiscanner.app.network

import com.wifiscanner.app.domain.DeviceInfo
import com.wifiscanner.app.domain.IpUtils
import com.wifiscanner.app.domain.LocalNetworkInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

sealed interface DeviceScanEvent {
    data class Progress(val done: Int, val total: Int) : DeviceScanEvent
    data class Found(val device: DeviceInfo) : DeviceScanEvent
    /** IP -> MAC from the system ARP table. Empty when Android does not allow reading it. */
    data class MacTable(val table: Map<String, String>) : DeviceScanEvent
}

/** Parses /proc/net/arp. Pure function so it can be unit tested. */
object ArpParser {
    private val LINE = Regex("""^(\d{1,3}(?:\.\d{1,3}){3})\s+\S+\s+\S+\s+([0-9a-fA-F:]{17})\s+\S+\s+\S+""")

    fun parse(lines: List<String>): Map<String, String> =
        lines.mapNotNull { line ->
            val m = LINE.find(line.trim()) ?: return@mapNotNull null
            val mac = m.groupValues[2].lowercase()
            if (mac == "00:00:00:00:00:00") null else m.groupValues[1] to mac
        }.toMap()

    fun readSystemTable(): Map<String, String> = try {
        parse(File("/proc/net/arp").readLines())
    } catch (e: Exception) {
        emptyMap()
    }
}

/**
 * Safe, limited LAN discovery: only the device's own private subnet (at most a /24), a short
 * reachability check per address and a handful of TCP connection attempts on common ports.
 * It never touches public ranges. Cancel the collecting coroutine to stop immediately.
 */
class DeviceScanner {
    private val resolverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun scan(local: LocalNetworkInfo): Flow<DeviceScanEvent> = channelFlow {
        val ownIp = local.ipv4 ?: return@channelFlow
        if (!IpUtils.isPrivate(ownIp)) return@channelFlow
        val hosts = IpUtils.hostRange(ownIp, local.prefixLength ?: 24).filter { it != ownIp }

        send(DeviceScanEvent.Found(DeviceInfo(ip = ownIp, isSelf = true, isGateway = ownIp == local.gateway)))

        val gate = Semaphore(MAX_PARALLEL)
        val done = AtomicInteger(0)
        val jobs = hosts.map { host ->
            launch(Dispatchers.IO) {
                gate.withPermit {
                    if (isReachable(host)) {
                        send(
                            DeviceScanEvent.Found(
                                DeviceInfo(ip = host, hostname = resolveHostname(host), isGateway = host == local.gateway)
                            )
                        )
                    }
                }
                send(DeviceScanEvent.Progress(done.incrementAndGet(), hosts.size))
            }
        }
        jobs.joinAll()
        send(DeviceScanEvent.MacTable(ArpParser.readSystemTable()))
    }

    private fun isReachable(host: String): Boolean {
        val address = try {
            InetAddress.getByName(host) // numeric literal: no DNS lookup
        } catch (e: Exception) {
            return false
        }
        try {
            if (address.isReachable(REACH_TIMEOUT_MS)) return true
        } catch (e: IOException) {
            // fall through to TCP probes
        }
        for (port in PROBE_PORTS) {
            try {
                Socket().use { it.connect(InetSocketAddress(address, port), TCP_TIMEOUT_MS) }
                return true
            } catch (e: ConnectException) {
                // "Connection refused" means a live host answered with a reset.
                if (e.message?.contains("refused", ignoreCase = true) == true) return true
            } catch (e: IOException) {
                // timeout / unreachable: keep trying other ports
            }
        }
        return false
    }

    private suspend fun resolveHostname(host: String): String? {
        val lookup = resolverScope.async {
            runCatching { InetAddress.getByName(host).canonicalHostName }.getOrNull()
        }
        return withTimeoutOrNull(HOSTNAME_TIMEOUT_MS) { lookup.await() }?.takeIf { it != host }
    }

    companion object {
        const val OVERALL_TIMEOUT_MS = 45_000L
        private const val MAX_PARALLEL = 32
        private const val REACH_TIMEOUT_MS = 300
        private const val TCP_TIMEOUT_MS = 200
        private const val HOSTNAME_TIMEOUT_MS = 1_000L
        private val PROBE_PORTS = intArrayOf(80, 443, 53, 8080)
    }
}
