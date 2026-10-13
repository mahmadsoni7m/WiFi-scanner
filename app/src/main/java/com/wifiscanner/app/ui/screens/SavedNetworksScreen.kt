package com.wifiscanner.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wifiscanner.app.R
import com.wifiscanner.app.data.SaveResult
import com.wifiscanner.app.data.SavedNetworkEntity
import com.wifiscanner.app.security.BiometricHelper
import com.wifiscanner.app.ui.components.Banner
import com.wifiscanner.app.ui.components.BannerKind
import com.wifiscanner.app.ui.components.ConfirmDialog
import com.wifiscanner.app.ui.components.EmptyState
import com.wifiscanner.app.ui.components.SubScreenScaffold
import com.wifiscanner.app.ui.components.WsCard
import com.wifiscanner.app.utils.copyToClipboard
import com.wifiscanner.app.utils.findActivity
import com.wifiscanner.app.viewmodel.SavedNetworksViewModel
import com.wifiscanner.app.viewmodel.appViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun SavedNetworksScreen(
    onBack: () -> Unit,
    vm: SavedNetworksViewModel = viewModel(factory = appViewModelFactory())
) {
    val networks by vm.networks.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val query by vm.queryText.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf<SavedNetworkEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<SavedNetworkEntity?>(null) }
    var fallbackCopy by remember { mutableStateOf<SavedNetworkEntity?>(null) }

    fun copyNow(entity: SavedNetworkEntity) {
        scope.launch {
            val password = vm.reveal(entity)
            if (password != null) {
                copyToClipboard(context, context.getString(R.string.saved_password), password, sensitive = true)
                Toast.makeText(context, R.string.saved_copied, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.saved_copy_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun requestCopy(entity: SavedNetworkEntity) {
        val activity = context.findActivity() as? FragmentActivity
        if (activity == null) {
            fallbackCopy = entity
            return
        }
        BiometricHelper.authenticate(
            activity = activity,
            title = context.getString(R.string.saved_auth_title),
            subtitle = context.getString(R.string.saved_auth_subtitle),
            cancelText = context.getString(R.string.cancel),
            onSuccess = { copyNow(entity) },
            onUnavailable = { fallbackCopy = entity }
        )
    }

    SubScreenScaffold(
        title = stringResource(R.string.saved_title),
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.saved_add))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Banner(BannerKind.INFO, stringResource(R.string.saved_privacy)) }
            if (total > 0) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = vm::setQuery,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = { Text(stringResource(R.string.saved_search)) }
                    )
                }
            }
            if (total == 0) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Key,
                        title = stringResource(R.string.saved_empty_title),
                        message = stringResource(R.string.saved_empty_message),
                        actionLabel = stringResource(R.string.saved_add),
                        onAction = { showAdd = true }
                    )
                }
            }
            items(networks, key = { it.id }) { entity ->
                WsCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entity.ssid, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(if (entity.encryptedPassword != null) R.string.saved_has_password else R.string.saved_no_password),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (entity.encryptedPassword != null) {
                            IconButton(onClick = { requestCopy(entity) }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.saved_copy))
                            }
                        }
                        IconButton(onClick = { editing = entity }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                        }
                        IconButton(onClick = { deleting = entity }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        NetworkEditDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { ssid, pw, _ -> vm.add(ssid, pw) }
        )
    }
    editing?.let { entity ->
        NetworkEditDialog(
            initial = entity,
            onDismiss = { editing = null },
            onSave = { ssid, pw, remove -> vm.update(entity, ssid, pw, remove) }
        )
    }
    deleting?.let { entity ->
        ConfirmDialog(
            title = stringResource(R.string.saved_delete_title),
            message = stringResource(R.string.saved_delete_message, entity.ssid),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                vm.delete(entity)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
    fallbackCopy?.let { entity ->
        AlertDialog(
            onDismissRequest = { fallbackCopy = null },
            title = { Text(stringResource(R.string.saved_no_auth_title)) },
            text = { Text(stringResource(R.string.saved_no_auth_message)) },
            confirmButton = {
                TextButton(onClick = {
                    fallbackCopy = null
                    copyNow(entity)
                }) { Text(stringResource(R.string.saved_copy)) }
            },
            dismissButton = { TextButton(onClick = { fallbackCopy = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun NetworkEditDialog(
    initial: SavedNetworkEntity?,
    onDismiss: () -> Unit,
    onSave: suspend (ssid: String, password: String?, removePassword: Boolean) -> SaveResult
) {
    val scope = rememberCoroutineScope()
    var ssid by remember { mutableStateOf(initial?.ssid ?: "") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var remove by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<SaveResult?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(if (initial == null) R.string.saved_add else R.string.saved_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text(stringResource(R.string.saved_ssid)) },
                    singleLine = true,
                    isError = error == SaveResult.INVALID_SSID || error == SaveResult.DUPLICATE
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(stringResource(if (initial?.encryptedPassword != null) R.string.saved_new_password else R.string.saved_password))
                    },
                    singleLine = true,
                    isError = error == SaveResult.INVALID_PASSWORD,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(R.string.saved_toggle_visibility)
                            )
                        }
                    }
                )
                if (initial?.encryptedPassword != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = remove, onCheckedChange = { remove = it })
                        Text(stringResource(R.string.saved_remove_password), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                error?.let {
                    val message = when (it) {
                        SaveResult.INVALID_SSID -> R.string.saved_err_ssid
                        SaveResult.INVALID_PASSWORD -> R.string.saved_err_password
                        SaveResult.DUPLICATE -> R.string.saved_err_duplicate
                        else -> R.string.saved_err_generic
                    }
                    Text(stringResource(message), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        val result = onSave(ssid, password, remove)
                        busy = false
                        if (result == SaveResult.OK) onDismiss() else error = result
                    }
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.cancel)) } }
    )
}
