package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.example.data.model.BusinessSettings
import com.example.data.model.ErpCoupon
import com.example.data.model.ErpInvoice
import com.example.data.model.ErpOnlineOrder
import com.example.data.model.ErpProduct
import com.example.data.model.ErpPurchaseOrder
import com.example.data.model.ErpRecurringInvoice
import com.example.data.model.ErpWarranty
import com.example.data.model.InvoiceLineItem
import com.example.data.model.PoLineItem
import com.example.ui.components.ErpStatusBadge
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoft
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoft
import com.example.ui.viewmodel.ErpUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessToolsScreen(
    state: ErpUiState,
    onSaveRecurring: (ErpRecurringInvoice) -> Unit,
    onRunRecurring: (ErpInvoice) -> Unit,
    onSaveWarranty: (ErpWarranty) -> Unit,
    onCreateAutoPo: (ErpPurchaseOrder) -> Unit
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf("recurring") }
    var showAddRec by remember { mutableStateOf(false) }
    var showAddWarranty by remember { mutableStateOf(false) }
    var bankCsvLines by remember { mutableStateOf<List<String>>(emptyList()) }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readLines().orEmpty()
            }.onSuccess { lines -> bankCsvLines = lines.take(25) }
        }
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
                        "recurring" to "Recurring",
                        "warranty" to "Warranties & AMC",
                        "reminders" to "Reminders",
                        "autopo" to "Low-Stock PO",
                        "bank" to "Bank Reconcile",
                        "calc" to "Margin & EMI"
                    )
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

            when (tab) {
                "recurring" -> {
                    item {
                        Button(onClick = { showAddRec = true }, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("New Recurring Schedule")
                        }
                    }
                    items(state.recurring, key = { it.id }) { rec ->
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(rec.clientName, style = MaterialTheme.typography.titleMedium)
                                    Text("${rec.frequency.uppercase()} • ${FormatUtils.inr(rec.totalAmount)} • Next: ${rec.nextRunDate}", style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(checked = rec.isActive, onCheckedChange = { onSaveRecurring(rec.copy(isActive = it)) })
                                Spacer(Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        val invNo = "${state.settings.invoicePrefix}-${state.settings.nextInvoiceNumber.toString().padStart(3, '0')}"
                                        onRunRecurring(
                                            ErpInvoice(
                                                invoiceNumber = invNo,
                                                clientName = rec.clientName,
                                                clientPhone = rec.clientPhone,
                                                dateStr = FormatUtils.todayStr(),
                                                subtotal = rec.totalAmount,
                                                grandTotal = rec.totalAmount,
                                                paidAmount = rec.totalAmount,
                                                items = rec.items.ifEmpty { listOf(InvoiceLineItem(name = "Recurring Retainer", qty = 1.0, rate = rec.totalAmount, gstPercent = 0.0)) }
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                                ) {
                                    Icon(Icons.Default.Autorenew, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Run")
                                }
                            }
                        }
                    }
                }

                "warranty" -> {
                    item {
                        Button(onClick = { showAddWarranty = true }, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Warranty / AMC")
                        }
                    }
                    items(state.warranties, key = { it.id }) { w ->
                        val expDate = FormatUtils.addMonthsToDateStr(w.startDate, w.months.toInt())
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${w.productName} (${w.serialNo.ifBlank { "No SN" }})", style = MaterialTheme.typography.titleSmall)
                                    Text("${w.clientName} • ${w.startDate} → $expDate", style = MaterialTheme.typography.bodySmall)
                                }
                                ErpStatusBadge("${w.months}m ${w.type.uppercase()}", SuccessSoft, SuccessGreen)
                            }
                        }
                    }
                }

                "reminders" -> {
                    val unpaid = state.invoices.filter { !it.isDeleted && it.status == "final" && it.paymentStatus != "paid" }
                    if (unpaid.isEmpty()) {
                        item { Text("No pending payment reminders right now!", style = MaterialTheme.typography.bodyMedium) }
                    }
                    items(unpaid, key = { it.id }) { inv ->
                        val due = (inv.grandTotal - inv.paidAmount).coerceAtLeast(0.0)
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${inv.clientName} (#${inv.invoiceNumber})", style = MaterialTheme.typography.titleSmall)
                                    Text("Due: ${FormatUtils.inr(due)}", style = MaterialTheme.typography.bodySmall, color = WarningAmber)
                                }
                                OutlinedButton(onClick = {
                                    val msg = "Hi ${inv.clientName}, reminder for Invoice #${inv.invoiceNumber} balance ${FormatUtils.inr(due)}. — ${state.settings.businessName}"
                                    FormatUtils.shareText(context, "Payment Reminder", msg)
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Send")
                                }
                            }
                        }
                    }
                }

                "autopo" -> {
                    val low = state.products.filter { !it.isDeleted && !it.isService && it.stock <= it.lowStockThreshold }
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Low-Stock Auto PO (${low.size} items)", style = MaterialTheme.typography.titleMedium)
                                low.forEach { p ->
                                    Text("• ${p.name}: ${p.stock.toInt()} ${p.unit} left (Min ${p.lowStockThreshold.toInt()})", style = MaterialTheme.typography.bodySmall)
                                }
                                Button(
                                    onClick = {
                                        val poItems = low.map { PoLineItem(it.id, it.name, (it.lowStockThreshold * 2 - it.stock).coerceAtLeast(5.0), it.costPrice.takeIf { c -> c > 0 } ?: it.sellingPrice * 0.7) }
                                        onCreateAutoPo(
                                            ErpPurchaseOrder(
                                                poNumber = "PO-${(state.purchaseOrders.size + 1).toString().padStart(3, '0')}",
                                                vendorName = "Primary Restock Supplier",
                                                dateStr = FormatUtils.todayStr(),
                                                status = "draft",
                                                items = poItems,
                                                totalAmount = poItems.sumOf { it.qty * it.rate }
                                            )
                                        )
                                    },
                                    enabled = low.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Generate Draft Restock PO") }
                            }
                        }
                    }
                }

                "bank" -> {
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Bank Statement CSV Reconciliation", style = MaterialTheme.typography.titleMedium)
                                Text("Import a bank CSV via Android Document Picker to match invoice & expense amounts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(
                                    onClick = { csvPicker.launch(arrayOf("text/*", "application/csv")) },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Select Bank CSV")
                                }
                                bankCsvLines.forEach { line ->
                                    val amtMatch = state.invoices.firstOrNull { !it.isDeleted && line.contains(it.grandTotal.toLong().toString()) }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(line.take(42), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        ErpStatusBadge(
                                            if (amtMatch != null) "MATCH ${amtMatch.invoiceNumber}" else "UNMATCHED",
                                            if (amtMatch != null) SuccessSoft else WarningSoft,
                                            if (amtMatch != null) SuccessGreen else WarningAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "calc" -> {
                    item {
                        var costStr by remember { mutableStateOf("500") }
                        var sellStr by remember { mutableStateOf("750") }
                        val cost = costStr.toDoubleOrNull() ?: 0.0
                        val sell = sellStr.toDoubleOrNull() ?: 0.0
                        val profit = sell - cost
                        val marginPct = if (sell > 0) (profit / sell) * 100.0 else 0.0
                        val markupPct = if (cost > 0) (profit / cost) * 100.0 else 0.0
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Margin & Markup Calculator", style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = costStr, onValueChange = { costStr = it }, label = { Text("Cost Price ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = sellStr, onValueChange = { sellStr = it }, label = { Text("Selling Price ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                                }
                                Text("Profit: ${FormatUtils.inr(profit)}  •  Margin: ${String.format("%.1f", marginPct)}%  •  Markup: ${String.format("%.1f", markupPct)}%", style = MaterialTheme.typography.titleSmall, color = TealPrimary)
                            }
                        }
                    }
                    item {
                        var principalStr by remember { mutableStateOf("100000") }
                        var rateStr by remember { mutableStateOf("12") }
                        var tenureStr by remember { mutableStateOf("12") }
                        val p = principalStr.toDoubleOrNull() ?: 0.0
                        val r = (rateStr.toDoubleOrNull() ?: 0.0) / 12.0 / 100.0
                        val n = (tenureStr.toIntOrNull() ?: 12).coerceAtLeast(1)
                        val emi = if (r > 0 && p > 0) {
                            val factor = Math.pow(1 + r, n.toDouble())
                            (p * r * factor) / (factor - 1.0)
                        } else if (p > 0) p / n else 0.0
                        val totalPayable = emi * n
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Business Loan / EMI Calculator", style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(value = principalStr, onValueChange = { principalStr = it }, label = { Text("Loan Principal ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = rateStr, onValueChange = { rateStr = it }, label = { Text("Interest % p.a.") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = tenureStr, onValueChange = { tenureStr = it }, label = { Text("Months") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                                }
                                Text("Monthly EMI: ${FormatUtils.inr(emi)}  •  Total Interest: ${FormatUtils.inr((totalPayable - p).coerceAtLeast(0.0))}", style = MaterialTheme.typography.titleSmall, color = EmeraldAccent)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddRec) {
        var name by remember { mutableStateOf("") }
        var amt by remember { mutableStateOf("") }
        var freq by remember { mutableStateOf("monthly") }
        ModalBottomSheet(onDismissRequest = { showAddRec = false }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("New Recurring Billing", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Client Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amt, onValueChange = { amt = it }, label = { Text("Amount ₹ *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("weekly", "monthly", "quarterly", "yearly").forEach { f ->
                        FilterChip(selected = freq == f, onClick = { freq = f }, label = { Text(f) })
                    }
                }
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveRecurring(ErpRecurringInvoice(clientName = name.trim(), frequency = freq, nextRunDate = FormatUtils.todayStr(), totalAmount = amt.toDoubleOrNull() ?: 0.0))
                            showAddRec = false
                        }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Save Schedule") }
            }
        }
    }

    if (showAddWarranty) {
        var prod by remember { mutableStateOf("") }
        var client by remember { mutableStateOf("") }
        var serial by remember { mutableStateOf("") }
        var months by remember { mutableStateOf("12") }
        ModalBottomSheet(onDismissRequest = { showAddWarranty = false }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Warranty / AMC", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = prod, onValueChange = { prod = it }, label = { Text("Product *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = client, onValueChange = { client = it }, label = { Text("Client *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = serial, onValueChange = { serial = it }, label = { Text("Serial #") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = months, onValueChange = { months = it }, label = { Text("Months") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                Button(
                    onClick = {
                        if (prod.isNotBlank() && client.isNotBlank()) {
                            onSaveWarranty(ErpWarranty(clientName = client.trim(), productName = prod.trim(), serialNo = serial.trim(), startDate = FormatUtils.todayStr(), months = months.toLongOrNull() ?: 12L))
                            showAddWarranty = false
                        }
                    },
                    enabled = prod.isNotBlank() && client.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Save Record") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteStoreScreen(
    state: ErpUiState,
    onSaveSettings: (BusinessSettings) -> Unit,
    onSaveProduct: (ErpProduct) -> Unit,
    onSaveCoupon: (ErpCoupon) -> Unit,
    onSaveOrder: (ErpOnlineOrder) -> Unit,
    onConvertOrderToInvoice: (ErpInvoice) -> Unit = {}
) {
    var tab by remember { mutableStateOf("setup") }
    var storeEnabled by remember(state.settings) { mutableStateOf(state.settings.storeEnabled) }
    var slug by remember(state.settings) { mutableStateOf(state.settings.storeSlug) }
    var tagline by remember(state.settings) { mutableStateOf(state.settings.storeTagline) }
    var waPhone by remember(state.settings) { mutableStateOf(state.settings.whatsappOrderPhone) }
    var showAddCoupon by remember { mutableStateOf(false) }
    var showAddOrder by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("setup" to "Store Setup", "catalog" to "Catalog", "coupons" to "Coupons", "orders" to "Orders (${state.onlineOrders.size})")) { (k, lbl) ->
                        FilterChip(selected = tab == k, onClick = { tab = k }, label = { Text(lbl) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White))
                    }
                }
            }

            when (tab) {
                "setup" -> {
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Online Storefront Active", style = MaterialTheme.typography.titleMedium)
                                    Switch(checked = storeEnabled, onCheckedChange = { storeEnabled = it })
                                }
                                OutlinedTextField(value = slug, onValueChange = { slug = it }, label = { Text("Store Slug") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Store Tagline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = waPhone, onValueChange = { waPhone = it }, label = { Text("WhatsApp Order Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                                Button(
                                    onClick = { onSaveSettings(state.settings.copy(storeEnabled = storeEnabled, storeSlug = slug.trim(), storeTagline = tagline.trim(), whatsappOrderPhone = waPhone.trim())) },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) { Text("Save Store Settings") }
                                val context = LocalContext.current
                                OutlinedButton(
                                    onClick = {
                                        val storeProds = state.products.filter { !it.isDeleted && it.showInStore }.take(10)
                                        val catalogMsg = buildString {
                                            appendLine("*${state.settings.businessName}* — ${tagline.ifBlank { "Online Catalog" }}")
                                            storeProds.forEach { appendLine("• ${it.name}: ${FormatUtils.inr(it.sellingPrice)}") }
                                            if (waPhone.isNotBlank()) appendLine("Order on WhatsApp: $waPhone")
                                        }
                                        FormatUtils.shareText(context, "${state.settings.businessName} Catalog", catalogMsg)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Share Store Catalog")
                                }
                            }
                        }
                    }
                }
                "catalog" -> {
                    items(state.products.filter { !it.isDeleted }, key = { it.id }) { prod ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(prod.name, style = MaterialTheme.typography.titleSmall)
                                    Text(FormatUtils.inr(prod.sellingPrice), style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(checked = prod.showInStore, onCheckedChange = { onSaveProduct(prod.copy(showInStore = it)) })
                            }
                        }
                    }
                }
                "coupons" -> {
                    item {
                        Button(onClick = { showAddCoupon = true }, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Create Coupon")
                        }
                    }
                    items(state.coupons, key = { it.id }) { c ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(c.code, style = MaterialTheme.typography.titleMedium, color = TealPrimary)
                                    Text("${c.discountPercent.toInt()}% OFF", style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(checked = c.isActive, onCheckedChange = { onSaveCoupon(c.copy(isActive = it)) })
                            }
                        }
                    }
                }
                "orders" -> {
                    item {
                        Button(onClick = { showAddOrder = true }, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Record WhatsApp / Store Order")
                        }
                    }
                    items(state.onlineOrders, key = { it.id }) { ord ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(ord.customerName, style = MaterialTheme.typography.titleMedium)
                                    Text(FormatUtils.inr(ord.totalAmount), style = MaterialTheme.typography.titleSmall, color = TealPrimary)
                                }
                                Text("${ord.itemsSummary}\n${ord.customerPhone}", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    listOf("accepted", "completed", "cancelled").forEach { st ->
                                        FilterChip(selected = ord.status == st, onClick = { onSaveOrder(ord.copy(status = st)) }, label = { Text(st.uppercase()) })
                                    }
                                    Spacer(Modifier.weight(1f))
                                    if (ord.status != "cancelled") {
                                        OutlinedButton(onClick = {
                                            val invNo = "${state.settings.invoicePrefix}-${state.settings.nextInvoiceNumber.toString().padStart(3, '0')}"
                                            onConvertOrderToInvoice(
                                                ErpInvoice(
                                                    invoiceNumber = invNo,
                                                    clientName = ord.customerName,
                                                    clientPhone = ord.customerPhone,
                                                    dateStr = FormatUtils.todayStr(),
                                                    subtotal = ord.totalAmount,
                                                    grandTotal = ord.totalAmount,
                                                    paidAmount = ord.totalAmount,
                                                    paymentMode = "upi",
                                                    notes = "Online Order: ${ord.itemsSummary}",
                                                    items = listOf(InvoiceLineItem(name = ord.itemsSummary.ifBlank { "Store Order" }, qty = 1.0, rate = ord.totalAmount, gstPercent = 0.0))
                                                )
                                            )
                                            onSaveOrder(ord.copy(status = "completed"))
                                        }) { Text("Bill") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCoupon) {
        var code by remember { mutableStateOf("") }
        var pct by remember { mutableStateOf("10") }
        ModalBottomSheet(onDismissRequest = { showAddCoupon = false }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("New Store Coupon", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("Coupon Code *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pct, onValueChange = { pct = it }, label = { Text("Discount %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        if (code.isNotBlank()) {
                            onSaveCoupon(ErpCoupon(code = code.trim(), discountPercent = pct.toDoubleOrNull() ?: 10.0))
                            showAddCoupon = false
                        }
                    },
                    enabled = code.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Save Coupon") }
            }
        }
    }

    if (showAddOrder) {
        var cName by remember { mutableStateOf("") }
        var cPhone by remember { mutableStateOf("") }
        var itemsSummary by remember { mutableStateOf("") }
        var amtStr by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showAddOrder = false }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Record Store / WhatsApp Order", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = cName, onValueChange = { cName = it }, label = { Text("Customer Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cPhone, onValueChange = { cPhone = it }, label = { Text("Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = itemsSummary, onValueChange = { itemsSummary = it }, label = { Text("Items Ordered *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amtStr, onValueChange = { amtStr = it }, label = { Text("Order Total ₹ *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        if (cName.isNotBlank() && itemsSummary.isNotBlank()) {
                            onSaveOrder(
                                ErpOnlineOrder(
                                    customerName = cName.trim(),
                                    customerPhone = cPhone.trim(),
                                    itemsSummary = itemsSummary.trim(),
                                    totalAmount = amtStr.toDoubleOrNull() ?: 0.0,
                                    status = "accepted"
                                )
                            )
                            showAddOrder = false
                        }
                    },
                    enabled = cName.isNotBlank() && itemsSummary.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Save Order") }
            }
        }
    }
}
