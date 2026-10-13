package com.wifiscanner.app.security

import android.content.Context
import com.wifiscanner.app.R
import com.wifiscanner.app.domain.Finding
import com.wifiscanner.app.domain.FindingKind
import com.wifiscanner.app.utils.formatDateTime

data class ChecklistItem(val id: String, val labelRes: Int)

/** Items the user ticks manually; the app cannot verify them from an ordinary Android app. */
object RouterChecklist {
    val items = listOf(
        ChecklistItem("admin_password", R.string.chk_admin_password),
        ChecklistItem("wifi_password", R.string.chk_wifi_password),
        ChecklistItem("encryption", R.string.chk_encryption),
        ChecklistItem("wps", R.string.chk_wps),
        ChecklistItem("firmware", R.string.chk_firmware),
        ChecklistItem("remote_admin", R.string.chk_remote_admin),
        ChecklistItem("guest", R.string.chk_guest),
        ChecklistItem("devices", R.string.chk_devices)
    )
}

/** Plain-text report of the findings that are actually available. */
object ReportBuilder {

    fun build(
        context: Context,
        timestamp: Long,
        findings: List<Finding>,
        diagnostics: List<Finding>,
        checked: Set<String>
    ): String {
        val sb = StringBuilder()
        sb.appendLine(context.getString(R.string.report_title))
        sb.appendLine(context.getString(R.string.report_generated, formatDateTime(timestamp)))
        sb.appendLine()

        sb.appendLine(context.getString(R.string.report_section_checks))
        if (findings.isEmpty()) sb.appendLine(context.getString(R.string.report_none)) else findings.forEach { sb.appendLine(line(context, it)) }
        sb.appendLine()

        sb.appendLine(context.getString(R.string.report_section_diagnostics))
        if (diagnostics.isEmpty()) sb.appendLine(context.getString(R.string.report_none)) else diagnostics.forEach { sb.appendLine(line(context, it)) }
        sb.appendLine()

        sb.appendLine(context.getString(R.string.report_section_checklist))
        RouterChecklist.items.forEach {
            val mark = if (it.id in checked) "[x]" else "[ ]"
            sb.appendLine("$mark ${context.getString(it.labelRes)}")
        }
        sb.appendLine()
        sb.appendLine(context.getString(R.string.report_disclaimer))
        return sb.toString()
    }

    private fun line(context: Context, f: Finding): String {
        val kind = context.getString(
            when (f.kind) {
                FindingKind.VERIFIED -> R.string.kind_verified
                FindingKind.WARNING -> R.string.kind_warning
                FindingKind.RECOMMENDATION -> R.string.kind_recommendation
                FindingKind.UNAVAILABLE -> R.string.kind_unavailable
            }
        )
        val detail = f.detailRes?.let { context.getString(it, *f.args.toTypedArray()) }
        return buildString {
            append("- [").append(kind).append("] ").append(context.getString(f.titleRes))
            if (detail != null) append(": ").append(detail)
        }
    }
}
