package com.wifiscanner.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.wifiscanner.app.R
import com.wifiscanner.app.utils.PermissionUtils
import com.wifiscanner.app.utils.openAppSettings

class WifiPermissionState(
    val granted: Boolean,
    /** True once the system dialog has been shown and answered during this screen's lifetime. */
    val requestedOnce: Boolean,
    val request: () -> Unit
)

@Composable
fun rememberWifiPermissionState(): WifiPermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(PermissionUtils.hasWifiPermissions(context)) }
    var requestedOnce by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = PermissionUtils.hasWifiPermissions(context)
        requestedOnce = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = PermissionUtils.hasWifiPermissions(context)
    }
    return WifiPermissionState(granted, requestedOnce) {
        launcher.launch(PermissionUtils.requiredWifiPermissions())
    }
}

/** Explains why the permission is needed BEFORE the system dialog is shown. */
@Composable
fun PermissionCard(state: WifiPermissionState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    WsCard(modifier) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.perm_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.perm_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.requestedOnce) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.perm_denied_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = state.request) { Text(stringResource(R.string.perm_allow)) }
            if (state.requestedOnce) {
                OutlinedButton(onClick = { openAppSettings(context) }) { Text(stringResource(R.string.perm_open_settings)) }
            }
        }
    }
}
