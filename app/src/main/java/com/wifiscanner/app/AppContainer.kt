package com.wifiscanner.app

import android.content.Context
import com.wifiscanner.app.data.AppDatabase
import com.wifiscanner.app.data.SavedNetworkRepository
import com.wifiscanner.app.data.ScanHistoryRepository
import com.wifiscanner.app.data.SecurityCheckRepository
import com.wifiscanner.app.data.SettingsStore
import com.wifiscanner.app.data.SpeedResultRepository
import com.wifiscanner.app.network.DeviceScanner
import com.wifiscanner.app.network.NetworkDiagnostics
import com.wifiscanner.app.network.NetworkInfoProvider
import com.wifiscanner.app.network.QuickConnector
import com.wifiscanner.app.network.SpeedTester
import com.wifiscanner.app.network.WifiScanner

/** Manual dependency container: one instance per process, created in [WifiScannerApp]. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val settings = SettingsStore(appContext)
    val networkInfo = NetworkInfoProvider(appContext)
    val wifiScanner = WifiScanner(appContext)
    val deviceScanner = DeviceScanner()
    val speedTester = SpeedTester()
    val diagnostics = NetworkDiagnostics()
    val quickConnector = QuickConnector(appContext)

    val scanHistory by lazy { ScanHistoryRepository(database.scanHistoryDao()) }
    val speedResults by lazy { SpeedResultRepository(database.speedResultDao()) }
    val securityChecks by lazy { SecurityCheckRepository(database.securityCheckDao()) }
    val savedNetworks by lazy { SavedNetworkRepository(database.savedNetworkDao()) }
}
