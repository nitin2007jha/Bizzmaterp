package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.FormatUtils
import com.example.data.model.BusinessSettings
import com.example.data.model.ErpClient
import com.example.data.model.ErpInvoice
import com.example.data.model.ErpProduct
import com.example.data.model.InvoiceLineItem
import com.example.data.model.InvoiceTemplates
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientPickerSheet(
    clients: List<ErpClient>,
    onSelectClient: (ErpClient) -> Unit,
    onQuickCreateClient: (ErpClient) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var showQuickAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newGstin by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var newState by remember { mutableStateOf("") }

    val filtered = remember(clients, search) {
        val q = search.trim().lowercase()
        clients.filter { !it.isDeleted }
            .filter {
                q.isBlank() ||
                    it.name.lowercase().contains(q) ||
                    it.phone.contains(q) ||
                    it.gstin.lowercase().contains(q)
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Customer", style = MaterialTheme.typography.titleLarge)
                TextButton(
                    onClick = { showQuickAdd = !showQuickAdd },
                    modifier = Modifier.testTag("client_picker_quick_add_toggle")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (showQuickAdd) "Pick Existing" else "+ Quick Add")
                }
            }

            AnimatedVisibility(visible = showQuickAdd) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Quick Create Customer", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Customer / Business Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("quick_client_name_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newPhone,
                                onValueChange = { newPhone = it },
                                label = { Text("Phone") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newGstin,
                                onValueChange = { newGstin = it.uppercase() },
                                label = { Text("GSTIN") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newState,
                                onValueChange = { newState = it },
                                label = { Text("State") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newAddress,
                                onValueChange = { newAddress = it },
                                label = { Text("Address") },
                                singleLine = true,
                                modifier = Modifier.weight(1.4f)
                            )
                        }
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    val created = ErpClient(
                                        name = newName.trim(),
                                        phone = newPhone.trim(),
                                        gstin = newGstin.trim(),
                                        state = newState.trim(),
                                        address = newAddress.trim()
                                    )
                                    onQuickCreateClient(created)
                                    onDismiss()
                                }
                            },
                            enabled = newName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            modifier = Modifier.fillMaxWidth().testTag("quick_client_save_button")
                        ) {
                            Text("Save & Select Customer")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search name, phone, GSTIN...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (filtered.isEmpty()) {
                Text(
                    text = "No matching clients found. Tap + Quick Add above to create one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id.ifBlank { it.name } }) { cli ->
                        Card(
                            onClick = { onSelectClient(cli) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cli.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        listOfNotNull(
                                            cli.phone.takeIf { it.isNotBlank() },
                                            cli.state.takeIf { it.isNotBlank() },
                                            cli.address.takeIf { it.isNotBlank() }
                                        ).joinToString(" • ").ifBlank { "No phone / address" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (cli.gstin.isNotBlank()) {
                                    Text(cli.gstin, style = MaterialTheme.typography.labelSmall, color = TealPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogItemPickerSheet(
    products: List<ErpProduct>,
    onSelectProduct: (ErpProduct) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("all") }

    val filtered = remember(products, search, typeFilter) {
        val q = search.trim().lowercase()
        products.filter { !it.isDeleted }
            .filter {
                when (typeFilter) {
                    "goods" -> !it.isService
                    "services" -> it.isService
                    else -> true
                }
            }
            .filter {
                q.isBlank() ||
                    it.name.lowercase().contains(q) ||
                    it.sku.lowercase().contains(q) ||
                    it.hsnSac.lowercase().contains(q)
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Add from Catalog", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search product, service, SKU, HSN...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All", "goods" to "Products", "services" to "Services").forEach { (k, lbl) ->
                    FilterChip(
                        selected = typeFilter == k,
                        onClick = { typeFilter = k },
                        label = { Text(lbl) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
                    )
                }
            }
            if (filtered.isEmpty()) {
                Text(
                    "No catalog items match your search.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id.ifBlank { it.name } }) { prod ->
                        Card(
                            onClick = { onSelectProduct(prod) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(prod.name, style = MaterialTheme.typography.titleSmall)
                                    val sub = if (prod.isService) {
                                        "Service • SAC: ${prod.hsnSac.ifBlank { "-" }}"
                                    } else {
                                        "Stock: ${prod.stock.toInt()} ${prod.unit} • HSN: ${prod.hsnSac.ifBlank { "-" }}"
                                    }
                                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    "${FormatUtils.inr(prod.sellingPrice)} (+${prod.gstPercent.toInt()}%)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TealPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LineItemEditorDialog(
    initial: InvoiceLineItem?,
    onSave: (InvoiceLineItem) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var hsn by remember(initial) { mutableStateOf(initial?.hsnSac ?: "") }
    var rate by remember(initial) { mutableStateOf(if (initial != null && initial.rate > 0) initial.rate.toString() else "") }
    var qty by remember(initial) { mutableStateOf(if (initial != null) initial.qty.toString() else "1") }
    var gst by remember(initial) { mutableStateOf(if (initial != null) initial.gstPercent.toInt().toString() else "18") }
    var isService by remember(initial) { mutableStateOf(initial?.isService ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Custom Line Item" else "Edit Line Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item / Service Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("line_item_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hsn,
                        onValueChange = { hsn = it },
                        label = { Text("HSN / SAC") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it },
                        label = { Text("Rate ₹ *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("line_item_rate_input")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = gst,
                        onValueChange = { gst = it },
                        label = { Text("GST %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("0", "5", "12", "18", "28").forEach { slab ->
                        FilterChip(
                            selected = gst == slab,
                            onClick = { gst = slab },
                            label = { Text("$slab%") }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mark as Service", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isService, onCheckedChange = { isService = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val r = rate.toDoubleOrNull() ?: 0.0
                    val q = (qty.toDoubleOrNull() ?: 1.0).coerceAtLeast(0.01)
                    val g = (gst.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
                    if (name.isNotBlank()) {
                        onSave(
                            InvoiceLineItem(
                                productId = initial?.productId ?: "",
                                name = name.trim(),
                                hsnSac = hsn.trim(),
                                qty = q,
                                rate = r,
                                gstPercent = g,
                                isService = isService
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("line_item_save_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (initial == null) "Add" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicePreviewSheet(
    invoice: ErpInvoice,
    settings: BusinessSettings,
    onPrint: () -> Unit,
    onExportPdf: () -> Unit,
    onSaveAndPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    val tpl = InvoiceTemplates.byId(invoice.templateId.ifBlank { settings.invoiceTemplate })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Live Invoice Preview", style = MaterialTheme.typography.titleLarge)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = tpl.bgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(tpl.primaryColor)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(settings.businessName, style = MaterialTheme.typography.titleLarge, color = Color.White)
                                if (settings.gstin.isNotBlank()) {
                                    Text("GSTIN: ${settings.gstin}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(invoice.docType.uppercase(), style = MaterialTheme.typography.labelMedium, color = tpl.accentColor)
                                Text("#${invoice.invoiceNumber}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("BILL TO", style = MaterialTheme.typography.labelSmall, color = tpl.inkColor.copy(alpha = 0.6f))
                                Text(invoice.clientName.ifBlank { "Walk-in Customer" }, style = MaterialTheme.typography.titleSmall, color = tpl.inkColor)
                                if (invoice.clientGstin.isNotBlank()) {
                                    Text("GSTIN: ${invoice.clientGstin}", style = MaterialTheme.typography.bodySmall, color = tpl.inkColor.copy(alpha = 0.75f))
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Date: ${invoice.dateStr}", style = MaterialTheme.typography.bodySmall, color = tpl.inkColor)
                                if (invoice.dueDateStr.isNotBlank()) {
                                    Text("Due: ${invoice.dueDateStr}", style = MaterialTheme.typography.bodySmall, color = tpl.inkColor)
                                }
                            }
                        }

                        HorizontalDivider(color = tpl.inkColor.copy(alpha = 0.12f))

                        invoice.items.forEach { item ->
                            val gross = item.qty * item.rate
                            val taxable = if (invoice.gstInclusive && item.gstPercent > 0) gross / (1.0 + item.gstPercent / 100.0) else gross
                            val taxAmt = if (invoice.gstInclusive && item.gstPercent > 0) gross - taxable else taxable * (item.gstPercent / 100.0)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium, color = tpl.inkColor, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${item.qty} × ${FormatUtils.inr(item.rate)} • Tax ${FormatUtils.inr(taxAmt)} (${item.gstPercent.toInt()}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tpl.inkColor.copy(alpha = 0.65f)
                                    )
                                }
                                Text(FormatUtils.inr(taxable + taxAmt), style = MaterialTheme.typography.titleSmall, color = tpl.inkColor)
                            }
                        }

                        HorizontalDivider(color = tpl.inkColor.copy(alpha = 0.12f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Taxable Subtotal", style = MaterialTheme.typography.bodySmall, color = tpl.inkColor.copy(alpha = 0.75f))
                            Text(FormatUtils.inr(invoice.subtotal), style = MaterialTheme.typography.bodySmall, color = tpl.inkColor)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (invoice.gstMode == "inter") "IGST" else "CGST + SGST", style = MaterialTheme.typography.bodySmall, color = tpl.inkColor.copy(alpha = 0.75f))
                            Text(FormatUtils.inr(invoice.cgstTotal + invoice.sgstTotal + invoice.igstTotal), style = MaterialTheme.typography.bodySmall, color = tpl.inkColor)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total", style = MaterialTheme.typography.titleMedium, color = tpl.primaryColor)
                            Text(FormatUtils.inr(invoice.grandTotal), style = MaterialTheme.typography.titleLarge, color = tpl.primaryColor)
                        }
                        Text(FormatUtils.amountInWordsInr(invoice.grandTotal), style = MaterialTheme.typography.labelSmall, color = tpl.inkColor.copy(alpha = 0.7f))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPrint, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Print")
                }
                OutlinedButton(onClick = onExportPdf, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Share PDF")
                }
            }
            Button(
                onClick = onSaveAndPrint,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Save & Print Invoice")
            }
        }
    }
}
