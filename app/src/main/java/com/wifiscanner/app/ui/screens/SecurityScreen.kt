package com.wifiscanner.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wifiscanner.app.R
import com.wifiscanner.app.domain.FindingKind
import com.wifiscanner.app.security.RouterChecklist
import com.wifiscanner.app.ui.components.Banner
import com.wifiscanner.app.ui.components.BannerKind
import com.wifiscanner.app.ui.components.FindingCard
import com.wifiscanner.app.ui.components.LoadingRow
import com.wifiscanner.app.ui.components.ScreenHeader
import com.wifiscanner.app.ui.components.SectionTitle
import com.wifiscanner.app.ui.components.WsCard
import com.wifiscanner.app.utils.copyToClipboard
import com.wifiscanner.app.utils.formatDateTime
import com.wifiscanner.app.utils.shareText
import com.wifiscanner.app.viewmodel.SecurityViewModel
import com.wifiscanner.app.viewmodel.appViewModelFactory

@Composable
fun SecurityScreen(vm: SecurityViewModel = viewModel(factory = appViewModelFactory())) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            ScreenHeader(stringResource(R.string.nav_security), stringResource(R.string.security_subtitle))
        }
        Spacer(Modifier.height(8.dp))
        TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.security_tab_checker)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.security_tab_testing)) })
        }
        if (tab == 0) CheckerTab(vm) else TestingTab(vm)
    }
}

@Composable
private fun CheckerTab(vm: SecurityViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val saved by vm.lastSaved.collectAsStateWithLifecycle()
    val facts = state.findings.filter { it.kind != FindingKind.RECOMMENDATION }
    val recommendations = state.findings.filter { it.kind == FindingKind.RECOMMENDATION }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Banner(BannerKind.INFO, stringResource(R.string.security_limits)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.runCheck() }, enabled = !state.running) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text(stringResource(R.string.sec_run_check), Modifier.padding(start = 6.dp))
                }
                if (state.running) LoadingRow(stringResource(R.string.sec_checking))
                val stamp = state.checkedAt ?: saved?.timestamp
                if (stamp != null) {
                    Text(
                        stringResource(R.string.sec_last_check, formatDateTime(stamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (state.findings.isEmpty() && !state.running) {
            item {
                Text(
                    stringResource(R.string.sec_not_run),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (facts.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.sec_section_findings)) }
            items(facts) { FindingCard(it) }
        }
        if (recommendations.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.sec_section_recommendations)) }
            items(recommendations) { FindingCard(it) }
        }
    }
}

@Composable
private fun TestingTab(vm: SecurityViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val checked by vm.checklist.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hasReport = state.findings.isNotEmpty() || state.diagnostics.isNotEmpty()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Banner(BannerKind.WARNING, stringResource(R.string.testing_authorized)) }
        item {
            WsCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.consent, onCheckedChange = { vm.setConsent(it) })
                    Text(stringResource(R.string.testing_consent), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.testing_not_included),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { SectionTitle(stringResource(R.string.testing_diagnostics)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.testing_diag_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { vm.runDiagnostics() }, enabled = state.consent && !state.diagRunning) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text(stringResource(R.string.testing_run), Modifier.padding(start = 6.dp))
                }
                if (state.diagRunning) LoadingRow(stringResource(R.string.testing_running))
            }
        }
        items(state.diagnostics) { FindingCard(it) }

        item { SectionTitle(stringResource(R.string.testing_checklist)) }
        item {
            WsCard(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.testing_checklist_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                RouterChecklist.items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.id in checked, onCheckedChange = { vm.toggleChecklist(item.id, it) })
                        Text(stringResource(item.labelRes), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item { SectionTitle(stringResource(R.string.testing_report)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!hasReport) {
                    Text(
                        stringResource(R.string.testing_report_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = hasReport,
                        onClick = {
                            copyToClipboard(context, context.getString(R.string.testing_report), vm.buildReport(context))
                            Toast.makeText(context, R.string.testing_report_copied, Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                        Text(stringResource(R.string.copy), Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(
                        enabled = hasReport,
                        onClick = { shareText(context, context.getString(R.string.testing_report), vm.buildReport(context)) }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Text(stringResource(R.string.share), Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    }
}
