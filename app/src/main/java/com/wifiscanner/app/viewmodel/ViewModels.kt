package com.wifiscanner.app.viewmodel

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wifiscanner.app.AppContainer
import com.wifiscanner.app.WifiScannerApp
import com.wifiscanner.app.data.SaveResult
import com.wifiscanner.app.data.SavedNetworkEntity
import com.wifiscanner.app.data.ScanHistoryEntity
import com.wifiscanner.app.data.SecurityCheckEntity
import com.wifiscanner.app.data.SpeedResultEntity
import com.wifiscanner.app.domain.DeviceInfo
import com.wifiscanner.app.domain.Finding
import com.wifiscanner.app.domain.IpUtils
import com.wifiscanner.app.domain.LocalNetworkInfo
import com.wifiscanner.app.domain.SecurityType
import com.wifiscanner.app.domain.SpeedPhase
import com.wifiscanner.app.domain.WifiAccessPoint
import com.wifiscanner.app.domain.WifiBand
import com.wifiscanner.app.network.ConnectState
import com.wifiscanner.app.network.DeviceScanEvent
import com.wifiscanner.app.network.DeviceScanner
import com.wifiscanner.app.network.NetworkSnapshot
import com.wifiscanner.app.network.SpeedErrorKind
import com.wifiscanner.app.network.SpeedTestException
import com.wifiscanner.app.network.SpeedUpdate
import com.wifiscanner.app.security.ReportBuilder
import com.wifiscanner.app.security.SecurityAnalyzer
import com.wifiscanner.app.ui.theme.ThemeMode
import com.wifiscanner.app.utils.PermissionUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private val SHARING = SharingStarted.WhileSubscribed(5_000)

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val vm: ViewModel = when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(container)
            modelClass.isAssignableFrom(ScannerViewModel::class.java) -> ScannerViewModel(container)
            modelClass.isAssignableFrom(SignalViewModel::class.java) -> SignalViewModel(container)
            modelClass.isAssignableFrom(DevicesViewModel::class.java) -> DevicesViewModel(container)
            modelClass.isAssignableFrom(SpeedViewModel::class.java) -> SpeedViewModel(container)
            modelClass.isAssignableFrom(SecurityViewModel::class.java) -> SecurityViewModel(container)
            modelClass.isAssignableFrom(SavedNetworksViewModel::class.java) -> SavedNetworksViewModel(container)
            modelClass.isAssignableFrom(QuickConnectViewModel::class.java) -> QuickConnectViewModel(container)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container)
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
        return vm as T
    }
}

@Composable
fun appViewModelFactory(): ViewModelProvider.Factory {
    val app = LocalContext.current.applicationContext as WifiScannerApp
    return AppViewModelFactory(app.container)
}

// ---------------------------------------------------------------- Home

data class HomeLive(
    val snapshot: NetworkSnapshot? = null,
    val top: List<WifiAccessPoint> = emptyList(),
    val locationOn: Boolean = true
)

data class HomeUiState(
    val live: HomeLive = HomeLive(),
    val lastScan: ScanHistoryEntity? = null,
    val lastSpeed: SpeedResultEntity? = null,
    val lastSecurity: SecurityCheckEntity? = null
)

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    private val live = MutableStateFlow(HomeLive())

    val state: StateFlow<HomeUiState> = combine(
        live,
        c.scanHistory.recent(1),
        c.speedResults.latest(),
        c.securityChecks.latest()
    ) { l, scans, speed, sec ->
        HomeUiState(l, scans.firstOrNull(), speed, sec)
    }.stateIn(viewModelScope, SHARING, HomeUiState())

    fun refresh() {
        viewModelScope.launch {
            val value = withContext(Dispatchers.IO) {
                HomeLive(
                    snapshot = c.networkInfo.snapshot(),
                    top = c.wifiScanner.cachedResults().sortedByDescending { it.rssi }.take(3),
                    locationOn = PermissionUtils.isLocationServiceEnabled(c.appContext)
                )
            }
            live.value = value
        }
    }
}

// ---------------------------------------------------------------- Scanner

enum class BandFilter { ALL, GHZ_2_4, GHZ_5, GHZ_6 }
enum class ScanNotice { NONE, THROTTLED, PERMISSION, WIFI_OFF, LOCATION_OFF }

data class ScannerUiState(
    val loading: Boolean = false,
    val all: List<WifiAccessPoint> = emptyList(),
    val lastScanMs: Long? = null,
    val query: String = "",
    val band: BandFilter = BandFilter.ALL,
    val notice: ScanNotice = ScanNotice.NONE
) {
    val visible: List<WifiAccessPoint>
        get() = all
            .filter {
                when (band) {
                    BandFilter.ALL -> true
                    BandFilter.GHZ_2_4 -> it.band == WifiBand.GHZ_2_4
                    BandFilter.GHZ_5 -> it.band == WifiBand.GHZ_5
                    BandFilter.GHZ_6 -> it.band == WifiBand.GHZ_6
                }
            }
            .filter {
                val q = query.trim()
                q.isEmpty() || (it.ssid?.contains(q, ignoreCase = true) == true) ||
                    (it.bssid?.contains(q, ignoreCase = true) == true)
            }
            .sortedByDescending { it.rssi }
}

class ScannerViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()
    val autoRefreshSeconds: StateFlow<Int> = c.settings.autoRefreshSeconds.stateIn(viewModelScope, SHARING, 0)

    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun setBand(b: BandFilter) = _state.update { it.copy(band = b) }

    fun loadCached() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { c.wifiScanner.cachedResults() }
            _state.update { it.copy(all = cached) }
        }
    }

    fun scan() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, notice = ScanNotice.NONE) }
            try {
                if (!c.networkInfo.isWifiEnabled()) {
                    _state.update { it.copy(notice = ScanNotice.WIFI_OFF) }
                    return@launch
                }
                val outcome = c.wifiScanner.scan()
                val notice = when {
                    outcome.results.isEmpty() && !PermissionUtils.isLocationServiceEnabled(c.appContext) -> ScanNotice.LOCATION_OFF
                    !outcome.fresh -> ScanNotice.THROTTLED
                    else -> ScanNotice.NONE
                }
                _state.update { it.copy(all = outcome.results, lastScanMs = outcome.timestampMs, notice = notice) }
                if (outcome.fresh && outcome.results.isNotEmpty()) {
                    c.scanHistory.record(outcome.results, outcome.timestampMs)
                }
            } catch (e: SecurityException) {
                _state.update { it.copy(notice = ScanNotice.PERMISSION) }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }
}

// ---------------------------------------------------------------- Signal analyzer

data class SignalUiState(val snapshot: NetworkSnapshot? = null, val history: List<Int> = emptyList())

class SignalViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(SignalUiState())
    val state: StateFlow<SignalUiState> = _state.asStateFlow()

    fun sample() {
        viewModelScope.launch {
            val snap = withContext(Dispatchers.IO) { c.networkInfo.snapshot() }
            _state.update { old ->
                val rssi = snap.connection.rssi
                val history = if (rssi != null) (old.history + rssi).takeLast(MAX_SAMPLES) else old.history
                SignalUiState(snap, history)
            }
        }
    }

    fun clearHistory() = _state.update { it.copy(history = emptyList()) }

    private companion object {
        const val MAX_SAMPLES = 60
    }
}

// ---------------------------------------------------------------- Network devices

enum class DevicePhase { IDLE, RUNNING, DONE, CANCELLED, TIMED_OUT }
enum class DeviceProblem { NO_WIFI, NOT_PRIVATE }

data class DevicesUiState(
    val phase: DevicePhase = DevicePhase.IDLE,
    val devices: List<DeviceInfo> = emptyList(),
    val done: Int = 0,
    val total: Int = 0,
    val problem: DeviceProblem? = null,
    val local: LocalNetworkInfo = LocalNetworkInfo(),
    /** null until the scan has finished; false when Android hides the ARP table (typical on Android 10+). */
    val macsAvailable: Boolean? = null
)

class DevicesViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(DevicesUiState())
    val state: StateFlow<DevicesUiState> = _state.asStateFlow()
    private var job: Job? = null

    fun checkNetwork() {
        if (_state.value.phase == DevicePhase.RUNNING) return
        viewModelScope.launch {
            val snap = withContext(Dispatchers.IO) { c.networkInfo.snapshot() }
            val ip = snap.local.ipv4
            val problem = when {
                !snap.local.present || ip == null -> DeviceProblem.NO_WIFI
                !IpUtils.isPrivate(ip) -> DeviceProblem.NOT_PRIVATE
                else -> null
            }
            _state.update { it.copy(local = snap.local, problem = problem) }
        }
    }

    fun start() {
        val st = _state.value
        if (st.phase == DevicePhase.RUNNING || st.problem != null) return
        _state.update { it.copy(phase = DevicePhase.RUNNING, devices = emptyList(), done = 0, total = 0, macsAvailable = null) }
        job = viewModelScope.launch {
            val finished = withTimeoutOrNull(DeviceScanner.OVERALL_TIMEOUT_MS) {
                c.deviceScanner.scan(st.local).collect { event -> handle(event) }
                true
            }
            _state.update { it.copy(phase = if (finished == null) DevicePhase.TIMED_OUT else DevicePhase.DONE) }
        }
    }

    fun cancel() {
        job?.cancel()
        _state.update { if (it.phase == DevicePhase.RUNNING) it.copy(phase = DevicePhase.CANCELLED) else it }
    }

    private fun handle(event: DeviceScanEvent) {
        when (event) {
            is DeviceScanEvent.Found -> _state.update { s ->
                if (s.devices.any { it.ip == event.device.ip }) s
                else s.copy(devices = (s.devices + event.device).sortedBy { IpUtils.toLong(it.ip) ?: Long.MAX_VALUE })
            }
            is DeviceScanEvent.Progress -> _state.update { it.copy(done = event.done, total = event.total) }
            is DeviceScanEvent.MacTable -> _state.update { s ->
                s.copy(
                    devices = s.devices.map { d -> d.copy(mac = event.table[d.ip] ?: d.mac) },
                    macsAvailable = event.table.isNotEmpty()
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Speed test

data class SpeedError(val kind: SpeedErrorKind, val detail: String?)

data class SpeedUiState(
    val running: Boolean = false,
    val update: SpeedUpdate = SpeedUpdate(SpeedPhase.IDLE),
    val error: SpeedError? = null,
    val server: String = ""
)

class SpeedViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(SpeedUiState())
    val state: StateFlow<SpeedUiState> = _state.asStateFlow()
    val history: StateFlow<List<SpeedResultEntity>> = c.speedResults.recent(20).stateIn(viewModelScope, SHARING, emptyList())
    val configuredServer: StateFlow<String> =
        c.settings.speedServer.stateIn(viewModelScope, SHARING, com.wifiscanner.app.data.SettingsStore.DEFAULT_SPEED_SERVER)
    private var job: Job? = null

    fun start() {
        if (_state.value.running) return
        job = viewModelScope.launch {
            val server = c.settings.speedServer.first()
            _state.value = SpeedUiState(running = true, update = SpeedUpdate(SpeedPhase.LATENCY), server = server)
            try {
                val result = c.speedTester.run(server) { upd -> _state.update { it.copy(update = upd) } }
                c.speedResults.save(result)
                _state.update { it.copy(running = false) }
            } catch (e: CancellationException) {
                _state.update { it.copy(running = false, update = SpeedUpdate(SpeedPhase.IDLE)) }
                throw e
            } catch (e: SpeedTestException) {
                _state.update { it.copy(running = false, update = SpeedUpdate(SpeedPhase.ERROR), error = SpeedError(e.kind, e.detail)) }
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }
}

// ---------------------------------------------------------------- Security checker + authorized testing

data class SecurityUiState(
    val running: Boolean = false,
    val findings: List<Finding> = emptyList(),
    val checkedAt: Long? = null,
    val consent: Boolean = false,
    val diagRunning: Boolean = false,
    val diagnostics: List<Finding> = emptyList(),
    val diagAt: Long? = null
)

class SecurityViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(SecurityUiState())
    val state: StateFlow<SecurityUiState> = _state.asStateFlow()
    val checklist: StateFlow<Set<String>> = c.settings.checklist.stateIn(viewModelScope, SHARING, emptySet())
    val lastSaved: StateFlow<SecurityCheckEntity?> = c.securityChecks.latest().stateIn(viewModelScope, SHARING, null)

    fun runCheck() {
        if (_state.value.running) return
        viewModelScope.launch {
            _state.update { it.copy(running = true) }
            try {
                val (snap, scan) = withContext(Dispatchers.IO) { c.networkInfo.snapshot() to c.wifiScanner.cachedResults() }
                val findings = SecurityAnalyzer.analyze(snap.transport, snap.hasPermission, snap.connection, snap.local, scan)
                val now = System.currentTimeMillis()
                _state.update { it.copy(findings = findings, checkedAt = now) }
                c.securityChecks.record(findings, now)
            } finally {
                _state.update { it.copy(running = false) }
            }
        }
    }

    fun setConsent(value: Boolean) = _state.update { it.copy(consent = value) }

    fun runDiagnostics() {
        val st = _state.value
        if (!st.consent || st.diagRunning) return
        viewModelScope.launch {
            _state.update { it.copy(diagRunning = true, diagnostics = emptyList()) }
            try {
                val snap = withContext(Dispatchers.IO) { c.networkInfo.snapshot() }
                val results = c.diagnostics.gateway(snap.local.gateway) + c.diagnostics.dns() + c.diagnostics.internet()
                _state.update { it.copy(diagnostics = results, diagAt = System.currentTimeMillis()) }
            } finally {
                _state.update { it.copy(diagRunning = false) }
            }
        }
    }

    fun toggleChecklist(id: String, checked: Boolean) {
        viewModelScope.launch { c.settings.setChecklistItem(id, checked) }
    }

    fun buildReport(context: Context): String {
        val s = _state.value
        return ReportBuilder.build(context, System.currentTimeMillis(), s.findings, s.diagnostics, checklist.value)
    }
}

// ---------------------------------------------------------------- Saved networks

class SavedNetworksViewModel(private val c: AppContainer) : ViewModel() {
    private val query = MutableStateFlow("")
    val queryText: StateFlow<String> = query.asStateFlow()
    val total: StateFlow<Int> = combine(c.savedNetworks.all, query) { list, _ -> list.size }.stateIn(viewModelScope, SHARING, 0)
    val networks: StateFlow<List<SavedNetworkEntity>> = combine(c.savedNetworks.all, query) { list, q ->
        val needle = q.trim()
        if (needle.isEmpty()) list else list.filter { it.ssid.contains(needle, ignoreCase = true) }
    }.stateIn(viewModelScope, SHARING, emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    suspend fun add(ssid: String, password: String?): SaveResult = c.savedNetworks.add(ssid, password)

    suspend fun update(entity: SavedNetworkEntity, ssid: String, password: String?, remove: Boolean): SaveResult =
        c.savedNetworks.update(entity, ssid, password, remove)

    suspend fun reveal(entity: SavedNetworkEntity): String? = c.savedNetworks.reveal(entity)

    fun delete(entity: SavedNetworkEntity) {
        viewModelScope.launch { c.savedNetworks.delete(entity) }
    }
}

// ---------------------------------------------------------------- Quick connect

class QuickConnectViewModel(private val c: AppContainer) : ViewModel() {
    private val _networks = MutableStateFlow<List<WifiAccessPoint>>(emptyList())
    val networks: StateFlow<List<WifiAccessPoint>> = _networks.asStateFlow()
    val connectState: StateFlow<ConnectState> = c.quickConnector.state

    fun loadNetworks() {
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) { c.wifiScanner.cachedResults() }
            _networks.value = results
                .filter { it.ssid != null }
                .groupBy { it.ssid }
                .map { (_, group) -> group.maxByOrNull { it.rssi }!! }
                .sortedByDescending { it.rssi }
        }
    }

    fun connect(ssid: String, password: String?, security: SecurityType, savePassword: Boolean) {
        c.quickConnector.connect(ssid, password, security)
        if (savePassword && !password.isNullOrEmpty()) {
            viewModelScope.launch { c.savedNetworks.upsertPassword(ssid, password) }
        }
    }

    fun cancel() = c.quickConnector.release()
}

// ---------------------------------------------------------------- Settings

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val theme: StateFlow<ThemeMode> = c.settings.themeMode.stateIn(viewModelScope, SHARING, ThemeMode.DARK)
    val autoRefresh: StateFlow<Int> = c.settings.autoRefreshSeconds.stateIn(viewModelScope, SHARING, 0)
    val speedServer: StateFlow<String> =
        c.settings.speedServer.stateIn(viewModelScope, SHARING, com.wifiscanner.app.data.SettingsStore.DEFAULT_SPEED_SERVER)

    fun setTheme(mode: ThemeMode) = launch { c.settings.setTheme(mode) }
    fun setAutoRefresh(seconds: Int) = launch { c.settings.setAutoRefresh(seconds) }
    fun setSpeedServer(url: String) = launch { c.settings.setSpeedServer(url) }
    fun resetSpeedServer() = launch { c.settings.resetSpeedServer() }
    fun clearScanHistory() = launch { c.scanHistory.clear() }
    fun clearSpeedResults() = launch { c.speedResults.clear() }
    fun clearSecurityResults() = launch { c.securityChecks.clear() }
    fun deleteAllCredentials() = launch { c.savedNetworks.deleteAll() }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
