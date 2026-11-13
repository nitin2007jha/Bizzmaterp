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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessSettings
import com.example.data.model.InvoiceTemplates
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.ErpUiState

private val allModuleSpecs = listOf(
    "invoice" to "Invoice Builder",
    "records" to "Sales & Records",
    "pos" to "Quick POS Billing",
    "inventory" to "Inventory & Stock",
    "services" to "Services Catalog",
    "clients" to "Clients CRM",
    "daybook" to "Daybook Ledger",
    "expenses" to "Expenses & ITC",
    "purchase-orders" to "Purchase Orders",
    "appointments" to "Appointments",
    "branches" to "Multi-Branch",
    "employees" to "Staff & Permissions",
    "gst" to "GST Filing Center",
    "reports" to "Financial Reports",
    "ai-report" to "AI Business Advisor",
    "ca-portal" to "CA Portal",
    "tools" to "Business Tools Hub",
    "website" to "Online Storefront",
    "data-module" to "Backup & Import"
)

@Composable
fun SettingsScreen(
    state: ErpUiState,
    userEmail: String,
    pinCode: String,
    paperSize: String,
    defaultTerms: String,
    onSaveSettings: (BusinessSettings) -> Unit,
    onSaveLocalPrefs: (pin: String, paper: String, terms: String) -> Unit,
    onOpenTrash: () -> Unit,
    onSignOut: () -> Unit
) {
    val s = state.settings
    var activeTab by remember { mutableStateOf("business") }

    var bizName by remember(s) { mutableStateOf(s.businessName) }
    var ownerName by remember(s) { mutableStateOf(s.ownerName) }
    var phone by remember(s) { mutableStateOf(s.phone) }
    var email by remember(s) { mutableStateOf(s.email) }
    var gstin by remember(s) { mutableStateOf(s.gstin) }
    var pan by remember(s) { mutableStateOf(s.pan) }
    var stateName by remember(s) { mutableStateOf(s.state) }
    var address by remember(s) { mutableStateOf(s.address) }

    var bankName by remember(s) { mutableStateOf(s.bankName) }
    var accountNo by remember(s) { mutableStateOf(s.accountNo) }
    var ifsc by remember(s) { mutableStateOf(s.ifsc) }
    var upiId by remember(s) { mutableStateOf(s.upiId) }
    var prefix by remember(s) { mutableStateOf(s.invoicePrefix) }
    var nextInvStr by remember(s) { mutableStateOf(s.nextInvoiceNumber.toString()) }
    var gstInclusive by remember(s) { mutableStateOf(s.gstInclusive) }
    var dailyTargetStr by remember(s) { mutableStateOf(s.dailyTarget.toLong().toString()) }
    var tplId by remember(s) { mutableStateOf(s.invoiceTemplate) }
    var paperInput by remember(paperSize) { mutableStateOf(paperSize) }
    var termsInput by remember(defaultTerms) { mutableStateOf(defaultTerms) }

    val enabledModules = remember(s) {
        mutableStateListOf<String>().apply { addAll(s.enabledModules) }
    }

    var darkMode by remember(s) { mutableStateOf(s.darkMode) }
    var language by remember(s) { mutableStateOf(s.language) }
    var pinEnabled by remember(s) { mutableStateOf(s.pinLockEnabled) }
    var pinInput by remember(pinCode) { mutableStateOf(pinCode) }

    fun commitAllSettings() {
        val cleanPin = if (pinInput.length == 4) pinInput else pinCode
        onSaveLocalPrefs(cleanPin, paperInput, termsInput.trim())
        onSaveSettings(
            s.copy(
                businessName = bizName.ifBlank { "My Business" }.trim(),
                ownerName = ownerName.trim(),
                phone = phone.trim(),
                email = email.trim(),
                gstin = gstin.trim(),
                pan = pan.trim(),
                state = stateName.trim(),
                address = address.trim(),
                bankName = bankName.trim(),
                accountNo = accountNo.trim(),
                ifsc = ifsc.trim(),
                upiId = upiId.trim(),
                invoicePrefix = prefix.ifBlank { "INV" }.trim(),
                nextInvoiceNumber = (nextInvStr.toLongOrNull() ?: s.nextInvoiceNumber).coerceAtLeast(1L),
                gstInclusive = gstInclusive,
                dailyTarget = (dailyTargetStr.toDoubleOrNull() ?: s.dailyTarget).coerceAtLeast(0.0),
                invoiceTemplate = tplId,
                enabledModules = enabledModules.distinct(),
                darkMode = darkMode,
                language = language,
                pinLockEnabled = pinEnabled
            )
        )
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
                        "business" to "Business",
                        "billing" to "Billing & Tax",
                        "modules" to "Modules (${enabledModules.size})",
                        "security" to "App & Security"
                    )
                    items(tabs) { (k, lbl) ->
                        FilterChip(
                            selected = activeTab == k,
                            onClick = { activeTab = k },
                            label = { Text(lbl) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("settings_tab_$k")
                        )
                    }
                }
            }

            when (activeTab) {
                "business" -> {
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Signed in: $userEmail", style = MaterialTheme.typography.titleSmall, color = TealPrimary)
                                OutlinedTextField(value = bizName, onValueChange = { bizName = it }, label = { Text("Business Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.weight(1f))
                                }
                                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Business Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = gstin, onValueChange = { gstin = it.uppercase() }, label = { Text("Business GSTIN") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = pan, onValueChange = { pan = it.uppercase() }, label = { Text("PAN") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                OutlinedTextField(value = stateName, onValueChange = { stateName = it }, label = { Text("State") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }

                "billing" -> {
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Bank, UPI & Invoice Defaults", style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(value = upiId, onValueChange = { upiId = it }, label = { Text("UPI ID (e.g. biz@okicici)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = bankName, onValueChange = { bankName = it }, label = { Text("Bank") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = ifsc, onValueChange = { ifsc = it.uppercase() }, label = { Text("IFSC") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                OutlinedTextField(value = accountNo, onValueChange = { accountNo = it }, label = { Text("A/C Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = prefix, onValueChange = { prefix = it.uppercase() }, label = { Text("Inv Prefix") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = nextInvStr, onValueChange = { nextInvStr = it.filter(Char::isDigit) }, label = { Text("Next Inv #") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = dailyTargetStr, onValueChange = { dailyTargetStr = it.filter(Char::isDigit) }, label = { Text("Daily Target ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1.2f))
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Prices Include GST by Default", style = MaterialTheme.typography.bodyMedium)
                                    Switch(checked = gstInclusive, onCheckedChange = { gstInclusive = it })
                                }
                                Text("Default Print Format", style = MaterialTheme.typography.labelLarge)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("a4" to "A4 Standard", "thermal" to "Thermal 80mm").forEach { (k, lbl) ->
                                        FilterChip(
                                            selected = paperInput == k,
                                            onClick = { paperInput = k },
                                            label = { Text(lbl) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = termsInput,
                                    onValueChange = { termsInput = it },
                                    label = { Text("Default Invoice Terms & Footer") },
                                    placeholder = { Text("e.g. Goods once sold will not be returned.") },
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                }

                "modules" -> {
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("ERP Module Visibility", style = MaterialTheme.typography.titleMedium)
                                        Text("Customize which modules appear in your All Modules launcher & Dashboard shortcuts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(onClick = {
                                        enabledModules.clear()
                                        enabledModules.addAll(listOf("dashboard") + allModuleSpecs.map { it.first })
                                    }) { Text("Enable All") }
                                }
                                allModuleSpecs.forEach { (key, title) ->
                                    val checked = key in enabledModules
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(title, style = MaterialTheme.typography.bodyMedium)
                                        Switch(
                                            checked = checked,
                                            onCheckedChange = { on ->
                                                if (on && key !in enabledModules) enabledModules.add(key)
                                                else if (!on) enabledModules.remove(key)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "security" -> {
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
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = onOpenTrash, modifier = Modifier.weight(1f).height(48.dp)) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
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

            item {
                Button(
                    onClick = { commitAllSettings() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("settings_save_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save All Settings")
                }
            }
        }
    }
}
