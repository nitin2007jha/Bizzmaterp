package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.FormatUtils
import com.example.ui.components.ErpStatusBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoft
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoft
import com.example.ui.viewmodel.AiReportUiState
import com.example.ui.viewmodel.ErpUiState
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun GstCenterScreen(
    state: ErpUiState,
    onExportMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var subTab by remember { mutableStateOf("dashboard") }
    val s = state.stats
    val finalInvoices = remember(state.invoices) {
        state.invoices.filter { !it.isDeleted && it.status == "final" && it.docType == "invoice" }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tabs = listOf(
                        "dashboard" to "Dashboard",
                        "gstr1" to "GSTR-1",
                        "gstr3b" to "GSTR-3B",
                        "itc" to "ITC Credit",
                        "calc" to "Calculator",
                        "calendar" to "Calendar"
                    )
                    items(tabs) { (k, lbl) ->
                        FilterChip(
                            selected = subTab == k,
                            onClick = { subTab = k },
                            label = { Text(lbl) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White),
                            modifier = Modifier.testTag("gst_tab_$k")
                        )
                    }
                }
            }

            when (subTab) {
                "dashboard" -> {
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("GST Summary (${state.settings.gstin.ifBlank { "GSTIN not set" }})", style = MaterialTheme.typography.titleMedium)
                                SummaryRow("Output CGST", FormatUtils.inr(s.cgstOutput))
                                SummaryRow("Output SGST", FormatUtils.inr(s.sgstOutput))
                                SummaryRow("Output IGST", FormatUtils.inr(s.igstOutput))
                                SummaryRow("Input Tax Credit (ITC)", "-${FormatUtils.inr(s.itcCredit)}", EmeraldAccent)
                                HorizontalDivider()
                                SummaryRow("Net GST Payable", FormatUtils.inr(s.netGstPayable), TealPrimary)
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                val root = JSONObject().apply {
                                    put("gstin", state.settings.gstin)
                                    put("fp", FormatUtils.currentMonthStr().replace("-", ""))
                                    put("b2b_count", finalInvoices.count { it.clientGstin.isNotBlank() })
                                    put("b2c_count", finalInvoices.count { it.clientGstin.isBlank() })
                                    put("invoices", JSONArray(finalInvoices.map { inv ->
                                        JSONObject().apply {
                                            put("inum", inv.invoiceNumber)
                                            put("idt", inv.dateStr)
                                            put("val", inv.grandTotal)
                                            put("ctin", inv.clientGstin)
                                            put("txval", inv.subtotal)
                                            put("camt", inv.cgstTotal)
                                            put("samt", inv.sgstTotal)
                                            put("iamt", inv.igstTotal)
                                        }
                                    }))
                                }
                                FormatUtils.exportAndShareTextFile(context, "GSTR1_${FormatUtils.currentMonthStr()}.json", "application/json", root.toString(2))
                                    .onSuccess { onExportMessage("GSTR-1 JSON exported") }
                                    .onFailure { onExportMessage(it.localizedMessage ?: "Export failed") }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("export_gstr1_json_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Export GSTR-1 JSON")
                        }
                    }
                }

                "gstr1" -> {
                    val b2b = finalInvoices.filter { it.clientGstin.isNotBlank() }
                    val b2c = finalInvoices.filter { it.clientGstin.isBlank() }
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("GSTR-1 Outward Supplies", style = MaterialTheme.typography.titleMedium)
                                SummaryRow("B2B Invoices (${b2b.size})", FormatUtils.inr(b2b.sumOf { it.grandTotal }))
                                SummaryRow("B2C Invoices (${b2c.size})", FormatUtils.inr(b2c.sumOf { it.grandTotal }))
                                SummaryRow("Total Taxable Value", FormatUtils.inr(finalInvoices.sumOf { it.subtotal }))
                            }
                        }
                    }
                }

                "gstr3b" -> {
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("GSTR-3B Monthly Return", style = MaterialTheme.typography.titleMedium)
                                SummaryRow("3.1 Outward Taxable Supplies", FormatUtils.inr(finalInvoices.sumOf { it.subtotal }))
                                SummaryRow("Total Output Tax", FormatUtils.inr(s.cgstOutput + s.sgstOutput + s.igstOutput))
                                SummaryRow("4. Eligible ITC", FormatUtils.inr(s.itcCredit))
                                SummaryRow("6.1 Net Tax Payable in Cash", FormatUtils.inr(s.netGstPayable), TealPrimary)
                            }
                        }
                    }
                }

                "itc" -> {
                    val itcExpenses = state.expenses.filter { !it.isDeleted && it.gstAmount > 0 }
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Eligible ITC Bills (${itcExpenses.size})", style = MaterialTheme.typography.titleMedium)
                                if (itcExpenses.isEmpty()) Text("Record expenses with GST ITC to claim input credit.", style = MaterialTheme.typography.bodySmall)
                                itcExpenses.forEach { e -> SummaryRow("${e.title} (${e.dateStr})", FormatUtils.inr(e.gstAmount), EmeraldAccent) }
                            }
                        }
                    }
                }

                "calc" -> {
                    item {
                        var baseAmt by remember { mutableStateOf("1000") }
                        var rate by remember { mutableStateOf("18") }
                        val amt = baseAmt.toDoubleOrNull() ?: 0.0
                        val r = rate.toDoubleOrNull() ?: 0.0
                        val gst = amt * (r / 100.0)
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("GST Rate Calculator", style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(value = baseAmt, onValueChange = { baseAmt = it }, label = { Text("Taxable Amount ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(listOf("5", "12", "18", "28")) { pct ->
                                        FilterChip(selected = rate == pct, onClick = { rate = pct }, label = { Text("$pct%") })
                                    }
                                }
                                SummaryRow("CGST (${r / 2}%)", FormatUtils.inr(gst / 2.0))
                                SummaryRow("SGST (${r / 2}%)", FormatUtils.inr(gst / 2.0))
                                SummaryRow("Total with GST", FormatUtils.inr(amt + gst), TealPrimary)
                            }
                        }
                    }
                }

                "calendar" -> {
                    val deadlines = listOf(
                        "GSTR-1 (Monthly)" to "11th of every month",
                        "GSTR-3B (Monthly)" to "20th of every month",
                        "QRMP IFF" to "13th of next month",
                        "CMP-08 (Composition)" to "18th post-quarter",
                        "GSTR-9 Annual Return" to "31st December"
                    )
                    items(deadlines) { (form, due) ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(form, style = MaterialTheme.typography.titleSmall)
                                ErpStatusBadge(due, WarningSoft, WarningAmber)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialReportsScreen(
    state: ErpUiState,
    onExportMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf("overview") }
    val s = state.stats
    val stockCostValue = state.products.filter { !it.isDeleted && !it.isService }.sumOf { it.stock * it.costPrice }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tabs = listOf("overview" to "Overview", "pnl" to "P&L", "bs" to "Balance Sheet", "tb" to "Trial Balance", "cf" to "Cash Flow", "aging" to "Aging")
                    items(tabs) { (k, lbl) ->
                        FilterChip(
                            selected = tab == k,
                            onClick = { tab = k },
                            label = { Text(lbl) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
                        )
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (tab) {
                            "overview" -> {
                                Text("Financial Overview", style = MaterialTheme.typography.titleMedium)
                                SummaryRow("Gross Revenue", FormatUtils.inr(s.totalRevenue), EmeraldAccent)
                                SummaryRow("Operating Expenses", FormatUtils.inr(s.totalExpenses), DangerRed)
                                SummaryRow("Net Profit", FormatUtils.inr(s.netProfit), TealPrimary)
                                SummaryRow("Inventory Asset Value", FormatUtils.inr(stockCostValue))
                                SummaryRow("Receivables Outstanding", FormatUtils.inr(s.unpaidAmount), WarningAmber)
                            }
                            "pnl" -> {
                                Text("Profit & Loss Statement", style = MaterialTheme.typography.titleMedium)
                                SummaryRow("Sales Revenue", FormatUtils.inr(s.totalRevenue))
                                SummaryRow("Less: Expenses", FormatUtils.inr(s.totalExpenses))
                                HorizontalDivider()
                                SummaryRow("Net Profit / (Loss)", FormatUtils.inr(s.netProfit), if (s.netProfit >= 0) SuccessGreen else DangerRed)
                            }
                            "bs" -> {
                                Text("Balance Sheet Snapshot", style = MaterialTheme.typography.titleMedium)
                                SummaryRow("Accounts Receivable", FormatUtils.inr(s.unpaidAmount))
                                SummaryRow("Inventory Valuation", FormatUtils.inr(stockCostValue))
                                SummaryRow("Net GST Liability", FormatUtils.inr(s.netGstPayable), DangerRed)
                                HorizontalDivider()
                                SummaryRow("Net Working Position", FormatUtils.inr(s.unpaidAmount + stockCostValue - s.netGstPayable), TealPrimary)
                            }
                            "tb" -> {
                                Text("Trial Balance", style = MaterialTheme.typography.titleMedium)
                                SummaryRow("Sales (Cr)", FormatUtils.inr(s.totalRevenue))
                                SummaryRow("Expenses (Dr)", FormatUtils.inr(s.totalExpenses))
                                SummaryRow("Receivables (Dr)", FormatUtils.inr(s.unpaidAmount))
                                SummaryRow("GST Output (Cr)", FormatUtils.inr(s.cgstOutput + s.sgstOutput + s.igstOutput))
                            }
                            "cf" -> {
                                Text("Cash Flow Projections", style = MaterialTheme.typography.titleMedium)
                                SummaryRow("7-Day Expected Inflow", FormatUtils.inr(s.forecast7d))
                                SummaryRow("15-Day Expected Inflow", FormatUtils.inr(s.forecast15d))
                                SummaryRow("30-Day Expected Inflow", FormatUtils.inr(s.forecast30d))
                            }
                            "aging" -> {
                                Text("Accounts Receivable Aging", style = MaterialTheme.typography.titleMedium)
                                SummaryRow("0 – 30 Days", FormatUtils.inr(s.aging0To30))
                                SummaryRow("31 – 60 Days", FormatUtils.inr(s.aging31To60), WarningAmber)
                                SummaryRow("60+ Days Overdue", FormatUtils.inr(s.aging60Plus), DangerRed)
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        val csv = buildString {
                            appendLine("Metric,Amount")
                            appendLine("Total Revenue,${s.totalRevenue}")
                            appendLine("Total Expenses,${s.totalExpenses}")
                            appendLine("Net Profit,${s.netProfit}")
                            appendLine("Receivables,${s.unpaidAmount}")
                            appendLine("Net GST Payable,${s.netGstPayable}")
                        }
                        FormatUtils.exportAndShareTextFile(context, "Financial_Summary_${FormatUtils.todayStr()}.csv", "text/csv", csv)
                            .onSuccess { onExportMessage("Report CSV exported") }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Export Financial Summary CSV")
                }
            }
        }
    }
}

@Composable
fun AiBusinessReportScreen(
    aiState: AiReportUiState,
    onGenerate: (String) -> Unit
) {
    val context = LocalContext.current
    var period by remember { mutableStateOf("This Month") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("AI Business Advisor (Gemini)", style = MaterialTheme.typography.titleMedium)
                        Text("Analyze your live revenue, expenses, receivables, and low-stock risks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("This Month", "Last Month", "All Time").forEach { p ->
                                FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White))
                            }
                        }
                        Button(
                            onClick = { onGenerate(period) },
                            enabled = aiState !is AiReportUiState.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("generate_ai_report_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (aiState is AiReportUiState.Loading) "Analyzing ERP Data..." else "Generate AI Business Report")
                        }
                    }
                }
            }

            when (aiState) {
                AiReportUiState.Idle -> {}
                AiReportUiState.Loading -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TealPrimary)
                        }
                    }
                }
                is AiReportUiState.Error -> {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(aiState.message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
                is AiReportUiState.Success -> {
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(aiState.markdownText, style = MaterialTheme.typography.bodyMedium)
                                OutlinedButton(
                                    onClick = { FormatUtils.shareText(context, "AI Business Report ($period)", aiState.markdownText) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Share Executive Report")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CaPortalScreen(state: ErpUiState, onExportMessage: (String) -> Unit) {
    val context = LocalContext.current
    val finalInvoices = state.invoices.filter { !it.isDeleted && it.status == "final" }
    val expenses = state.expenses.filter { !it.isDeleted }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CA & Audit Export Portal", style = MaterialTheme.typography.titleMedium)
                    Text("One-tap accountant-ready registers for tax filing and audit reconciliation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SummaryRow("Finalized Invoices", finalInvoices.size.toString())
                    SummaryRow("Expense Vouchers", expenses.size.toString())
                    SummaryRow("Net GST Payable", FormatUtils.inr(state.stats.netGstPayable), TealPrimary)
                }
            }

            Button(
                onClick = {
                    val csv = buildString {
                        appendLine("InvoiceNo,Date,Client,GSTIN,Subtotal,CGST,SGST,IGST,GrandTotal,Status")
                        finalInvoices.forEach {
                            appendLine("\"${it.invoiceNumber}\",${it.dateStr},\"${it.clientName}\",\"${it.clientGstin}\",${it.subtotal},${it.cgstTotal},${it.sgstTotal},${it.igstTotal},${it.grandTotal},${it.paymentStatus}")
                        }
                    }
                    FormatUtils.exportAndShareTextFile(context, "CA_Sales_Register_${FormatUtils.todayStr()}.csv", "text/csv", csv)
                        .onSuccess { onExportMessage("Sales Register exported") }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Export Sales Register (CSV)") }

            OutlinedButton(
                onClick = {
                    val csv = buildString {
                        appendLine("Date,Title,Category,Vendor,Amount,GST_ITC,Mode,Status")
                        expenses.forEach {
                            appendLine("${it.dateStr},\"${it.title}\",\"${it.category}\",\"${it.vendor}\",${it.amount},${it.gstAmount},${it.paymentMode},${it.approvalStatus}")
                        }
                    }
                    FormatUtils.exportAndShareTextFile(context, "CA_Expense_Register_${FormatUtils.todayStr()}.csv", "text/csv", csv)
                        .onSuccess { onExportMessage("Expense Register exported") }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Export Expense Register (CSV)") }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor)
    }
}
