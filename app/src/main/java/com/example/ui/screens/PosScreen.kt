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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.FormatUtils
import com.example.data.model.ErpClient
import com.example.data.model.ErpInvoice
import com.example.data.model.ErpProduct
import com.example.data.model.InvoiceLineItem
import com.example.ui.components.ErpStatusBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.ErpUiState

@Composable
fun PosScreen(
    state: ErpUiState,
    paperSize: String = "a4",
    onQuickAddClient: (ErpClient) -> Unit = {},
    onPrintMessage: (String, Boolean) -> Unit = { _, _ -> },
    onCheckoutInvoice: (ErpInvoice) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var payMode by remember { mutableStateOf("upi") }
    var selectedClient by remember { mutableStateOf<ErpClient?>(null) }
    var showClientPicker by remember { mutableStateOf(false) }
    var discountStr by remember { mutableStateOf("") }
    val cart = remember { mutableStateMapOf<String, Double>() }

    val catalog = remember(state.products) { state.products.filter { !it.isDeleted } }
    val categories = remember(catalog) {
        listOf("All") + catalog.map { it.category.ifBlank { "General" } }.distinct().sorted()
    }
    val visibleItems = remember(catalog, query, selectedCategory) {
        val q = query.trim().lowercase()
        catalog.filter { p ->
            (selectedCategory == "All" || p.category == selectedCategory) &&
                (q.isBlank() || p.name.lowercase().contains(q) || p.barcode.lowercase().contains(q) || p.sku.lowercase().contains(q))
        }
    }

    val cartProducts = catalog.filter { (cart[it.id] ?: 0.0) > 0.0 }
    val subtotal = cartProducts.sumOf { p ->
        val qty = cart[p.id] ?: 0.0
        val gross = qty * p.sellingPrice
        if (state.settings.gstInclusive && p.gstPercent > 0) gross / (1.0 + p.gstPercent / 100.0) else gross
    }
    val gstTotal = cartProducts.sumOf { p ->
        val qty = cart[p.id] ?: 0.0
        val gross = qty * p.sellingPrice
        if (state.settings.gstInclusive && p.gstPercent > 0) gross - (gross / (1.0 + p.gstPercent / 100.0)) else gross * (p.gstPercent / 100.0)
    }
    val discount = discountStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val grandTotal = (subtotal + gstTotal - discount).coerceAtLeast(0.0)

    fun buildPosInvoice(): ErpInvoice {
        val invNo = "${state.settings.invoicePrefix}-${state.settings.nextInvoiceNumber.toString().padStart(3, '0')}"
        val lines = cartProducts.map { p ->
            InvoiceLineItem(
                productId = p.id,
                name = p.name,
                hsnSac = p.hsnSac,
                qty = cart[p.id] ?: 1.0,
                rate = p.sellingPrice,
                gstPercent = p.gstPercent,
                isService = p.isService
            )
        }
        return ErpInvoice(
            invoiceNumber = invNo,
            docType = "invoice",
            status = "final",
            paymentStatus = if (payMode == "credit") "unpaid" else "paid",
            paymentMode = payMode,
            clientId = selectedClient?.id.orEmpty(),
            clientName = selectedClient?.name?.ifBlank { "POS Walk-in" } ?: "POS Walk-in",
            clientPhone = selectedClient?.phone.orEmpty(),
            clientGstin = selectedClient?.gstin.orEmpty(),
            clientAddress = selectedClient?.address.orEmpty(),
            dateStr = FormatUtils.todayStr(),
            gstMode = "intra",
            gstInclusive = state.settings.gstInclusive,
            subtotal = subtotal,
            cgstTotal = gstTotal / 2.0,
            sgstTotal = gstTotal / 2.0,
            discountAmount = discount,
            grandTotal = grandTotal,
            paidAmount = if (payMode == "credit") 0.0 else grandTotal,
            templateId = state.settings.invoiceTemplate,
            items = lines
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Search or scan barcode / SKU...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).testTag("pos_search_input")
                )
                OutlinedButton(
                    onClick = { showClientPicker = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Pick customer")
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = selectedClient?.name?.take(10) ?: "Walk-in",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

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

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 152.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(visibleItems, key = { it.id }) { prod ->
                    val qtyInCart = cart[prod.id] ?: 0.0
                    PosProductTile(
                        product = prod,
                        qty = qtyInCart,
                        onAdd = { cart[prod.id] = qtyInCart + 1.0 },
                        onRemove = {
                            val next = qtyInCart - 1.0
                            if (next <= 0.0) cart.remove(prod.id) else cart[prod.id] = next
                        }
                    )
                }
            }

            // Live Checkout Bar
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            listOf("upi" to "UPI", "cash" to "Cash", "credit" to "Credit").forEach { (m, lbl) ->
                                FilterChip(
                                    selected = payMode == m,
                                    onClick = { payMode = m },
                                    label = { Text(lbl) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
                                )
                            }
                        }
                        OutlinedTextField(
                            value = discountStr,
                            onValueChange = { discountStr = it },
                            label = { Text("Disc ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(96.dp)
                        )
                        if (cartProducts.isNotEmpty()) {
                            IconButton(onClick = { cart.clear(); discountStr = "" }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear cart", tint = DangerRed)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${cart.values.sum().toInt()} items • GST ${FormatUtils.inr(gstTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(FormatUtils.inr(grandTotal), style = MaterialTheme.typography.headlineSmall, color = TealPrimary)
                        }
                        OutlinedButton(
                            onClick = {
                                val inv = buildPosInvoice()
                                onCheckoutInvoice(inv)
                                FormatUtils.printInvoice(context, inv, state.settings, paperSize)
                                    .onFailure { onPrintMessage(it.localizedMessage ?: "Print failed", true) }
                                cart.clear()
                                discountStr = ""
                                selectedClient = null
                            },
                            enabled = cartProducts.isNotEmpty(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Charge & Print")
                        }
                        Button(
                            onClick = {
                                onCheckoutInvoice(buildPosInvoice())
                                cart.clear()
                                discountStr = ""
                                selectedClient = null
                            },
                            enabled = cartProducts.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(50.dp).testTag("pos_checkout_button")
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Charge")
                        }
                    }
                }
            }
        }
    }

    if (showClientPicker) {
        ClientPickerSheet(
            clients = state.clients.filter { !it.isDeleted },
            onSelectClient = { cli ->
                selectedClient = cli
                showClientPicker = false
            },
            onQuickCreateClient = { newCli ->
                onQuickAddClient(newCli)
                selectedClient = newCli
                showClientPicker = false
            },
            onDismiss = { showClientPicker = false }
        )
    }
}

@Composable
private fun PosProductTile(
    product: ErpProduct,
    qty: Double,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        onClick = onAdd,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (qty > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.testTag("pos_tile_${product.name}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ErpStatusBadge(
                    if (product.isService) "SERVICE" else "${product.stock.toInt()} ${product.unit}",
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (qty > 0) {
                    ErpStatusBadge("${qty.toInt()}x", TealPrimary, Color.White)
                }
            }
            Text(product.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(FormatUtils.inr(product.sellingPrice), style = MaterialTheme.typography.titleMedium, color = TealPrimary, fontWeight = FontWeight.Bold)
                if (qty > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onRemove, modifier = Modifier.height(32.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease qty")
                        }
                        IconButton(onClick = onAdd, modifier = Modifier.height(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Increase qty")
                        }
                    }
                }
            }
        }
    }
}
