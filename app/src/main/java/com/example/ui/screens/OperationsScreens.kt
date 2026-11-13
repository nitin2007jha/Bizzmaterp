package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.ErpAppointment
import com.example.data.model.ErpBranch
import com.example.data.model.ErpEmployee
import com.example.data.model.ErpInvoice
import com.example.data.model.ErpPurchaseOrder
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
fun PurchaseOrdersScreen(
    state: ErpUiState,
    onSavePo: (ErpPurchaseOrder) -> Unit,
    onReceivePo: (ErpPurchaseOrder) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Purchase Orders (${state.purchaseOrders.size})", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = { showCreate = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("po_add_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("New PO")
                }
            }

            if (state.purchaseOrders.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No purchase orders yet. Create a PO to order stock from vendors.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.purchaseOrders, key = { it.id }) { po ->
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("${po.poNumber} • ${po.vendorName}", style = MaterialTheme.typography.titleMedium)
                                        Text("${po.dateStr} • ${po.items.size} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(FormatUtils.inr(po.totalAmount), style = MaterialTheme.typography.titleMedium, color = TealPrimary)
                                        ErpStatusBadge(
                                            po.status.uppercase(),
                                            if (po.status == "received") SuccessSoft else WarningSoft,
                                            if (po.status == "received") SuccessGreen else WarningAmber
                                        )
                                    }
                                }
                                if (po.status != "received") {
                                    Button(
                                        onClick = { onReceivePo(po) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                        modifier = Modifier.fillMaxWidth().testTag("po_receive_${po.poNumber}")
                                    ) {
                                        Icon(Icons.Default.Inventory2, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Receive Stock into Inventory")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var vendor by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        val items = remember { mutableStateListOf<PoLineItem>() }
        ModalBottomSheet(onDismissRequest = { showCreate = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Create Purchase Order", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = vendor, onValueChange = { vendor = it }, label = { Text("Vendor / Supplier Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Vendor Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Tap products to add to PO:", style = MaterialTheme.typography.labelLarge)
                LazyColumn(modifier = Modifier.heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.products.filter { !it.isDeleted && !it.isService }) { prod ->
                        OutlinedButton(onClick = {
                            items += PoLineItem(productId = prod.id, name = prod.name, qty = 10.0, rate = prod.costPrice.takeIf { it > 0 } ?: prod.sellingPrice * 0.7)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("+ ${prod.name} (10 ${prod.unit})")
                        }
                    }
                }
                val total = items.sumOf { it.qty * it.rate }
                Text("PO Items: ${items.size} • Total: ${FormatUtils.inr(total)}", style = MaterialTheme.typography.titleSmall, color = TealPrimary)
                Button(
                    onClick = {
                        if (vendor.isNotBlank()) {
                            onSavePo(
                                ErpPurchaseOrder(
                                    poNumber = "PO-${(state.purchaseOrders.size + 1).toString().padStart(3, '0')}",
                                    vendorName = vendor.trim(),
                                    vendorPhone = phone.trim(),
                                    dateStr = FormatUtils.todayStr(),
                                    status = "sent",
                                    items = items.toList(),
                                    totalAmount = total
                                )
                            )
                            showCreate = false
                        }
                    },
                    enabled = vendor.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Create Purchase Order") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    state: ErpUiState,
    onSaveAppointment: (ErpAppointment) -> Unit,
    onConvertToInvoice: (ErpInvoice) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Appointments & Bookings", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { showAdd = true }, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary), modifier = Modifier.testTag("appt_add_button")) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Book")
                }
            }
            if (state.appointments.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No appointments booked yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.appointments, key = { it.id }) { appt ->
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(appt.clientName, style = MaterialTheme.typography.titleMedium)
                                        Text("${appt.serviceName} • ${appt.dateStr} ${appt.timeStr}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(FormatUtils.inr(appt.amount), style = MaterialTheme.typography.titleSmall, color = TealPrimary)
                                        ErpStatusBadge(appt.status.uppercase(), if (appt.status == "completed") SuccessSoft else WarningSoft, if (appt.status == "completed") SuccessGreen else WarningAmber)
                                    }
                                }
                                if (appt.status == "scheduled") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { onSaveAppointment(appt.copy(status = "completed")) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Complete")
                                        }
                                        Button(
                                            onClick = {
                                                onSaveAppointment(appt.copy(status = "completed"))
                                                val invNo = "${state.settings.invoicePrefix}-${state.settings.nextInvoiceNumber.toString().padStart(3, '0')}"
                                                onConvertToInvoice(
                                                    ErpInvoice(
                                                        invoiceNumber = invNo,
                                                        docType = "invoice",
                                                        status = "final",
                                                        paymentStatus = "paid",
                                                        paymentMode = "upi",
                                                        clientName = appt.clientName,
                                                        clientPhone = appt.clientPhone,
                                                        dateStr = FormatUtils.todayStr(),
                                                        subtotal = appt.amount,
                                                        grandTotal = appt.amount,
                                                        paidAmount = appt.amount,
                                                        items = listOf(InvoiceLineItem(name = appt.serviceName, qty = 1.0, rate = appt.amount, gstPercent = 0.0, isService = true))
                                                    )
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Bill Now")
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

    if (showAdd) {
        var cName by remember { mutableStateOf("") }
        var cPhone by remember { mutableStateOf("") }
        var service by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("11:00 AM") }
        var amount by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showAdd = false }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("New Appointment", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = cName, onValueChange = { cName = it }, label = { Text("Client Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cPhone, onValueChange = { cPhone = it }, label = { Text("Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = service, onValueChange = { service = it }, label = { Text("Service *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Fee ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                Button(
                    onClick = {
                        if (cName.isNotBlank() && service.isNotBlank()) {
                            onSaveAppointment(ErpAppointment(clientName = cName.trim(), clientPhone = cPhone.trim(), serviceName = service.trim(), dateStr = FormatUtils.todayStr(), timeStr = time.trim(), amount = amount.toDoubleOrNull() ?: 0.0))
                            showAdd = false
                        }
                    },
                    enabled = cName.isNotBlank() && service.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Save Appointment") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchesScreen(state: ErpUiState, onSaveBranch: (ErpBranch) -> Unit) {
    var editing by remember { mutableStateOf<ErpBranch?>(null) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Business Branches (${state.branches.size})", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { editing = ErpBranch() }, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Branch")
                }
            }
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.branches, key = { it.id }) { br ->
                    Card(onClick = { editing = br }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(br.name, style = MaterialTheme.typography.titleMedium)
                                Text("${br.manager.ifBlank { "No manager" }} • ${br.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (br.address.isNotBlank()) Text(br.address, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = br.isActive, onCheckedChange = { onSaveBranch(br.copy(isActive = it)) })
                        }
                    }
                }
            }
        }
    }
    editing?.let { init ->
        var name by remember { mutableStateOf(init.name) }
        var manager by remember { mutableStateOf(init.manager) }
        var phone by remember { mutableStateOf(init.phone) }
        var address by remember { mutableStateOf(init.address) }
        ModalBottomSheet(onDismissRequest = { editing = null }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Branch Details", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Branch Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = manager, onValueChange = { manager = it }, label = { Text("Manager") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveBranch(init.copy(name = name.trim(), manager = manager.trim(), phone = phone.trim(), address = address.trim()))
                            editing = null
                        }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Save Branch") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EmployeesScreen(state: ErpUiState, onSaveEmployee: (ErpEmployee) -> Unit) {
    var editing by remember { mutableStateOf<ErpEmployee?>(null) }
    val allPerms = listOf("billing", "pos", "inventory", "clients", "expenses", "reports", "gst", "settings")

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Staff & Permissions (${state.employees.size})", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { editing = ErpEmployee(permissions = listOf("billing", "pos")) }, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Staff")
                }
            }
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.employees, key = { it.id }) { emp ->
                    Card(onClick = { editing = emp }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(emp.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${emp.role} • ${emp.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                ErpStatusBadge(if (emp.isActive) "ACTIVE" else "INACTIVE", if (emp.isActive) SuccessSoft else WarningSoft, if (emp.isActive) SuccessGreen else WarningAmber)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                emp.permissions.forEach { p ->
                                    ErpStatusBadge(p, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { init ->
        var name by remember { mutableStateOf(init.name) }
        var role by remember { mutableStateOf(init.role) }
        var phone by remember { mutableStateOf(init.phone) }
        var email by remember { mutableStateOf(init.email) }
        val perms = remember { mutableStateListOf<String>().apply { addAll(init.permissions) } }
        ModalBottomSheet(onDismissRequest = { editing = null }) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Staff Profile & Permissions", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Module Access", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allPerms.forEach { p ->
                        val active = p in perms
                        FilterChip(
                            selected = active,
                            onClick = { if (active) perms.remove(p) else perms.add(p) },
                            label = { Text(p.uppercase()) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
                        )
                    }
                }
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveEmployee(init.copy(name = name.trim(), role = role.ifBlank { "Staff" }.trim(), phone = phone.trim(), email = email.trim(), permissions = perms.toList()))
                            editing = null
                        }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Save Staff") }
            }
        }
    }
}
