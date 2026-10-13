package com.wifiscanner.app.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wifiscanner.app.R
import com.wifiscanner.app.ui.screens.DevicesScreen
import com.wifiscanner.app.ui.screens.HomeScreen
import com.wifiscanner.app.ui.screens.QuickConnectScreen
import com.wifiscanner.app.ui.screens.SavedNetworksScreen
import com.wifiscanner.app.ui.screens.ScannerScreen
import com.wifiscanner.app.ui.screens.SecurityScreen
import com.wifiscanner.app.ui.screens.SettingsScreen
import com.wifiscanner.app.ui.screens.SignalScreen
import com.wifiscanner.app.ui.screens.SpeedScreen

object Routes {
    const val HOME = "home"
    const val SCANNER = "scanner"
    const val DEVICES = "devices"
    const val SPEED = "speed"
    const val SECURITY = "security"
    const val SETTINGS = "settings"
    const val SIGNAL = "signal"
    const val SAVED = "saved"
    const val QUICK_CONNECT = "quick_connect"
    const val QUICK_CONNECT_PATTERN = "quick_connect?ssid={ssid}"

    fun quickConnect(ssid: String?): String =
        if (ssid.isNullOrEmpty()) QUICK_CONNECT else "$QUICK_CONNECT?ssid=${Uri.encode(ssid)}"
}

private data class TopLevelItem(val route: String, @StringRes val label: Int, val icon: ImageVector)

private val topLevelItems = listOf(
    TopLevelItem(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    TopLevelItem(Routes.SCANNER, R.string.nav_scanner, Icons.Filled.Wifi),
    TopLevelItem(Routes.DEVICES, R.string.nav_devices, Icons.Filled.Devices),
    TopLevelItem(Routes.SPEED, R.string.nav_speed, Icons.Filled.Speed),
    TopLevelItem(Routes.SECURITY, R.string.nav_security, Icons.Filled.Security),
    TopLevelItem(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings)
)

private fun NavController.navigateTo(route: String) {
    if (topLevelItems.any { it.route == route }) {
        navigate(route) {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    } else {
        navigate(route) { launchSingleTop = true }
    }
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = topLevelItems.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    topLevelItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { nav.navigateTo(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label), maxLines = 1) },
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(inner).consumeWindowInsets(inner)
        ) {
            composable(Routes.HOME) { HomeScreen(onNavigate = { nav.navigateTo(it) }) }
            composable(Routes.SCANNER) {
                ScannerScreen(
                    onOpenSignal = { nav.navigateTo(Routes.SIGNAL) },
                    onConnect = { ssid -> nav.navigateTo(Routes.quickConnect(ssid)) }
                )
            }
            composable(Routes.DEVICES) { DevicesScreen() }
            composable(Routes.SPEED) { SpeedScreen() }
            composable(Routes.SECURITY) { SecurityScreen() }
            composable(Routes.SETTINGS) { SettingsScreen(onOpenSaved = { nav.navigateTo(Routes.SAVED) }) }
            composable(Routes.SIGNAL) { SignalScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.SAVED) { SavedNetworksScreen(onBack = { nav.popBackStack() }) }
            composable(
                route = Routes.QUICK_CONNECT_PATTERN,
                arguments = listOf(
                    navArgument("ssid") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                QuickConnectScreen(
                    initialSsid = entry.arguments?.getString("ssid"),
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
