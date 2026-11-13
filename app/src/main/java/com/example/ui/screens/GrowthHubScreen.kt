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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.FormatUtils
import com.example.data.model.ErpPurchaseOrder
import com.example.data.model.PoLineItem
import com.example.ui.components.ErpStatusBadge
import com.example.ui.components.UpiPaymentQrCard
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoft
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoft
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoft
import com.example.ui.viewmodel.ErpUiState

@Composable
fun GrowthHubScreen(
    state: ErpUiState,
    onCreateAutoPo: (ErpPurchaseOrder) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("upi") }

    val tabs = listOf(
        "upi" to "UPI Collect",
        "cash" to "Cash Register",
        "sim" to "Break-Even & Valuation",
        "abc" to "Inventory ABC",
        "chaser" to "Overdue Chaser"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tabs) { (key, label) ->
                        FilterChip(
                            selected = activeTab == key,
                            onClick = { activeTab = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White),
                            modifier = Modifier.testTag("growth_tab_$key")
                        )
                    }
                }
            }

            when (activeTab) {
                "upi" -> {
                    item {
                        var upiAmtStr by remember { mutableStateOf("500") }
                        var upiNote by remember { mutableStateOf("Invoice Payment") }
                        val amt = upiAmtStr.toDoubleOrNull() ?: 0.0
                        val unpaid = state.invoices.filter { !it.isDeleted && it.status == "final" && it.paymentStatus != "paid" }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Dynamic UPI Payment QR", style = MaterialTheme.typography.titleMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = upiAmtStr,
                                            onValueChange = { upiAmtStr = it },
                                            label = { Text("Amount ₹") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = upiNote,
                                            onValueChange = { upiNote = it },
                                            label = { Text("Payment Note") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1.4f)
                                        )
                                    }
                                    UpiPaymentQrCard(
                                        upiId = state.settings.upiId,
                                        payeeName = state.settings.businessName,
                                        amount = amt,
                                        note = upiNote,
                                        onResultMessage = onMessage
                                    )
                                }
                            }

                            if (unpaid.isNotEmpty()) {
                                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Tap Unpaid Bill to Pre-Fill QR", style = MaterialTheme.typography.titleSmall)
                                        unpaid.take(6).forEach { inv ->
                                            val due = (inv.grandTotal - inv.paidAmount).coerceAtLeast(0.0)
                                            OutlinedButton(
                                                onClick = {
                                                    upiAmtStr = due.toLong().toString()
                                                    upiNote = "${inv.invoiceNumber} ${inv.clientName}".trim()
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("${inv.invoiceNumber} • ${inv.clientName.ifBlank { "Walk-in" }} — ${FormatUtils.inr(due)}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "cash" -> {
                    item {
                        val today = FormatUtils.todayStr()
                        val todayCashIn = remember(state.invoices, today) {
                            state.invoices.filter { !it.isDeleted && it.status == "final" && it.dateStr == today && it.paymentMode == "cash" }
                                .sumOf { it.paidAmount }
                        }
                        val todayCashOut = remember(state.expenses, today) {
                            state.expenses.filter { !it.isDeleted && it.approvalStatus != "rejected" && it.dateStr == today && it.paymentMode == "cash" }
                                .sumOf { it.amount }
                        }
                        var openingStr by remember { mutableStateOf("2000") }
                        var countedStr by remember { mutableStateOf("") }
                        val opening = openingStr.toDoubleOrNull() ?: 0.0
                        val expected = opening + todayCashIn - todayCashOut
                        val counted = countedStr.toDoubleOrNull()
                        val variance = if (counted != null) counted - expected else null

                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Daily Cash Drawer Reconciliation ($today)", style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(
                                    value = openingStr,
                                    onValueChange = { openingStr = it },
                                    label = { Text("Opening Cash Float ₹") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                HubMetricRow("Today Cash Sales (+)", FormatUtils.inr(todayCashIn), SuccessGreen)
                                HubMetricRow("Today Cash Expenses (-)", FormatUtils.inr(todayCashOut), DangerRed)
                                HorizontalDivider()
                                HubMetricRow("Expected Cash in Drawer", FormatUtils.inr(expected), TealPrimary)

                                OutlinedTextField(
                                    value = countedStr,
                                    onValueChange = { countedStr = it },
                                    label = { Text("Physical Cash Counted ₹") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (variance != null) {
                                    val ok = kotlin.math.abs(variance) < 1.0
                                    ErpStatusBadge(
                                        label = when {
                                            ok -> "DRAWER MATCHED"
                                            variance > 0 -> "EXCESS +${FormatUtils.inr(variance)}"
                                            else -> "SHORT ${FormatUtils.inr(variance)}"
                                        },
                                        containerColor = if (ok) SuccessSoft else if (variance > 0) WarningSoft else DangerSoft,
                                        contentColor = if (ok) SuccessGreen else if (variance > 0) WarningAmber else DangerRed
                                    )
                                }
                            }
                        }
                    }
                }

                "sim" -> {
                    item {
                        var fixedStr by remember { mutableStateOf("35000") }
                        var sellPriceStr by remember { mutableStateOf("500") }
                        var varCostStr by remember { mutableStateOf("300") }
                        val fixed = fixedStr.toDoubleOrNull() ?: 0.0
                        val unitSell = sellPriceStr.toDoubleOrNull() ?: 0.0
                        val unitVar = varCostStr.toDoubleOrNull() ?: 0.0
                        val contrib = (unitSell - unitVar).coerceAtLeast(0.0)
                        val bepUnits = if (contrib > 0) kotlin.math.ceil(fixed / contrib).toLong() else 0L
                        val bepRevenue = bepUnits * unitSell

                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Break-Even Point Calculator", style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(value = fixedStr, onValueChange = { fixedStr = it }, label = { Text("Monthly Fixed Overhead ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = sellPriceStr, onValueChange = { sellPriceStr = it }, label = { Text("Avg Unit Price ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = varCostStr, onValueChange = { varCostStr = it }, label = { Text("Avg Unit Cost ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                                }
                                HubMetricRow("Contribution / Unit", FormatUtils.inr(contrib), TealPrimary)
                                HubMetricRow("Break-Even Units / Month", "$bepUnits units", IndigoAccent)
                                HubMetricRow("Break-Even Revenue", FormatUtils.inr(bepRevenue), EmeraldAccent)
                            }
                        }
                    }

                    item {
                        var baseMarginPctStr by remember { mutableStateOf("25") }
                        var discountPct by remember { mutableStateOf(10.0) }
                        val baseMargin = baseMarginPctStr.toDoubleOrNull()?.coerceIn(1.0, 95.0) ?: 25.0
                        val newMargin = baseMargin - discountPct
                        val extraVolumePct = if (newMargin > 0.0) ((baseMargin / newMargin) - 1.0) * 100.0 else 999.0

                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Discount Margin Guard Simulator", style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(value = baseMarginPctStr, onValueChange = { baseMarginPctStr = it }, label = { Text("Your Gross Margin %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(5.0, 10.0, 15.0, 20.0).forEach { d ->
                                        FilterChip(selected = discountPct == d, onClick = { discountPct = d }, label = { Text("${d.toInt()}% Off") })
                                    }
                                }
                                Text(
                                    text = if (newMargin <= 0.0) "Warning: ${discountPct.toInt()}% discount wipes out your entire margin!"
                                    else "A ${discountPct.toInt()}% discount requires ${extraVolumePct.toInt()}% MORE sales volume just to make the same profit!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (newMargin <= 0.0 || extraVolumePct >= 50.0) DangerRed else WarningAmber
                                )
                            }
                        }
                    }

                    item {
                        val stockVal = state.products.filter { !it.isDeleted && !it.isService }.sumOf { it.stock * it.costPrice }
                        val annualizedProfit = (state.stats.netProfit * 12.0).coerceAtLeast(0.0)
                        val estVal = annualizedProfit * 2.5 + stockVal + state.stats.unpaidAmount
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Estimated Business Valuation", style = MaterialTheme.typography.titleMedium)
                                HubMetricRow("2.5x Annualized Profit", FormatUtils.inr(annualizedProfit * 2.5), TealPrimary)
                                HubMetricRow("Inventory at Cost", FormatUtils.inr(stockVal), IndigoAccent)
                                HubMetricRow("Receivables Book", FormatUtils.inr(state.stats.unpaidAmount), WarningAmber)
                                HorizontalDivider()
                                HubMetricRow("Indicative Valuation", FormatUtils.inr(estVal), EmeraldAccent)
                            }
                        }
                    }
                }

                "abc" -> {
                    item {
                        val revByItem = remember(state.invoices) {
                            state.invoices.filter { !it.isDeleted && it.status == "final" && it.docType == "invoice" }
                                .flatMap { it.items }
                                .groupBy { it.name }
                                .mapValues { (_, list) -> list.sumOf { it.qty * it.rate } }
                                .entries.sortedByDescending { it.value }
                        }
                        val totalItemRev = revByItem.sumOf { it.value }.coerceAtLeast(1.0)
                        var cum = 0.0
                        val classified = revByItem.map { (name, amt) ->
                            cum += amt
                            val pct = (cum / totalItemRev) * 100.0
                            val cls = when {
                                pct <= 70.0 -> "A"
                                pct <= 90.0 -> "B"
                                else -> "C"
                            }
                            Triple(name, amt, cls)
                        }

                        val lowStock = state.products.filter { !it.isDeleted && !it.isService && it.stock <= it.lowStockThreshold }
                        val restockCost = lowStock.sumOf {
                            val need = (it.lowStockThreshold * 2.0 - it.stock).coerceAtLeast(5.0)
                            val unitCost = it.costPrice.takeIf { c -> c > 0 } ?: (it.sellingPrice * 0.7)
                            need * unitCost
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Restock Capital Required", style = MaterialTheme.typography.titleMedium)
                                    HubMetricRow("${lowStock.size} Low-Stock SKUs", FormatUtils.inr(restockCost), DangerRed)
                                    Button(
                                        onClick = {
                                            val poItems = lowStock.map {
                                                PoLineItem(it.id, it.name, (it.lowStockThreshold * 2 - it.stock).coerceAtLeast(5.0), it.costPrice.takeIf { c -> c > 0 } ?: it.sellingPrice * 0.7)
                                            }
                                            onCreateAutoPo(
                                                ErpPurchaseOrder(
                                                    poNumber = "PO-${(state.purchaseOrders.size + 1).toString().padStart(3, '0')}",
                                                    vendorName = "Primary Restock Supplier",
                                                    dateStr = FormatUtils.todayStr(),
                                                    status = "draft",
                                                    items = poItems,
                                                    totalAmount = restockCost
                                                )
                                            )
                                        },
                                        enabled = lowStock.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Draft Restock PO")
                                    }
                                }
                            }

                            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("ABC Inventory Velocity", style = MaterialTheme.typography.titleMedium)
                                    if (classified.isEmpty()) {
                                        Text("Finalize sales invoices to unlock ABC item velocity.", style = MaterialTheme.typography.bodySmall)
                                    } else {
                                        classified.take(12).forEach { (name, amt, cls) ->
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(FormatUtils.inr(amt), style = MaterialTheme.typography.titleSmall)
                                                    ErpStatusBadge(
                                                        "Class $cls",
                                                        when (cls) {
                                                            "A" -> SuccessSoft
                                                            "B" -> WarningSoft
                                                            else -> DangerSoft
                                                        },
                                                        when (cls) {
                                                            "A" -> SuccessGreen
                                                            "B" -> WarningAmber
                                                            else -> DangerRed
                                                        }
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

                "chaser" -> {
                    val unpaid = state.invoices
                        .filter { !it.isDeleted && it.status == "final" && it.paymentStatus != "paid" }
                        .sortedByDescending { it.grandTotal - it.paidAmount }
                    if (unpaid.isEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Text("All invoices are paid — zero overdue follow-ups needed!", modifier = Modifier.padding(16.dp))
                            }
                        }
                    } else {
                        items(unpaid, key = { it.id }) { inv ->
                            val due = (inv.grandTotal - inv.paidAmount).coerceAtLeast(0.0)
                            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text(inv.clientName.ifBlank { "Walk-in" }, style = MaterialTheme.typography.titleMedium)
                                            Text("#${inv.invoiceNumber} • Date ${inv.dateStr}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(FormatUtils.inr(due), style = MaterialTheme.typography.titleMedium, color = DangerRed)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(
                                            "Polite" to "Hi ${inv.clientName}, gentle reminder for Invoice #${inv.invoiceNumber} (${FormatUtils.inr(due)}). Thank you! — ${state.settings.businessName}",
                                            "Firm" to "Dear ${inv.clientName}, payment of ${FormatUtils.inr(due)} for #${inv.invoiceNumber} is pending. Kindly settle at the earliest. — ${state.settings.businessName}",
                                            "Urgent" to "URGENT: Invoice #${inv.invoiceNumber} (${FormatUtils.inr(due)}) is overdue. Please remit via UPI (${state.settings.upiId.ifBlank { "bank" }}) today. — ${state.settings.businessName}"
                                        ).forEach { (tone, msg) ->
                                            OutlinedButton(
                                                onClick = { FormatUtils.shareText(context, "Payment Reminder ($tone)", msg) },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                                                Spacer(Modifier.width(4.dp))
                                                Text(tone)
                                            }
                                        }
                                    }
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
private fun HubMetricRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor)
    }
}
