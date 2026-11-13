package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.FormatUtils
import com.example.data.model.ErpProduct
import com.example.ui.components.ErpStatusBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoft
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoft
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoft

private data class CategoryStats(
    val name: String,
    val skuCount: Int,
    val totalUnits: Double,
    val costValue: Double,
    val lowCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerSheet(
    products: List<ErpProduct>,
    managedCategories: List<String>,
    servicesOnly: Boolean,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (String, String) -> Unit,
    onSelectCategoryFilter: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var renamingTarget by remember { mutableStateOf<String?>(null) }

    val statsList = remember(products, managedCategories, servicesOnly) {
        val active = products.filter { !it.isDeleted && it.isService == servicesOnly }
        val allNames = (managedCategories + active.map { it.category.ifBlank { "General" } })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sorted()

        allNames.map { catName ->
            val items = active.filter { it.category.ifBlank { "General" }.equals(catName, ignoreCase = true) }
            CategoryStats(
                name = catName,
                skuCount = items.size,
                totalUnits = items.sumOf { it.stock },
                costValue = items.sumOf { it.stock * (it.costPrice.takeIf { c -> c > 0 } ?: it.sellingPrice * 0.7) },
                lowCount = items.count { !it.isService && it.stock <= it.lowStockThreshold }
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Category, contentDescription = null, tint = TealPrimary)
                Column {
                    Text("Category Management", style = MaterialTheme.typography.titleLarge)
                    Text("Create, rename, merge & audit inventory categories", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("New Category Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).testTag("category_new_input")
                )
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCategory(newCategoryName.trim())
                            newCategoryName = ""
                        }
                    },
                    enabled = newCategoryName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(54.dp).testTag("category_add_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(statsList, key = { it.name.lowercase() }) { stat ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(stat.name, style = MaterialTheme.typography.titleMedium)
                                    ErpStatusBadge("${stat.skuCount} SKUs", SuccessSoft, SuccessGreen)
                                    if (stat.lowCount > 0) {
                                        ErpStatusBadge("${stat.lowCount} Low", DangerSoft, DangerRed)
                                    }
                                }
                                Text(
                                    text = if (servicesOnly) "Service category • ${stat.skuCount} items"
                                    else "${stat.totalUnits.toInt()} units • Est. Stock Value ${FormatUtils.inr(stat.costValue)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(onClick = { renamingTarget = stat.name }) {
                                    Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename Category", tint = TealPrimary)
                                }
                                IconButton(onClick = {
                                    onSelectCategoryFilter(stat.name)
                                    onDismiss()
                                }) {
                                    Icon(Icons.Default.FilterList, contentDescription = "Filter by ${stat.name}", tint = EmeraldAccent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renamingTarget?.let { oldName ->
        var edited by remember(oldName) { mutableStateOf(oldName) }
        AlertDialog(
            onDismissRequest = { renamingTarget = null },
            title = { Text("Rename / Merge Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "All products currently in \"$oldName\" will update automatically. Enter an existing category name to merge them.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = edited,
                        onValueChange = { edited = it },
                        label = { Text("Target Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameCategory(oldName, edited)
                        renamingTarget = null
                    },
                    enabled = edited.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { renamingTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StockAuditDialog(
    product: ErpProduct,
    onApplyExactStock: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var qtyStr by remember(product.id) { mutableStateOf(product.stock.toInt().toString()) }
    val previewQty = (qtyStr.toDoubleOrNull() ?: product.stock).coerceAtLeast(0.0)
    val delta = previewQty - product.stock

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Live Stock Audit: ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Current Stock: ${product.stock.toInt()} ${product.unit} • Alert below ${product.lowStockThreshold.toInt()} ${product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("Physical Count (${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("stock_audit_input")
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(-10, -5, 5, 10, 25, 50).forEach { step ->
                        AssistChip(
                            onClick = {
                                val next = (previewQty + step).coerceAtLeast(0.0)
                                qtyStr = next.toInt().toString()
                            },
                            label = { Text(if (step > 0) "+$step" else "$step") }
                        )
                    }
                }
                if (delta != 0.0) {
                    ErpStatusBadge(
                        label = if (delta > 0) "Net Gain +${delta.toInt()} ${product.unit}" else "Net Deduction ${delta.toInt()} ${product.unit}",
                        containerColor = if (delta > 0) SuccessSoft else WarningSoft,
                        contentColor = if (delta > 0) SuccessGreen else WarningAmber
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyExactStock(previewQty) },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) { Text("Update Stock") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
