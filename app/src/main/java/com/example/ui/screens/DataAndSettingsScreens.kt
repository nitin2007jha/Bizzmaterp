package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.FormatUtils
import com.example.data.model.ErpClient
import com.example.data.model.ErpExpense
import com.example.data.model.ErpInvoice
import com.example.data.model.ErpProduct
import com.example.data.model.InvoiceLineItem
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.ErpUiState
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DataModuleScreen(
    state: ErpUiState,
    onLoadSampleData: (List<ErpProduct>, List<ErpClient>, List<ErpInvoice>, List<ErpExpense>) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current

    val importJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
            val root = JSONObject(raw)
            val prods = mutableListOf<ErpProduct>()
            val clis = mutableListOf<ErpClient>()
            val invs = mutableListOf<ErpInvoice>()
            val exps = mutableListOf<ErpExpense>()

            root.optJSONArray("products")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val name = o.optString("name")
                    if (name.isNotBlank()) {
                        prods += ErpProduct(
                            name = name,
                            category = o.optString("category", "General"),
                            sellingPrice = o.optDouble("price", o.optDouble("sellingPrice", 0.0)),
                            costPrice = o.optDouble("costPrice", 0.0),
                            gstPercent = o.optDouble("gstPercent", 18.0),
                            stock = o.optDouble("stock", 0.0),
                            hsnSac = o.optString("hsnSac", ""),
                            unit = o.optString("unit", "pcs")
                        )
                    }
                }
            }
            root.optJSONArray("clients")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val name = o.optString("name")
                    if (name.isNotBlank()) {
                        clis += ErpClient(
                            name = name,
                            phone = o.optString("phone", ""),
                            email = o.optString("email", ""),
                            gstin = o.optString("gstin", ""),
                            address = o.optString("address", "")
                        )
                    }
                }
            }
            onLoadSampleData(prods, clis, invs, exps)
            onMessage("Imported ${prods.size} products and ${clis.size} clients from JSON!")
        }.onFailure {
            onMessage(it.localizedMessage ?: "Invalid JSON backup file")
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Backup & Restore (JSON)", style = MaterialTheme.typography.titleMedium)
                        Text("Export all your ERP records or restore products & clients from a previously exported JSON backup.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val json = JSONObject().apply {
                                        put("businessName", state.settings.businessName)
                                        put("exportedAt", FormatUtils.todayStr())
                                        put("invoicesCount", state.invoices.size)
                                        put("products", JSONArray(state.products.filter { !it.isDeleted }.map {
                                            JSONObject().put("name", it.name).put("category", it.category)
                                                .put("sellingPrice", it.sellingPrice).put("costPrice", it.costPrice)
                                                .put("gstPercent", it.gstPercent).put("stock", it.stock)
                                                .put("hsnSac", it.hsnSac).put("unit", it.unit)
                                        }))
                                        put("clients", JSONArray(state.clients.filter { !it.isDeleted }.map {
                                            JSONObject().put("name", it.name).put("phone", it.phone)
                                                .put("email", it.email).put("gstin", it.gstin).put("address", it.address)
                                        }))
                                    }.toString(2)
                                    FormatUtils.exportAndShareTextFile(context, "ERP_Backup_${FormatUtils.todayStr()}.json", "application/json", json)
                                        .onSuccess { onMessage("JSON Backup ready to share") }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Export JSON")
                            }
                            OutlinedButton(
                                onClick = { importJsonLauncher.launch(arrayOf("application/json", "text/*")) },
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Import JSON")
                            }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Spreadsheet CSV Exports", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val csv = buildString {
                                    appendLine("InvoiceNo,DocType,Date,DueDate,Client,GSTIN,Subtotal,CGST,SGST,IGST,Total,Paid,Mode,Status")
                                    state.invoices.filter { !it.isDeleted }.forEach {
                                        appendLine("\"${it.invoiceNumber}\",${it.docType},${it.dateStr},${it.dueDateStr},\"${it.clientName}\",\"${it.clientGstin}\",${it.subtotal},${it.cgstTotal},${it.sgstTotal},${it.igstTotal},${it.grandTotal},${it.paidAmount},${it.paymentMode},${it.paymentStatus}")
                                    }
                                }
                                FormatUtils.exportAndShareTextFile(context, "Invoices_${FormatUtils.todayStr()}.csv", "text/csv", csv)
                                    .onSuccess { onMessage("Invoices CSV exported") }
                            }) {
                                Icon(Icons.Default.TableChart, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Invoices CSV")
                            }
                            OutlinedButton(onClick = {
                                val csv = buildString {
                                    appendLine("Name,Category,HSN,SKU,SellPrice,CostPrice,GST%,Stock,Unit")
                                    state.products.filter { !it.isDeleted }.forEach {
                                        appendLine("\"${it.name}\",\"${it.category}\",\"${it.hsnSac}\",\"${it.sku}\",${it.sellingPrice},${it.costPrice},${it.gstPercent},${it.stock},${it.unit}")
                                    }
                                }
                                FormatUtils.exportAndShareTextFile(context, "Products_${FormatUtils.todayStr()}.csv", "text/csv", csv)
                                    .onSuccess { onMessage("Products CSV exported") }
                            }) {
                                Icon(Icons.Default.TableChart, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Products CSV")
                            }
                            OutlinedButton(onClick = {
                                val csv = buildString {
                                    appendLine("Name,Phone,Email,GSTIN,State,OpeningBal")
                                    state.clients.filter { !it.isDeleted }.forEach {
                                        appendLine("\"${it.name}\",\"${it.phone}\",\"${it.email}\",\"${it.gstin}\",\"${it.state}\",${it.openingBalance}")
                                    }
                                }
                                FormatUtils.exportAndShareTextFile(context, "Clients_${FormatUtils.todayStr()}.csv", "text/csv", csv)
                                    .onSuccess { onMessage("Clients CSV exported") }
                            }) {
                                Icon(Icons.Default.TableChart, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Clients CSV")
                            }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Quick Demo Dataset", style = MaterialTheme.typography.titleMedium)
                        Text("Optional: Load sample products, clients, invoices, and expenses to explore charts & GST reports.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(
                            onClick = {
                                val today = FormatUtils.todayStr()
                                val sampleProds = listOf(
                                    ErpProduct(name = "Ashwagandha Gold 60 Caps", category = "Ayurveda", sellingPrice = 499.0, costPrice = 260.0, gstPercent = 12.0, stock = 42.0, lowStockThreshold = 10.0, unit = "btl", hsnSac = "3004"),
                                    ErpProduct(name = "Chyawanprash Royal 500g", category = "Immunity", sellingPrice = 380.0, costPrice = 210.0, gstPercent = 12.0, stock = 7.0, lowStockThreshold = 10.0, unit = "jar", hsnSac = "2106"),
                                    ErpProduct(name = "Wellness Consultation", category = "Clinic", isService = true, sellingPrice = 600.0, gstPercent = 18.0, durationMins = 30L, hsnSac = "9993")
                                )
                                val sampleClients = listOf(
                                    ErpClient(name = "Aarav Medical Store", phone = "9876543210", gstin = "27AABCU9603R1ZM", state = "Maharashtra", address = "Pune, MH"),
                                    ErpClient(name = "Meera Sharma", phone = "9822011223", state = "Maharashtra", address = "Nashik, MH")
                                )
                                val sampleInv = listOf(
                                    ErpInvoice(
                                        invoiceNumber = "INV-101",
                                        docType = "invoice",
                                        status = "final",
                                        paymentStatus = "paid",
                                        paymentMode = "upi",
                                        clientName = "Aarav Medical Store",
                                        clientPhone = "9876543210",
                                        clientGstin = "27AABCU9603R1ZM",
                                        dateStr = today,
                                        subtotal = 1996.0,
                                        cgstTotal = 119.76,
                                        sgstTotal = 119.76,
                                        grandTotal = 2235.52,
                                        paidAmount = 2235.52,
                                        items = listOf(InvoiceLineItem(name = "Ashwagandha Gold 60 Caps", hsnSac = "3004", qty = 4.0, rate = 499.0, gstPercent = 12.0))
                                    )
                                )
                                val sampleExp = listOf(
                                    ErpExpense(title = "Kraft Packaging Cartons", category = "Supplies", vendor = "PackIndia", amount = 850.0, gstAmount = 129.0, paymentMode = "upi", dateStr = today)
                                )
                                onLoadSampleData(sampleProds, sampleClients, sampleInv, sampleExp)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("load_sample_erp_data_button")
                        ) {
                            Icon(Icons.Default.Science, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Load Sample ERP Data")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecycleBinScreen(
    state: ErpUiState,
    onRestoreInvoice: (ErpInvoice) -> Unit,
    onPurgeInvoice: (String) -> Unit,
    onRestoreProduct: (ErpProduct) -> Unit,
    onPurgeProduct: (String) -> Unit,
    onRestoreClient: (ErpClient) -> Unit,
    onPurgeClient: (String) -> Unit,
    onRestoreExpense: (ErpExpense) -> Unit,
    onPurgeExpense: (String) -> Unit
) {
    val invs = state.invoices.filter { it.isDeleted }
    val prods = state.products.filter { it.isDeleted }
    val clis = state.clients.filter { it.isDeleted }
    val exps = state.expenses.filter { it.isDeleted }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (invs.isEmpty() && prods.isEmpty() && clis.isEmpty() && exps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Recycle Bin is empty", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 108.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(invs, key = { "inv_${it.id}" }) { inv ->
                    TrashRow("Invoice #${inv.invoiceNumber}", FormatUtils.inr(inv.grandTotal), { onRestoreInvoice(inv) }, { onPurgeInvoice(inv.id) })
                }
                items(prods, key = { "prd_${it.id}" }) { p ->
                    TrashRow("Product: ${p.name}", FormatUtils.inr(p.sellingPrice), { onRestoreProduct(p) }, { onPurgeProduct(p.id) })
                }
                items(clis, key = { "cli_${it.id}" }) { c ->
                    TrashRow("Client: ${c.name}", c.phone, { onRestoreClient(c) }, { onPurgeClient(c.id) })
                }
                items(exps, key = { "exp_${it.id}" }) { e ->
                    TrashRow("Expense: ${e.title}", FormatUtils.inr(e.amount), { onRestoreExpense(e) }, { onPurgeExpense(e.id) })
                }
            }
        }
    }
}

@Composable
private fun TrashRow(title: String, sub: String, onRestore: () -> Unit, onPurge: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRestore) { Icon(Icons.Default.Restore, contentDescription = "Restore", tint = EmeraldAccent) }
            IconButton(onClick = onPurge) { Icon(Icons.Default.DeleteForever, contentDescription = "Delete Permanently", tint = DangerRed) }
        }
    }
}
