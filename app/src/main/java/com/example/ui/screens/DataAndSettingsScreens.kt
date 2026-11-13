package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.FormatUtils
import com.example.data.model.BusinessSettings
import com.example.data.model.ErpClient
import com.example.data.model.ErpExpense
import com.example.data.model.ErpInvoice
import com.example.data.model.ErpProduct
import com.example.data.model.InvoiceLineItem
import com.example.data.model.InvoiceTemplates
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.ErpUiState
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun DataModuleScreen(
    state: ErpUiState,
    onLoadSampleData: (List<ErpProduct>, List<ErpClient>, List<ErpInvoice>, List<ErpExpense>) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backup & Portability", style = MaterialTheme.typography.titleMedium)
                    Text("Export all your ERP records as a structured JSON backup or spreadsheet CSV.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = {
                            val json = JSONObject().apply {
                                put("businessName", state.settings.businessName)
                                put("exportedAt", FormatUtils.todayStr())
                                put("invoicesCount", state.invoices.size)
                                put("products", JSONArray(state.products.map { JSONObject().put("name", it.name).put("price", it.sellingPrice).put("stock", it.stock) }))
                                put("clients", JSONArray(state.clients.map { JSONObject().put("name", it.name).put("phone", it.phone) }))
                            }.toString(2)
                            FormatUtils.exportAndShareTextFile(context, "ERP_Backup_${FormatUtils.todayStr()}.json", "application/json", json)
                                .onSuccess { onMessage("JSON Backup ready to share") }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Export Full JSON Backup")
                    }
                }
            }

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
                                ErpClient(name = "Aarav Medical Store", phone = "9876543210", gstin = "27AABCU9603R1ZM", address = "Pune, MH"),
                                ErpClient(name = "Meera Sharma", phone = "9822011223", address = "Nashik, MH")
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
                                    dateStr = today,
                                    subtotal = 1996.0,
                                    cgstTotal = 119.76,
                                    sgstTotal = 119.76,
                                    grandTotal = 2235.52,
                                    paidAmount = 2235.52,
                                    items = listOf(InvoiceLineItem(name = "Ashwagandha Gold 60 Caps", qty = 4.0, rate = 499.0, gstPercent = 12.0))
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

@Composable
fun SettingsScreen(
    state: ErpUiState,
    userEmail: String,
    pinCode: String,
    onSaveSettings: (BusinessSettings) -> Unit,
    onSavePinCode: (String) -> Unit,
    onOpenTrash: () -> Unit,
    onSignOut: () -> Unit
) {
    val s = state.settings
    var bizName by remember(s) { mutableStateOf(s.businessName) }
    var ownerName by remember(s) { mutableStateOf(s.ownerName) }
    var phone by remember(s) { mutableStateOf(s.phone) }
    var gstin by remember(s) { mutableStateOf(s.gstin) }
    var pan by remember(s) { mutableStateOf(s.pan) }
    var stateName by remember(s) { mutableStateOf(s.state) }
    var address by remember(s) { mutableStateOf(s.address) }
    var bankName by remember(s) { mutableStateOf(s.bankName) }
    var accountNo by remember(s) { mutableStateOf(s.accountNo) }
    var ifsc by remember(s) { mutableStateOf(s.ifsc) }
    var upiId by remember(s) { mutableStateOf(s.upiId) }
    var prefix by remember(s) { mutableStateOf(s.invoicePrefix) }
    var tplId by remember(s) { mutableStateOf(s.invoiceTemplate) }
    var darkMode by remember(s) { mutableStateOf(s.darkMode) }
    var language by remember(s) { mutableStateOf(s.language) }
    var pinEnabled by remember(s) { mutableStateOf(s.pinLockEnabled) }
    var pinInput by remember(pinCode) { mutableStateOf(pinCode) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Account: $userEmail", style = MaterialTheme.typography.titleSmall, color = TealPrimary)
                        OutlinedTextField(value = bizName, onValueChange = { bizName = it }, label = { Text("Business Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = gstin, onValueChange = { gstin = it.uppercase() }, label = { Text("Business GSTIN") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = pan, onValueChange = { pan = it.uppercase() }, label = { Text("PAN") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(value = stateName, onValueChange = { stateName = it }, label = { Text("State") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Bank, UPI & Invoice Defaults", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = upiId, onValueChange = { upiId = it }, label = { Text("UPI ID (e.g. biz@okicici)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = bankName, onValueChange = { bankName = it }, label = { Text("Bank") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = ifsc, onValueChange = { ifsc = it.uppercase() }, label = { Text("IFSC") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = accountNo, onValueChange = { accountNo = it }, label = { Text("A/C Number") }, singleLine = true, modifier = Modifier.weight(1.4f))
                            OutlinedTextField(value = prefix, onValueChange = { prefix = it.uppercase() }, label = { Text("Inv Prefix") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Text("Default Invoice Template", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(InvoiceTemplates.all) { tpl ->
                                val selected = tpl.id == tplId
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(if (selected) 2.dp else 1.dp, if (selected) TealPrimary else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                        .clickable { tplId = tpl.id }
                                        .padding(8.dp)
                                ) {
                                    Box(Modifier.size(28.dp).clip(CircleShape).background(tpl.primaryColor))
                                    Spacer(Modifier.height(4.dp))
                                    Text(tpl.name.split(" ").first(), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Appearance, Language & PIN Lock", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (k, lbl) ->
                                FilterChip(selected = darkMode == k, onClick = { darkMode = k }, label = { Text(lbl) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("en" to "English", "hi" to "Hindi", "hinglish" to "Hinglish").forEach { (k, lbl) ->
                                FilterChip(selected = language == k, onClick = { language = k }, label = { Text(lbl) })
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Enable 4-Digit App PIN Lock", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = pinEnabled, onCheckedChange = { pinEnabled = it })
                        }
                        if (pinEnabled) {
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pinInput = it },
                                label = { Text("4-Digit PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Button(
                            onClick = {
                                if (pinInput.length == 4) onSavePinCode(pinInput)
                                onSaveSettings(
                                    s.copy(
                                        businessName = bizName.ifBlank { "My Business" }.trim(),
                                        ownerName = ownerName.trim(),
                                        phone = phone.trim(),
                                        gstin = gstin.trim(),
                                        pan = pan.trim(),
                                        state = stateName.trim(),
                                        address = address.trim(),
                                        bankName = bankName.trim(),
                                        accountNo = accountNo.trim(),
                                        ifsc = ifsc.trim(),
                                        upiId = upiId.trim(),
                                        invoicePrefix = prefix.ifBlank { "INV" }.trim(),
                                        invoiceTemplate = tplId,
                                        darkMode = darkMode,
                                        language = language,
                                        pinLockEnabled = pinEnabled
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("settings_save_button")
                        ) { Text("Save Settings") }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onOpenTrash, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Recycle Bin")
                    }
                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("settings_sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out")
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
