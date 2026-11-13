package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.FormatUtils
import com.example.data.model.ErpProduct
import com.example.ui.components.ErpStatusBadge
import com.example.ui.components.SwipeActionCard
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoft
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoft
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.ErpUiState

@Composable
fun InventoryScreen(
    state: ErpUiState,
    servicesOnly: Boolean = false,
    onSaveProduct: (ErpProduct) -> Unit,
    onAdjustStock: (ErpProduct, Double) -> Unit,
    onSoftDelete: (ErpProduct) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var lowStockOnly by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ErpProduct?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showBarcodeDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showBarcodeDialog = true
        else {
            onMessage("Camera permission denied — enter barcode manually below.")
            showBarcodeDialog = true
        }
    }

    val categories = remember(state.products, servicesOnly) {
        listOf("All") + state.products
            .filter { !it.isDeleted && it.isService == servicesOnly }
            .map { it.category.ifBlank { "General" } }
            .distinct()
            .sorted()
    }

    val list = remember(state.products, servicesOnly, query, selectedCategory, lowStockOnly) {
        val q = query.trim().lowercase()
        state.products
            .filter { !it.isDeleted && it.isService == servicesOnly }
            .filter { selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true) }
            .filter { !lowStockOnly || (!it.isService && it.stock <= it.lowStockThreshold) }
            .filter { p ->
                q.isBlank() ||
                    p.name.lowercase().contains(q) ||
                    p.sku.lowercase().contains(q) ||
                    p.barcode.lowercase().contains(q) ||
                    p.category.lowercase().contains(q)
            }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (!servicesOnly) {
                        {
                            IconButton(onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (granted) showBarcodeDialog = true else cameraLauncher.launch(Manifest.permission.CAMERA)
                            }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode Lookup", tint = TealPrimary)
                            }
                        }
                    } else null,
                    placeholder = { Text(if (servicesOnly) "Search services..." else "Search SKU, barcode, name...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).testTag("inventory_search_input")
                )

                Button(
                    onClick = {
                        editingProduct = ErpProduct(isService = servicesOnly)
                        showEditor = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(52.dp).testTag("inventory_add_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }

            if (categories.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
                        )
                    }
                }
            }

            if (!servicesOnly) {
                val stockVal = list.sumOf { it.stock * it.costPrice }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = lowStockOnly,
                        onClick = { lowStockOnly = !lowStockOnly },
                        label = { Text("Low Stock Only") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DangerRed, selectedLabelColor = Color.White),
                        modifier = Modifier.testTag("inventory_low_stock_chip")
                    )
                    Text("Valuation: ${FormatUtils.inr(stockVal)}", style = MaterialTheme.typography.titleSmall, color = TealPrimary)
                }
            }

            if (list.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (servicesOnly) "No services yet. Tap + Add to list your offerings." else "No inventory products yet. Tap + Add to add stock.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(list, key = { it.id }) { prod ->
                        val isLow = !prod.isService && prod.stock <= prod.lowStockThreshold
                        SwipeActionCard(
                            onSwipeRight = {
                                editingProduct = prod
                                showEditor = true
                            },
                            rightLabel = "Edit",
                            rightColor = TealPrimary,
                            onSwipeLeft = { onSoftDelete(prod) },
                            leftLabel = "Trash"
                        ) {
                            Card(
                                onClick = {
                                    editingProduct = prod
                                    showEditor = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth().testTag("product_card_${prod.name}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(prod.name, style = MaterialTheme.typography.titleMedium)
                                            ErpStatusBadge(prod.category, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        val margin = if (prod.sellingPrice > 0 && prod.costPrice > 0) {
                                            " • Margin ${(((prod.sellingPrice - prod.costPrice) / prod.sellingPrice) * 100).toInt()}%"
                                        } else ""
                                        Text(
                                            text = "Sell ${FormatUtils.inr(prod.sellingPrice)} • GST ${prod.gstPercent.toInt()}%$margin",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (prod.sku.isNotBlank() || prod.hsnSac.isNotBlank()) {
                                            Text(
                                                text = "SKU: ${prod.sku.ifBlank { "-" }} • HSN/SAC: ${prod.hsnSac.ifBlank { "-" }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (prod.isService) {
                                        ErpStatusBadge("${prod.durationMins} min", SuccessSoft, SuccessGreen)
                                    } else {
                                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            ErpStatusBadge(
                                                "${prod.stock.toInt()} ${prod.unit}",
                                                if (isLow) DangerSoft else SuccessSoft,
                                                if (isLow) DangerRed else SuccessGreen
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { onAdjustStock(prod, -1.0) }, modifier = Modifier.testTag("stock_dec_${prod.name}")) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Decrease Stock")
                                                }
                                                IconButton(onClick = { onAdjustStock(prod, 1.0) }, modifier = Modifier.testTag("stock_inc_${prod.name}")) {
                                                    Icon(Icons.Default.Add, contentDescription = "Increase Stock", tint = EmeraldAccent)
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

    if (showEditor && editingProduct != null) {
        ProductEditorSheet(
            initial = editingProduct!!,
            onDismiss = { showEditor = false },
            onSave = {
                onSaveProduct(it)
                showEditor = false
            }
        )
    }

    if (showBarcodeDialog) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBarcodeDialog = false },
            title = { Text("Barcode / SKU Lookup") },
            text = {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Enter or paste barcode") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    query = code.trim()
                    showBarcodeDialog = false
                }) { Text("Search") }
            },
            dismissButton = {
                TextButton(onClick = { showBarcodeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorSheet(
    initial: ErpProduct,
    onDismiss: () -> Unit,
    onSave: (ErpProduct) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var category by remember { mutableStateOf(initial.category) }
    var sku by remember { mutableStateOf(initial.sku) }
    var barcode by remember { mutableStateOf(initial.barcode) }
    var hsnSac by remember { mutableStateOf(initial.hsnSac) }
    var selling by remember { mutableStateOf(if (initial.sellingPrice > 0) initial.sellingPrice.toString() else "") }
    var cost by remember { mutableStateOf(if (initial.costPrice > 0) initial.costPrice.toString() else "") }
    var gst by remember { mutableStateOf(initial.gstPercent.toInt().toString()) }
    var stock by remember { mutableStateOf(initial.stock.toInt().toString()) }
    var lowThresh by remember { mutableStateOf(initial.lowStockThreshold.toInt().toString()) }
    var unit by remember { mutableStateOf(initial.unit) }
    var duration by remember { mutableStateOf(initial.durationMins.toString()) }
    var batchNo by remember { mutableStateOf(initial.batchNo) }
    var expiryDate by remember { mutableStateOf(initial.expiryDate) }
    var description by remember { mutableStateOf(initial.description) }
    var showInStore by remember { mutableStateOf(initial.showInStore) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (initial.id.isBlank()) "Add ${if (initial.isService) "Service" else "Product"}" else "Edit ${initial.name}",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("prod_name_input"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = hsnSac, onValueChange = { hsnSac = it }, label = { Text("HSN / SAC") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = selling, onValueChange = { selling = it }, label = { Text("Selling ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f).testTag("prod_price_input"))
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = gst, onValueChange = { gst = it }, label = { Text("GST %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(0.8f))
            }
            if (!initial.isService) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = lowThresh, onValueChange = { lowThresh = it }, label = { Text("Low Alert") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.weight(0.9f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = batchNo, onValueChange = { batchNo = it }, label = { Text("Batch #") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = expiryDate, onValueChange = { expiryDate = it }, label = { Text("Expiry (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            } else {
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (mins)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description / Notes") }, modifier = Modifier.fillMaxWidth())

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Show in Online Storefront", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = showInStore, onCheckedChange = { showInStore = it })
            }

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                category = category.ifBlank { "General" }.trim(),
                                sku = sku.trim(),
                                barcode = barcode.trim(),
                                hsnSac = hsnSac.trim(),
                                sellingPrice = selling.toDoubleOrNull() ?: 0.0,
                                costPrice = cost.toDoubleOrNull() ?: 0.0,
                                gstPercent = gst.toDoubleOrNull() ?: 18.0,
                                stock = stock.toDoubleOrNull() ?: 0.0,
                                lowStockThreshold = lowThresh.toDoubleOrNull() ?: 10.0,
                                unit = unit.ifBlank { "pcs" }.trim(),
                                durationMins = duration.toLongOrNull() ?: 30L,
                                batchNo = batchNo.trim(),
                                expiryDate = expiryDate.trim(),
                                description = description.trim(),
                                showInStore = showInStore
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("prod_save_button")
            ) { Text("Save Item") }
        }
    }
}
