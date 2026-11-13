package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.FormatUtils
import com.example.data.model.ErpClient
import com.example.data.model.ErpInvoice
import com.example.data.model.InvoiceLineItem
import com.example.data.model.InvoiceTemplates
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.ErpUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InvoiceBuilderScreen(
    state: ErpUiState,
    editingInvoice: ErpInvoice?,
    defaultTerms: String = "",
    paperSize: String = "a4",
    onQuickAddClient: (ErpClient) -> Unit = {},
    onSaveInvoice: (ErpInvoice, Boolean, Boolean) -> Unit,
    onPrintMessage: (String, Boolean) -> Unit = { _, _ -> },
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val settings = state.settings
    val defaultNumber = remember(editingInvoice, settings.invoicePrefix, settings.nextInvoiceNumber) {
        editingInvoice?.invoiceNumber?.ifBlank { null }
            ?: "${settings.invoicePrefix}-${settings.nextInvoiceNumber.toString().padStart(3, '0')}"
    }

    var invoiceNumber by remember(editingInvoice) { mutableStateOf(defaultNumber) }
    var docType by remember(editingInvoice) { mutableStateOf(editingInvoice?.docType ?: "invoice") }
    var dateStr by remember(editingInvoice) { mutableStateOf(editingInvoice?.dateStr?.ifBlank { FormatUtils.todayStr() } ?: FormatUtils.todayStr()) }
    var dueDateStr by remember(editingInvoice) { mutableStateOf(editingInvoice?.dueDateStr ?: "") }
    var branchId by remember(editingInvoice) { mutableStateOf(editingInvoice?.branchId ?: "") }
    var clientId by remember(editingInvoice) { mutableStateOf(editingInvoice?.clientId ?: "") }
    var clientName by remember(editingInvoice) { mutableStateOf(editingInvoice?.clientName ?: "") }
    var clientPhone by remember(editingInvoice) { mutableStateOf(editingInvoice?.clientPhone ?: "") }
    var clientGstin by remember(editingInvoice) { mutableStateOf(editingInvoice?.clientGstin ?: "") }
    var clientAddress by remember(editingInvoice) { mutableStateOf(editingInvoice?.clientAddress ?: "") }
    var gstMode by remember(editingInvoice) { mutableStateOf(editingInvoice?.gstMode ?: "intra") }
    var gstInclusive by remember(editingInvoice) { mutableStateOf(editingInvoice?.gstInclusive ?: settings.gstInclusive) }
    var reverseCharge by remember(editingInvoice) { mutableStateOf(editingInvoice?.reverseCharge ?: false) }
    var paymentMode by remember(editingInvoice) { mutableStateOf(editingInvoice?.paymentMode ?: "upi") }
    var paymentStatus by remember(editingInvoice) { mutableStateOf(editingInvoice?.paymentStatus ?: "paid") }
    var partialPaidStr by remember(editingInvoice) { mutableStateOf(editingInvoice?.paidAmount?.takeIf { it > 0 }?.toString() ?: "") }
    var discountStr by remember(editingInvoice) { mutableStateOf(editingInvoice?.discountAmount?.takeIf { it > 0 }?.toString() ?: "") }
    var tdsStr by remember(editingInvoice) { mutableStateOf(editingInvoice?.tdsAmount?.takeIf { it > 0 }?.toString() ?: "") }
    var shippingStr by remember(editingInvoice) { mutableStateOf(editingInvoice?.shippingAmount?.takeIf { it > 0 }?.toString() ?: "") }
    var upiSplitStr by remember(editingInvoice) { mutableStateOf(editingInvoice?.upiSplit?.takeIf { it > 0 }?.toString() ?: "") }
    var cashSplitStr by remember(editingInvoice) { mutableStateOf(editingInvoice?.cashSplit?.takeIf { it > 0 }?.toString() ?: "") }
    var creditSplitStr by remember(editingInvoice) { mutableStateOf(editingInvoice?.creditSplit?.takeIf { it > 0 }?.toString() ?: "") }
    var notes by remember(editingInvoice, defaultTerms) { mutableStateOf(editingInvoice?.notes ?: defaultTerms) }
    var templateId by remember(editingInvoice) { mutableStateOf(editingInvoice?.templateId?.ifBlank { settings.invoiceTemplate } ?: settings.invoiceTemplate) }

    val lineItems = remember(editingInvoice) {
        mutableStateListOf<InvoiceLineItem>().apply {
            if (editingInvoice != null && editingInvoice.items.isNotEmpty()) addAll(editingInvoice.items)
        }
    }

    var showClientPicker by remember { mutableStateOf(false) }
    var showCatalogPicker by remember { mutableStateOf(false) }
    var showLineEditor by remember { mutableStateOf(false) }
    var editingLineIndex by remember { mutableIntStateOf(-1) }
    var showPreviewSheet by remember { mutableStateOf(false) }

    // Tax & total calculations
    val rawSum = lineItems.sumOf { it.qty * it.rate }
    val totalGst = lineItems.sumOf { item ->
        val gross = item.qty * item.rate
        if (gstInclusive && item.gstPercent > 0) {
            gross - (gross / (1.0 + item.gstPercent / 100.0))
        } else {
            gross * (item.gstPercent / 100.0)
        }
    }
    val subtotal = if (gstInclusive) (rawSum - totalGst) else rawSum
    val cgst = if (gstMode == "intra") totalGst / 2.0 else 0.0
    val sgst = if (gstMode == "intra") totalGst / 2.0 else 0.0
    val igst = if (gstMode == "inter") totalGst else 0.0
    val discount = discountStr.toDoubleOrNull() ?: 0.0
    val tds = tdsStr.toDoubleOrNull() ?: 0.0
    val shipping = shippingStr.toDoubleOrNull() ?: 0.0
    val grandTotal = (subtotal + cgst + sgst + igst + shipping - discount - tds).coerceAtLeast(0.0)

    fun buildInvoice(status: String): ErpInvoice {
        val upiSplit = upiSplitStr.toDoubleOrNull() ?: 0.0
        val cashSplit = cashSplitStr.toDoubleOrNull() ?: 0.0
        val creditSplit = creditSplitStr.toDoubleOrNull() ?: 0.0
        val paidAmt = when {
            paymentMode == "credit" -> 0.0
            paymentMode == "split" -> (upiSplit + cashSplit).coerceAtMost(grandTotal)
            paymentStatus == "paid" -> grandTotal
            paymentStatus == "partial" -> (partialPaidStr.toDoubleOrNull() ?: (grandTotal * 0.5)).coerceIn(0.0, grandTotal)
            else -> 0.0
        }
        return ErpInvoice(
            id = editingInvoice?.id ?: "",
            userId = editingInvoice?.userId ?: "",
            invoiceNumber = invoiceNumber.ifBlank { defaultNumber },
            docType = docType,
            status = status,
            paymentStatus = if (paymentMode == "credit") "unpaid" else paymentStatus,
            paymentMode = paymentMode,
            clientId = clientId,
            clientName = clientName.ifBlank { "Walk-in Customer" },
            clientPhone = clientPhone,
            clientGstin = clientGstin,
            clientAddress = clientAddress,
            dateStr = dateStr,
            dueDateStr = dueDateStr,
            gstMode = gstMode,
            gstInclusive = gstInclusive,
            reverseCharge = reverseCharge,
            subtotal = subtotal,
            discountAmount = discount,
            tdsAmount = tds,
            shippingAmount = shipping,
            cgstTotal = cgst,
            sgstTotal = sgst,
            igstTotal = igst,
            grandTotal = grandTotal,
            paidAmount = paidAmt,
            upiSplit = upiSplit,
            cashSplit = cashSplit,
            creditSplit = creditSplit,
            notes = notes,
            templateId = templateId,
            branchId = branchId,
            items = lineItems.toList(),
            createdAt = editingInvoice?.createdAt
        )
    }

    fun applySelectedClient(cli: ErpClient) {
        clientId = cli.id
        clientName = cli.name
        clientPhone = cli.phone
        clientGstin = cli.gstin
        clientAddress = cli.address
        val bizPrefix = settings.gstin.trim().take(2)
        val cliPrefix = cli.gstin.trim().take(2)
        if (bizPrefix.length == 2 && cliPrefix.length == 2) {
            gstMode = if (bizPrefix == cliPrefix) "intra" else "inter"
        } else if (settings.state.isNotBlank() && cli.state.isNotBlank()) {
            gstMode = if (settings.state.equals(cli.state, ignoreCase = true)) "intra" else "inter"
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Doc Type Selector
            item {
                Text("Document Type", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val types = listOf(
                        "invoice" to "Tax Invoice",
                        "quotation" to "Quotation",
                        "proforma" to "Proforma",
                        "purchase" to "Purchase Bill",
                        "credit_note" to "Credit Note",
                        "challan" to "Delivery Challan"
                    )
                    items(types) { (key, label) ->
                        FilterChip(
                            selected = docType == key,
                            onClick = { docType = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White),
                            modifier = Modifier.testTag("doc_type_$key")
                        )
                    }
                }
            }

            // Number, Dates, Branch & Customer Selection
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = invoiceNumber,
                                onValueChange = { invoiceNumber = it },
                                label = { Text("Doc #") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("invoice_number_input")
                            )
                            OutlinedTextField(
                                value = dateStr,
                                onValueChange = { dateStr = it },
                                label = { Text("Date") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = dueDateStr,
                                onValueChange = { dueDateStr = it },
                                label = { Text("Due Date") },
                                placeholder = { Text("YYYY-MM-DD") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(7 to "+7d", 15 to "+15d", 30 to "+30d").forEach { (days, lbl) ->
                                    AssistChip(
                                        onClick = { dueDateStr = FormatUtils.addDaysToDateStr(dateStr, days) },
                                        label = { Text(lbl) }
                                    )
                                }
                            }
                        }

                        val activeBranches = state.branches.filter { it.isActive }
                        if (activeBranches.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    FilterChip(selected = branchId.isBlank(), onClick = { branchId = "" }, label = { Text("Main Branch") })
                                }
                                items(activeBranches) { br ->
                                    FilterChip(selected = branchId == br.id, onClick = { branchId = br.id }, label = { Text(br.name) })
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = clientName,
                                onValueChange = { clientName = it },
                                label = { Text("Customer / Party Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("invoice_client_input")
                            )
                            OutlinedButton(
                                onClick = { showClientPicker = true },
                                modifier = Modifier.height(54.dp).testTag("invoice_pick_client_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Pick Customer")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = clientPhone,
                                onValueChange = { clientPhone = it },
                                label = { Text("Phone") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = clientGstin,
                                onValueChange = { clientGstin = it.uppercase() },
                                label = { Text("Client GSTIN") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = clientAddress,
                            onValueChange = { clientAddress = it },
                            label = { Text("Billing Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Line Items
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Items (${lineItems.size})", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = {
                                        editingLineIndex = -1
                                        showLineEditor = true
                                    },
                                    modifier = Modifier.testTag("invoice_add_custom_item_button")
                                ) { Text("+ Custom") }
                                Button(
                                    onClick = { showCatalogPicker = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    modifier = Modifier.testTag("invoice_add_catalog_item_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Catalog")
                                }
                            }
                        }

                        if (lineItems.isEmpty()) {
                            Text(
                                text = "Add products or services from your catalog or tap + Custom.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            lineItems.forEachIndexed { idx, item ->
                                val gross = item.qty * item.rate
                                val taxable = if (gstInclusive && item.gstPercent > 0) gross / (1.0 + item.gstPercent / 100.0) else gross
                                val taxAmt = if (gstInclusive && item.gstPercent > 0) gross - taxable else taxable * (item.gstPercent / 100.0)
                                val lineTotal = taxable + taxAmt

                                val matchedProd = state.products.firstOrNull {
                                    !it.isDeleted && ((item.productId.isNotBlank() && it.id == item.productId) ||
                                        (!it.isService && it.name.equals(item.name, ignoreCase = true)))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editingLineIndex = idx
                                            showLineEditor = true
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Line", modifier = Modifier.size(13.dp), tint = TealPrimary)
                                        }
                                        Text(
                                            text = "${item.qty} × ${FormatUtils.inr(item.rate)} • Tax ${FormatUtils.inr(taxAmt)} (${item.gstPercent.toInt()}%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Line Total: ${FormatUtils.inr(lineTotal)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TealPrimary
                                            )
                                            if (docType == "invoice" && matchedProd != null && !matchedProd.isService) {
                                                val exceeds = item.qty > matchedProd.stock
                                                val after = (matchedProd.stock - item.qty).coerceAtLeast(0.0).toInt()
                                                Text(
                                                    text = if (exceeds) "⚠ Only ${matchedProd.stock.toInt()} left" else "• After bill: $after ${matchedProd.unit}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (exceeds) DangerRed else EmeraldAccent
                                                )
                                            }
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            if (item.qty > 1) lineItems[idx] = item.copy(qty = item.qty - 1)
                                        }) { Text("-", fontWeight = FontWeight.Bold) }
                                        Text("${item.qty.toInt()}", style = MaterialTheme.typography.titleSmall)
                                        IconButton(onClick = {
                                            lineItems[idx] = item.copy(qty = item.qty + 1)
                                        }) { Text("+", fontWeight = FontWeight.Bold) }
                                        IconButton(onClick = { lineItems.removeAt(idx) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove Item", tint = DangerRed)
                                        }
                                    }
                                }
                                if (idx < lineItems.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // GST & Adjustments
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("GST & Charges", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = gstMode == "intra", onClick = { gstMode = "intra" }, label = { Text("Intra-State (CGST+SGST)") })
                            FilterChip(selected = gstMode == "inter", onClick = { gstMode = "inter" }, label = { Text("Inter-State (IGST)") })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Prices include GST (Inclusive)", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = gstInclusive, onCheckedChange = { gstInclusive = it })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Reverse Charge Applicable", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = reverseCharge, onCheckedChange = { reverseCharge = it })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = discountStr, onValueChange = { discountStr = it }, label = { Text("Discount ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = tdsStr, onValueChange = { tdsStr = it }, label = { Text("TDS ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = shippingStr, onValueChange = { shippingStr = it }, label = { Text("Shipping ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Payment Mode + Split + Templates
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Payment & Invoice Template", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("upi" to "UPI", "cash" to "Cash", "credit" to "Credit", "bank" to "Bank", "split" to "Split").forEach { (m, lbl) ->
                                FilterChip(
                                    selected = paymentMode == m,
                                    onClick = {
                                        paymentMode = m
                                        if (m == "credit") paymentStatus = "unpaid"
                                    },
                                    label = { Text(lbl) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
                                )
                            }
                        }

                        if (paymentMode == "split") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = upiSplitStr, onValueChange = { upiSplitStr = it }, label = { Text("UPI ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = cashSplitStr, onValueChange = { cashSplitStr = it }, label = { Text("Cash ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = creditSplitStr, onValueChange = { creditSplitStr = it }, label = { Text("Credit ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                            }
                        } else if (paymentMode != "credit") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("paid" to "Paid", "partial" to "Partial", "unpaid" to "Unpaid").forEach { (st, lbl) ->
                                    FilterChip(selected = paymentStatus == st, onClick = { paymentStatus = st }, label = { Text(lbl) })
                                }
                            }
                            if (paymentStatus == "partial") {
                                OutlinedTextField(
                                    value = partialPaidStr,
                                    onValueChange = { partialPaidStr = it },
                                    label = { Text("Amount Paid Now ₹ (Default 50%)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Text("Choose Template (${InvoiceTemplates.all.size})", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(InvoiceTemplates.all) { tpl ->
                                val selected = tpl.id == templateId
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(if (selected) 2.dp else 1.dp, if (selected) TealPrimary else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                        .clickable { templateId = tpl.id }
                                        .padding(8.dp)
                                ) {
                                    Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(tpl.primaryColor))
                                    Spacer(Modifier.height(4.dp))
                                    Text(tpl.name.split(" ").first(), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes / Terms & Conditions") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Summary + Save & Print Actions
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Taxable Subtotal")
                            Text(FormatUtils.inr(subtotal))
                        }
                        if (gstMode == "inter") {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("IGST")
                                Text(FormatUtils.inr(igst))
                            }
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CGST + SGST")
                                Text("${FormatUtils.inr(cgst)} + ${FormatUtils.inr(sgst)}")
                            }
                        }
                        if (discount > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount")
                                Text("-${FormatUtils.inr(discount)}")
                            }
                        }
                        if (shipping > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Shipping")
                                Text(FormatUtils.inr(shipping))
                            }
                        }
                        if (tds > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TDS Deducted")
                                Text("-${FormatUtils.inr(tds)}")
                            }
                        }
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total", style = MaterialTheme.typography.titleLarge, color = TealPrimary)
                            Text(FormatUtils.inr(grandTotal), style = MaterialTheme.typography.titleLarge, color = TealPrimary)
                        }
                        Text(
                            text = FormatUtils.amountInWordsInr(grandTotal),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showPreviewSheet = true },
                        enabled = lineItems.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("invoice_preview_button")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Preview")
                    }
                    OutlinedButton(
                        onClick = { onSaveInvoice(buildInvoice("draft"), false, false) },
                        enabled = lineItems.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("invoice_save_draft_button")
                    ) { Text("Save Draft") }
                }

                Spacer(Modifier.height(8.dp))

                val shouldIncrement = editingInvoice == null
                val shouldDeduct = docType == "invoice" && (editingInvoice == null || editingInvoice.status != "final" || editingInvoice.docType != "invoice")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val inv = buildInvoice("final")
                            onSaveInvoice(inv, shouldIncrement, shouldDeduct)
                            FormatUtils.printInvoice(context, inv, settings, paperSize, defaultTerms)
                                .onFailure { onPrintMessage(it.localizedMessage ?: "Print unavailable", true) }
                        },
                        enabled = lineItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier.weight(1f).height(52.dp).testTag("invoice_save_print_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Save & Print")
                    }
                    Button(
                        onClick = { onSaveInvoice(buildInvoice("final"), shouldIncrement, shouldDeduct) },
                        enabled = lineItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        modifier = Modifier.weight(1.2f).height(52.dp).testTag("invoice_finalize_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (editingInvoice == null) "Finalize Bill" else "Update Bill")
                    }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }

    if (showClientPicker) {
        ClientPickerSheet(
            clients = state.clients,
            onSelectClient = { cli ->
                applySelectedClient(cli)
                showClientPicker = false
            },
            onQuickCreateClient = { newCli ->
                onQuickAddClient(newCli)
                applySelectedClient(newCli)
            },
            onDismiss = { showClientPicker = false }
        )
    }

    if (showCatalogPicker) {
        CatalogItemPickerSheet(
            products = state.products,
            onSelectProduct = { prod ->
                val existingIdx = lineItems.indexOfFirst { it.productId == prod.id && prod.id.isNotBlank() }
                if (existingIdx >= 0) {
                    val old = lineItems[existingIdx]
                    lineItems[existingIdx] = old.copy(qty = old.qty + 1)
                } else {
                    lineItems += InvoiceLineItem(
                        productId = prod.id,
                        name = prod.name,
                        hsnSac = prod.hsnSac,
                        qty = 1.0,
                        rate = prod.sellingPrice,
                        gstPercent = prod.gstPercent,
                        isService = prod.isService
                    )
                }
                showCatalogPicker = false
            },
            onDismiss = { showCatalogPicker = false }
        )
    }

    if (showLineEditor) {
        LineItemEditorDialog(
            initial = lineItems.getOrNull(editingLineIndex),
            onSave = { item ->
                if (editingLineIndex in lineItems.indices) {
                    lineItems[editingLineIndex] = item
                } else {
                    lineItems += item
                }
                showLineEditor = false
            },
            onDismiss = { showLineEditor = false }
        )
    }

    if (showPreviewSheet) {
        val previewInv = buildInvoice("final")
        InvoicePreviewSheet(
            invoice = previewInv,
            settings = settings,
            onPrint = {
                FormatUtils.printInvoice(context, previewInv, settings, paperSize, defaultTerms)
                    .onFailure { onPrintMessage(it.localizedMessage ?: "Print failed", true) }
            },
            onExportPdf = {
                FormatUtils.exportAndShareInvoicePdf(context, previewInv, settings, paperSize, defaultTerms)
                    .onFailure { onPrintMessage(it.localizedMessage ?: "PDF export failed", true) }
            },
            onSaveAndPrint = {
                showPreviewSheet = false
                val shouldDeduct = docType == "invoice" && (editingInvoice == null || editingInvoice.status != "final" || editingInvoice.docType != "invoice")
                onSaveInvoice(previewInv, editingInvoice == null, shouldDeduct)
                FormatUtils.printInvoice(context, previewInv, settings, paperSize, defaultTerms)
                    .onFailure { onPrintMessage(it.localizedMessage ?: "Print failed", true) }
            },
            onDismiss = { showPreviewSheet = false }
        )
    }
}
