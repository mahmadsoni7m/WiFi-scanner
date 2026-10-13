package com.wifiscanner.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wifiscanner.app.navigation.AppNavigation
import com.wifiscanner.app.ui.theme.ThemeMode
import com.wifiscanner.app.ui.theme.WifiScannerTheme
import com.wifiscanner.app.ui.theme.isDark

/** AppCompatActivity (a FragmentActivity) so BiometricPrompt and per-app language switching work. */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as WifiScannerApp).container
        setContent {
            val theme by container.settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.DARK)
            val dark = theme.isDark(isSystemInDarkTheme())
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                )
                onDispose { }
            }
            WifiScannerTheme(theme) { AppNavigation() }
        }
    }
}
