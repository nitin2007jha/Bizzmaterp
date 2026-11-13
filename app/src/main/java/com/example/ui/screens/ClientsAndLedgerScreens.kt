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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.clip
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
import com.example.data.model.ErpClient
import com.example.data.model.ErpExpense
import com.example.ui.components.ErpStatusBadge
import com.example.ui.components.SwipeActionCard
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoft
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoft
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoft
import com.example.ui.viewmodel.ErpUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    state: ErpUiState,
    onSaveClient: (ErpClient) -> Unit,
    onSoftDelete: (ErpClient) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("all") }
    var selectedClient by remember { mutableStateOf<ErpClient?>(null) }
    var editingClient by remember { mutableStateOf<ErpClient?>(null) }

    fun clientBalance(cli: ErpClient): Double {
        val unpaidInv = state.invoices
            .filter { !it.isDeleted && it.status == "final" && it.paymentStatus != "paid" && (it.clientId == cli.id || it.clientName.equals(cli.name, ignoreCase = true)) }
            .sumOf { (it.grandTotal - it.paidAmount).coerceAtLeast(0.0) }
        return cli.openingBalance + unpaidInv
    }

    val clients = remember(state.clients, state.invoices, query, filterMode) {
        val q = query.trim().lowercase()
        state.clients.filter { !it.isDeleted }
            .filter { cli ->
                val bal = clientBalance(cli)
                when (filterMode) {
                    "due" -> bal > 0.0
                    "settled" -> bal <= 0.0
                    else -> true
                }
            }
            .filter { q.isBlank() || it.name.lowercase().contains(q) || it.phone.contains(q) || it.gstin.lowercase().contains(q) }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Search clients, phone, GSTIN...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).testTag("clients_search_input")
                )
                Button(
                    onClick = { editingClient = ErpClient() },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(52.dp).testTag("clients_add_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Client")
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("all" to "All Clients", "due" to "Balance Due", "settled" to "Settled")) { (k, lbl) ->
                    FilterChip(
                        selected = filterMode == k,
                        onClick = { filterMode = k },
                        label = { Text(lbl) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
                    )
                }
            }

            val totalDue = remember(clients) { clients.sumOf { clientBalance(it).coerceAtLeast(0.0) } }
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${clients.size} Active Clients", style = MaterialTheme.typography.titleSmall)
                    Text("Total Due: ${FormatUtils.inr(totalDue)}", style = MaterialTheme.typography.titleSmall, color = if (totalDue > 0) WarningAmber else SuccessGreen)
                }
            }

            if (clients.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No clients yet. Tap + Client to add parties to your CRM.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(clients, key = { it.id }) { cli ->
                        val bal = clientBalance(cli)
                        val overLimit = cli.creditLimit > 0 && bal > cli.creditLimit
                        val ltv = state.invoices
                            .filter { !it.isDeleted && it.status == "final" && (it.clientId == cli.id || it.clientName.equals(cli.name, ignoreCase = true)) }
                            .sumOf { it.grandTotal }
                        val tier = when {
                            ltv >= 25000.0 -> "VIP"
                            ltv >= 5000.0 -> "REGULAR"
                            else -> null
                        }
                        SwipeActionCard(
                            onSwipeRight = { editingClient = cli },
                            rightLabel = "Edit",
                            rightColor = TealPrimary,
                            onSwipeLeft = { onSoftDelete(cli) },
                            leftLabel = "Trash"
                        ) {
                            Card(
                                onClick = { selectedClient = cli },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth().testTag("client_card_${cli.name}")
                            ) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(cli.name, style = MaterialTheme.typography.titleMedium)
                                            if (tier != null) {
                                                ErpStatusBadge(tier, MaterialTheme.colorScheme.primaryContainer, TealPrimary)
                                            }
                                        }
                                        Text(
                                            listOfNotNull(
                                                cli.phone.takeIf { it.isNotBlank() },
                                                cli.gstin.takeIf { it.isNotBlank() },
                                                if (ltv > 0) "Billed ${FormatUtils.inr(ltv)}" else null
                                            ).joinToString(" • ").ifBlank { "No phone added" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(FormatUtils.inr(bal), style = MaterialTheme.typography.titleSmall, color = if (overLimit) DangerRed else if (bal > 0) WarningAmber else SuccessGreen)
                                        ErpStatusBadge(
                                            if (overLimit) "OVER LIMIT" else if (bal > 0) "DUE" else "SETTLED",
                                            if (overLimit) DangerSoft else if (bal > 0) WarningSoft else SuccessSoft,
                                            if (overLimit) DangerRed else if (bal > 0) WarningAmber else SuccessGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedClient?.let { cli ->
        val cliInvoices = state.invoices.filter { !it.isDeleted && (it.clientId == cli.id || it.clientName.equals(cli.name, ignoreCase = true)) }
        val bal = clientBalance(cli)
        ModalBottomSheet(onDismissRequest = { selectedClient = null }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(cli.name, style = MaterialTheme.typography.headlineSmall)
                Text("Ledger Balance: ${FormatUtils.inr(bal)}", style = MaterialTheme.typography.titleMedium, color = TealPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { editingClient = cli; selectedClient = null }, modifier = Modifier.weight(1f)) { Text("Edit") }
                    OutlinedButton(
                        onClick = {
                            val statement = buildString {
                                appendLine("ACCOUNT STATEMENT — ${state.settings.businessName}")
                                appendLine("Party: ${cli.name} (${cli.phone})")
                                appendLine("Current Balance Due: ${FormatUtils.inr(bal)}")
                                appendLine("--- Recent Transactions ---")
                                cliInvoices.take(10).forEach {
                                    appendLine("${it.dateStr} | ${it.invoiceNumber} | ${FormatUtils.inr(it.grandTotal)} | ${it.paymentStatus.uppercase()}")
                                }
                            }
                            FormatUtils.shareText(context, "Ledger Statement - ${cli.name}", statement)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Statement") }
                    Button(
                        onClick = {
                            val msg = "Hello ${cli.name}, friendly reminder for pending balance ${FormatUtils.inr(bal)} with ${state.settings.businessName}."
                            FormatUtils.shareText(context, "Payment Reminder", msg)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Remind")
                    }
                }
                Text("Transactions (${cliInvoices.size})", style = MaterialTheme.typography.titleSmall)
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(cliInvoices) { inv ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${inv.invoiceNumber} • ${inv.dateStr}", style = MaterialTheme.typography.bodyMedium)
                            Text("${FormatUtils.inr(inv.grandTotal)} (${inv.paymentStatus})", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }
    }

    editingClient?.let { initial ->
        ClientEditorSheet(
            initial = initial,
            onDismiss = { editingClient = null },
            onSave = {
                onSaveClient(it)
                editingClient = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientEditorSheet(initial: ErpClient, onDismiss: () -> Unit, onSave: (ErpClient) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var phone by remember { mutableStateOf(initial.phone) }
    var email by remember { mutableStateOf(initial.email) }
    var gstin by remember { mutableStateOf(initial.gstin) }
    var stateName by remember { mutableStateOf(initial.state) }
    var address by remember { mutableStateOf(initial.address) }
    var notes by remember { mutableStateOf(initial.notes) }
    var opening by remember { mutableStateOf(if (initial.openingBalance != 0.0) initial.openingBalance.toString() else "") }
    var creditLimit by remember { mutableStateOf(if (initial.creditLimit > 0) initial.creditLimit.toString() else "") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (initial.id.isBlank()) "Add Client" else "Edit Client", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Client / Party Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("client_name_input"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = gstin, onValueChange = { gstin = it.uppercase() }, label = { Text("GSTIN") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.weight(1.2f))
                OutlinedTextField(value = stateName, onValueChange = { stateName = it }, label = { Text("State") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = opening, onValueChange = { opening = it }, label = { Text("Opening Bal ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = creditLimit, onValueChange = { creditLimit = it }, label = { Text("Credit Limit ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(initial.copy(name = name.trim(), phone = phone.trim(), email = email.trim(), gstin = gstin.trim(), state = stateName.trim(), address = address.trim(), notes = notes.trim(), openingBalance = opening.toDoubleOrNull() ?: 0.0, creditLimit = creditLimit.toDoubleOrNull() ?: 0.0))
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("client_save_button")
            ) { Text("Save Client") }
        }
    }
}

private data class DaybookEntry(val dateStr: String, val title: String, val subtitle: String, val amount: Double, val isCredit: Boolean)

@Composable
fun DaybookScreen(
    state: ErpUiState,
    onExportMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var typeFilter by remember { mutableStateOf("all") }
    val allEntries = remember(state.invoices, state.expenses) {
        val rev = state.invoices.filter { !it.isDeleted && it.status == "final" && it.docType == "invoice" }.map {
            DaybookEntry(it.dateStr, "Sale ${it.invoiceNumber}", "${it.clientName.ifBlank { "Walk-in" }} • ${it.paymentMode.uppercase()}", it.grandTotal, true)
        }
        val exp = state.expenses.filter { !it.isDeleted && it.approvalStatus != "rejected" }.map {
            DaybookEntry(it.dateStr, it.title.ifBlank { it.category }, "${it.category} • ${it.paymentMode.uppercase()}", it.amount, false)
        }
        (rev + exp).sortedByDescending { it.dateStr }
    }
    val entries = remember(allEntries, typeFilter) {
        when (typeFilter) {
            "in" -> allEntries.filter { it.isCredit }
            "out" -> allEntries.filter { !it.isCredit }
            else -> allEntries
        }
    }
    val cashIn = allEntries.filter { it.isCredit }.sumOf { it.amount }
    val cashOut = allEntries.filter { !it.isCredit }.sumOf { it.amount }
    val netFlow = cashIn - cashOut

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = SuccessSoft)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Inflow", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                        Text(FormatUtils.inr(cashIn), style = MaterialTheme.typography.titleSmall, color = SuccessGreen)
                    }
                }
                Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = DangerSoft)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Outflow", style = MaterialTheme.typography.labelSmall, color = DangerRed)
                        Text(FormatUtils.inr(cashOut), style = MaterialTheme.typography.titleSmall, color = DangerRed)
                    }
                }
                Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Net Cash", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(FormatUtils.inr(netFlow), style = MaterialTheme.typography.titleSmall, color = if (netFlow >= 0) TealPrimary else DangerRed)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all" to "All", "in" to "Inflow (+)", "out" to "Outflow (-)").forEach { (k, lbl) ->
                        FilterChip(
                            selected = typeFilter == k,
                            onClick = { typeFilter = k },
                            label = { Text(lbl) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
                        )
                    }
                }
                IconButton(onClick = {
                    val csv = buildString {
                        appendLine("Date,Type,Title,Details,Amount")
                        entries.forEach { e ->
                            appendLine("${e.dateStr},${if (e.isCredit) "IN" else "OUT"},\"${e.title}\",\"${e.subtitle}\",${e.amount}")
                        }
                    }
                    FormatUtils.exportAndShareTextFile(context, "Daybook_${FormatUtils.todayStr()}.csv", "text/csv", csv)
                        .onSuccess { onExportMessage("Daybook CSV exported") }
                }) {
                    Icon(Icons.Default.Download, contentDescription = "Export Daybook CSV", tint = TealPrimary)
                }
            }
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries) { e ->
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(if (e.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, contentDescription = null, tint = if (e.isCredit) SuccessGreen else DangerRed)
                            Column(Modifier.weight(1f)) {
                                Text(e.title, style = MaterialTheme.typography.titleSmall)
                                Text("${e.dateStr} • ${e.subtitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text((if (e.isCredit) "+" else "-") + FormatUtils.inr(e.amount), style = MaterialTheme.typography.titleSmall, color = if (e.isCredit) SuccessGreen else DangerRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpensesScreen(
    state: ErpUiState,
    onSaveExpense: (ErpExpense) -> Unit,
    onSoftDelete: (ErpExpense) -> Unit
) {
    var filterStatus by remember { mutableStateOf("all") }
    var editing by remember { mutableStateOf<ErpExpense?>(null) }

    val list = remember(state.expenses, filterStatus) {
        state.expenses.filter { !it.isDeleted && (filterStatus == "all" || it.approvalStatus == filterStatus) }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val totalItc = remember(list) { list.filter { it.approvalStatus == "approved" }.sumOf { it.gstAmount } }
            val byCategory = remember(list) {
                list.filter { it.approvalStatus == "approved" }
                    .groupBy { it.category.ifBlank { "Operations" } }
                    .mapValues { (_, items) -> items.sumOf { it.amount } }
                    .entries.sortedByDescending { it.value }.take(3)
            }
            val totalApproved = list.filter { it.approvalStatus == "approved" }.sumOf { it.amount }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Approved Spend • ITC ${FormatUtils.inr(totalItc)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FormatUtils.inr(totalApproved), style = MaterialTheme.typography.titleLarge, color = DangerRed)
                }
                Button(
                    onClick = { editing = ErpExpense(dateStr = FormatUtils.todayStr()) },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("expenses_add_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Expense")
                }
            }

            if (byCategory.isNotEmpty() && totalApproved > 0.0) {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Top Expense Categories", style = MaterialTheme.typography.labelLarge)
                        byCategory.forEach { (cat, amt) ->
                            val frac = (amt / totalApproved).toFloat().coerceIn(0.05f, 1f)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(cat, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(86.dp))
                                LinearProgressIndicator(
                                    progress = { frac },
                                    color = DangerRed,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(4.dp))
                                )
                                Text(FormatUtils.inr(amt), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("all" to "All", "approved" to "Approved", "pending" to "Pending", "rejected" to "Rejected")) { (k, lbl) ->
                    FilterChip(selected = filterStatus == k, onClick = { filterStatus = k }, label = { Text(lbl) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White))
                }
            }

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { exp ->
                    SwipeActionCard(onSwipeLeft = { onSoftDelete(exp) }, leftLabel = "Trash") {
                        Card(onClick = { editing = exp }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(exp.title, style = MaterialTheme.typography.titleMedium)
                                    Text("${exp.category} • ${exp.dateStr} • GST ITC ${FormatUtils.inr(exp.gstAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(FormatUtils.inr(exp.amount), style = MaterialTheme.typography.titleMedium, color = DangerRed)
                                    if (exp.approvalStatus == "pending") {
                                        Row {
                                            IconButton(onClick = { onSaveExpense(exp.copy(approvalStatus = "approved")) }) { Icon(Icons.Default.Check, contentDescription = "Approve", tint = SuccessGreen) }
                                            IconButton(onClick = { onSaveExpense(exp.copy(approvalStatus = "rejected")) }) { Icon(Icons.Default.Close, contentDescription = "Reject", tint = DangerRed) }
                                        }
                                    } else {
                                        ErpStatusBadge(exp.approvalStatus.uppercase(), if (exp.approvalStatus == "approved") SuccessSoft else DangerSoft, if (exp.approvalStatus == "approved") SuccessGreen else DangerRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { initial ->
        ExpenseEditorSheet(initial = initial, onDismiss = { editing = null }, onSave = { onSaveExpense(it); editing = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseEditorSheet(initial: ErpExpense, onDismiss: () -> Unit, onSave: (ErpExpense) -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var category by remember { mutableStateOf(initial.category) }
    var vendor by remember { mutableStateOf(initial.vendor) }
    var amount by remember { mutableStateOf(if (initial.amount > 0) initial.amount.toString() else "") }
    var gst by remember { mutableStateOf(if (initial.gstAmount > 0) initial.gstAmount.toString() else "") }
    var payMode by remember { mutableStateOf(initial.paymentMode) }
    var approval by remember { mutableStateOf(initial.approvalStatus) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (initial.id.isBlank()) "Record Expense" else "Edit Expense", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("expense_title_input"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = vendor, onValueChange = { vendor = it }, label = { Text("Vendor") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount ₹ *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f).testTag("expense_amount_input"))
                OutlinedTextField(value = gst, onValueChange = { gst = it }, label = { Text("GST ITC ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("upi", "cash", "bank", "card")) { m ->
                    FilterChip(selected = payMode == m, onClick = { payMode = m }, label = { Text(m.uppercase()) })
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("approved", "pending", "rejected")) { st ->
                    FilterChip(selected = approval == st, onClick = { approval = st }, label = { Text(st.replaceFirstChar { it.uppercase() }) })
                }
            }
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(initial.copy(title = title.trim(), category = category.ifBlank { "Operations" }.trim(), vendor = vendor.trim(), amount = amount.toDoubleOrNull() ?: 0.0, gstAmount = gst.toDoubleOrNull() ?: 0.0, paymentMode = payMode, approvalStatus = approval))
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("expense_save_button")
            ) { Text("Save Expense") }
        }
    }
}
