package com.wifiscanner.app.network

import com.wifiscanner.app.domain.SpeedPhase
import com.wifiscanner.app.domain.SpeedResult
import com.wifiscanner.app.domain.Validators
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLException
import kotlin.random.Random

enum class SpeedErrorKind { INVALID_URL, UNREACHABLE, TIMEOUT, HTTP, NO_DATA, OTHER }

class SpeedTestException(val kind: SpeedErrorKind, val detail: String? = null, cause: Throwable? = null) :
    Exception(detail, cause)

data class SpeedUpdate(
    val phase: SpeedPhase,
    val progress: Float = 0f,
    val liveMbps: Double? = null,
    val latencyMs: Double? = null,
    val jitterMs: Double? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null
)

/**
 * Speed test against a Cloudflare-compatible endpoint (GET {base}/__down?bytes=N, POST {base}/__up).
 * Only random test data is transferred; no personal information is sent. Results are measured,
 * never simulated; any failure throws [SpeedTestException].
 */
class SpeedTester {

    suspend fun run(baseUrl: String, onUpdate: (SpeedUpdate) -> Unit): SpeedResult {
        val base = baseUrl.trim().trimEnd('/')
        if (!Validators.isValidHttpsUrl(base)) throw SpeedTestException(SpeedErrorKind.INVALID_URL, base)
        try {
            onUpdate(SpeedUpdate(SpeedPhase.LATENCY))
            val (latency, jitter) = measureLatency(base)
            onUpdate(SpeedUpdate(SpeedPhase.DOWNLOAD, latencyMs = latency, jitterMs = jitter))

            val download = measureThroughput(DOWNLOAD_STREAMS, { b, d, t -> downloadWorker(b, d, t) }, base) { live, fraction ->
                onUpdate(SpeedUpdate(SpeedPhase.DOWNLOAD, fraction, live, latency, jitter))
            }
            onUpdate(SpeedUpdate(SpeedPhase.UPLOAD, 0f, null, latency, jitter, download))

            val upload = measureThroughput(UPLOAD_STREAMS, { b, d, t -> uploadWorker(b, d, t) }, base) { live, fraction ->
                onUpdate(SpeedUpdate(SpeedPhase.UPLOAD, fraction, live, latency, jitter, download))
            }
            onUpdate(SpeedUpdate(SpeedPhase.DONE, 1f, null, latency, jitter, download, upload))
            return SpeedResult(
                timestamp = System.currentTimeMillis(),
                downloadMbps = download,
                uploadMbps = upload,
                latencyMs = latency,
                jitterMs = jitter,
                server = hostOf(base)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: SpeedTestException) {
            throw e
        } catch (e: Exception) {
            throw map(e, base)
        }
    }

    private suspend fun measureLatency(base: String): Pair<Double, Double> {
        val samples = ArrayList<Double>()
        repeat(LATENCY_REQUESTS) { index ->
            currentCoroutineContext().ensureActive()
            val ms = withContext(Dispatchers.IO) { timedRequest("$base/__down?bytes=0") }
            if (index > 0) samples += ms // the first request includes the TLS handshake
        }
        if (samples.isEmpty()) throw SpeedTestException(SpeedErrorKind.NO_DATA)
        val average = samples.average()
        val jitter = if (samples.size > 1) {
            samples.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average()
        } else 0.0
        return average to jitter
    }

    private fun timedRequest(url: String): Double {
        val start = System.nanoTime()
        val conn = open(url)
        try {
            val code = conn.responseCode
            if (code != 200) throw SpeedTestException(SpeedErrorKind.HTTP, code.toString())
            conn.inputStream.use { it.readBytes() }
        } catch (e: Exception) {
            conn.disconnect()
            throw e
        }
        return (System.nanoTime() - start) / 1_000_000.0
    }

    private suspend fun measureThroughput(
        streams: Int,
        worker: suspend (String, Long, AtomicLong) -> Unit,
        base: String,
        onProgress: (Double, Float) -> Unit
    ): Double = coroutineScope {
        val total = AtomicLong(0)
        val startNs = System.nanoTime()
        val deadlineNs = startNs + PHASE_DURATION_MS * 1_000_000L
        val workers = List(streams) { async(Dispatchers.IO) { worker(base, deadlineNs, total) } }
        while (isActive && System.nanoTime() < deadlineNs && workers.any { it.isActive }) {
            delay(TICK_MS)
            val elapsed = (System.nanoTime() - startNs) / 1e9
            if (elapsed > 0) {
                onProgress(mbps(total.get(), elapsed), (elapsed * 1000 / PHASE_DURATION_MS).toFloat().coerceIn(0f, 1f))
            }
        }
        val elapsed = (System.nanoTime() - startNs) / 1e9
        workers.forEach { it.cancel() }
        val bytes = total.get()
        if (bytes <= 0L) throw SpeedTestException(SpeedErrorKind.NO_DATA)
        mbps(bytes, elapsed)
    }

    private suspend fun downloadWorker(base: String, deadlineNs: Long, total: AtomicLong) {
        val buffer = ByteArray(64 * 1024)
        while (currentCoroutineContext().isActive && System.nanoTime() < deadlineNs) {
            val conn = open("$base/__down?bytes=$DOWNLOAD_BYTES")
            try {
                val code = conn.responseCode
                if (code != 200) throw SpeedTestException(SpeedErrorKind.HTTP, code.toString())
                conn.inputStream.use { input ->
                    while (currentCoroutineContext().isActive && System.nanoTime() < deadlineNs) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        total.addAndGet(n.toLong())
                    }
                }
            } finally {
                conn.disconnect()
            }
        }
    }

    private suspend fun uploadWorker(base: String, deadlineNs: Long, total: AtomicLong) {
        val payload = Random.Default.nextBytes(UPLOAD_CHUNK_BYTES)
        while (currentCoroutineContext().isActive && System.nanoTime() < deadlineNs) {
            val conn = open("$base/__up")
            try {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setFixedLengthStreamingMode(payload.size)
                conn.outputStream.use { it.write(payload) }
                val code = conn.responseCode
                if (code !in 200..299) throw SpeedTestException(SpeedErrorKind.HTTP, code.toString())
                conn.inputStream.use { it.readBytes() }
                total.addAndGet(payload.size.toLong()) // counted only after the server answered
            } catch (e: Exception) {
                conn.disconnect()
                throw e
            }
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6_000
            readTimeout = 6_000
            useCaches = false
            setRequestProperty("Cache-Control", "no-cache")
        }

    private fun mbps(bytes: Long, seconds: Double): Double = bytes * 8.0 / seconds / 1_000_000.0

    private fun hostOf(base: String): String = runCatching { URI(base).host }.getOrNull() ?: base

    private fun map(e: Exception, base: String): SpeedTestException = when (e) {
        is UnknownHostException, is ConnectException, is SSLException ->
            SpeedTestException(SpeedErrorKind.UNREACHABLE, hostOf(base), e)
        is SocketTimeoutException -> SpeedTestException(SpeedErrorKind.TIMEOUT, hostOf(base), e)
        is IOException -> SpeedTestException(SpeedErrorKind.UNREACHABLE, hostOf(base), e)
        else -> SpeedTestException(SpeedErrorKind.OTHER, e.javaClass.simpleName, e)
    }

    private companion object {
        const val LATENCY_REQUESTS = 9
        const val DOWNLOAD_STREAMS = 4
        const val UPLOAD_STREAMS = 3
        const val PHASE_DURATION_MS = 8_000L
        const val TICK_MS = 250L
        const val DOWNLOAD_BYTES = 25_000_000
        const val UPLOAD_CHUNK_BYTES = 512 * 1024
    }
}
