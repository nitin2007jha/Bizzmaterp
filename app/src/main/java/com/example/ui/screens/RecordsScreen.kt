package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.FormatUtils
import com.example.data.model.BusinessSettings
import com.example.data.model.ErpInvoice
import com.example.data.model.InvoiceTemplates
import com.example.ui.components.ErpStatusBadge
import com.example.ui.components.SwipeActionCard
import com.example.ui.components.UpiPaymentQrCard
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoft
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoft
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoft
import com.example.ui.viewmodel.ErpUiState

@Composable
fun RecordsScreen(
    state: ErpUiState,
    paperSize: String = "a4",
    defaultTerms: String = "",
    onNewInvoice: () -> Unit,
    onEditInvoice: (ErpInvoice) -> Unit,
    onDuplicateInvoice: (ErpInvoice) -> Unit = {},
    onConvertToInvoice: (ErpInvoice) -> Unit = {},
    onMarkPaid: (ErpInvoice) -> Unit,
    onSoftDelete: (ErpInvoice) -> Unit,
    onSharePdfError: (String) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var selectedInvoice by remember { mutableStateOf<ErpInvoice?>(null) }

    val filtered = remember(state.invoices, query, filter) {
        val q = query.trim().lowercase()
        state.invoices
            .filter { !it.isDeleted }
            .filter { inv ->
                when (filter) {
                    "paid" -> inv.status == "final" && inv.paymentStatus == "paid"
                    "unpaid" -> inv.status == "final" && inv.paymentStatus != "paid"
                    "draft" -> inv.status == "draft"
                    "quotation" -> inv.docType == "quotation"
                    "proforma" -> inv.docType == "proforma"
                    "challan" -> inv.docType == "challan"
                    "credit_note" -> inv.docType == "credit_note"
                    else -> true
                }
            }
            .filter { inv ->
                q.isBlank() ||
                    inv.invoiceNumber.lowercase().contains(q) ||
                    inv.clientName.lowercase().contains(q) ||
                    inv.clientPhone.contains(q)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Search bill # or client...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("records_search_input")
                )
                Button(
                    onClick = onNewInvoice,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("records_new_invoice_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bill")
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val chips = listOf(
                    "all" to "All (${state.invoices.count { !it.isDeleted }})",
                    "paid" to "Paid",
                    "unpaid" to "Pending",
                    "draft" to "Drafts",
                    "quotation" to "Quotations",
                    "proforma" to "Proforma",
                    "challan" to "Challans",
                    "credit_note" to "Credit Notes"
                )
                items(chips) { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("records_filter_$key")
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = TealPrimary, modifier = Modifier.padding(8.dp))
                        Text("No matching records found", style = MaterialTheme.typography.titleMedium)
                        Text("Swipe right on any pending bill to mark Paid, or swipe left to move to Trash.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { inv ->
                        SwipeActionCard(
                            onSwipeRight = if (inv.paymentStatus != "paid" && inv.status == "final") {
                                { onMarkPaid(inv) }
                            } else null,
                            rightLabel = "Mark Paid",
                            onSwipeLeft = { onSoftDelete(inv) },
                            leftLabel = "Trash"
                        ) {
                            Card(
                                onClick = { selectedInvoice = inv },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("record_card_${inv.invoiceNumber}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(inv.invoiceNumber, style = MaterialTheme.typography.titleMedium, color = TealPrimary)
                                            if (inv.docType != "invoice") {
                                                ErpStatusBadge(inv.docType.uppercase(), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                            if (inv.status == "draft") {
                                                ErpStatusBadge("DRAFT", WarningSoft, WarningAmber)
                                            }
                                        }
                                        Text(
                                            text = inv.clientName.ifBlank { "Walk-in Customer" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${inv.dateStr} • ${inv.paymentMode.uppercase()} • ${inv.items.size} item(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(FormatUtils.inr(inv.grandTotal), style = MaterialTheme.typography.titleMedium)
                                        val (bg, fg) = when {
                                            inv.status == "draft" -> WarningSoft to WarningAmber
                                            inv.paymentStatus == "paid" -> SuccessSoft to SuccessGreen
                                            inv.paymentStatus == "partial" -> WarningSoft to WarningAmber
                                            else -> DangerSoft to DangerRed
                                        }
                                        ErpStatusBadge(
                                            if (inv.status == "draft") "DRAFT" else inv.paymentStatus.uppercase(),
                                            bg,
                                            fg
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

    selectedInvoice?.let { inv ->
        InvoiceDetailSheet(
            invoice = inv,
            settings = state.settings,
            onDismiss = { selectedInvoice = null },
            onMarkPaid = {
                onMarkPaid(inv)
                selectedInvoice = null
            },
            onEdit = {
                selectedInvoice = null
                onEditInvoice(inv)
            },
            onDuplicate = {
                selectedInvoice = null
                onDuplicateInvoice(inv)
            },
            onConvertToInvoice = {
                selectedInvoice = null
                onConvertToInvoice(inv)
            },
            onDelete = {
                onSoftDelete(inv)
                selectedInvoice = null
            },
            onPrint = {
                FormatUtils.printInvoice(context, inv, state.settings, paperSize, defaultTerms)
                    .onFailure { onSharePdfError(it.localizedMessage ?: "Print failed") }
            },
            onExportPdf = {
                FormatUtils.exportAndShareInvoicePdf(context, inv, state.settings, paperSize, defaultTerms)
                    .onFailure { onSharePdfError(it.localizedMessage ?: "PDF export failed") }
            },
            onShareReminder = {
                val bal = (inv.grandTotal - inv.paidAmount).coerceAtLeast(0.0)
                val upiLine = if (state.settings.upiId.isNotBlank()) "\nPay via UPI: ${state.settings.upiId}" else ""
                val msg = "Hello ${inv.clientName.ifBlank { "Customer" }},\nInvoice #${inv.invoiceNumber} dated ${inv.dateStr} for ${FormatUtils.inr(inv.grandTotal)} (${if (bal > 0) "Balance due: ${FormatUtils.inr(bal)}" else "Paid"}).$upiLine\n— ${state.settings.businessName}"
                FormatUtils.shareText(context, "Invoice ${inv.invoiceNumber}", msg)
            },
            onUpiMessage = onSharePdfError
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailSheet(
    invoice: ErpInvoice,
    settings: BusinessSettings,
    onDismiss: () -> Unit,
    onMarkPaid: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onConvertToInvoice: () -> Unit = {},
    onDelete: () -> Unit,
    onPrint: () -> Unit,
    onExportPdf: () -> Unit,
    onShareReminder: () -> Unit,
    onUpiMessage: (String) -> Unit = {}
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
            // Styled Template Preview Card
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
                            Column {
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

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("BILL TO", style = MaterialTheme.typography.labelSmall, color = tpl.inkColor.copy(alpha = 0.6f))
                                Text(invoice.clientName.ifBlank { "Walk-in Customer" }, style = MaterialTheme.typography.titleSmall, color = tpl.inkColor)
                                if (invoice.clientPhone.isNotBlank()) {
                                    Text(invoice.clientPhone, style = MaterialTheme.typography.bodySmall, color = tpl.inkColor.copy(alpha = 0.75f))
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Date: ${invoice.dateStr}", style = MaterialTheme.typography.bodySmall, color = tpl.inkColor)
                                Text("Template: ${tpl.name}", style = MaterialTheme.typography.labelSmall, color = tpl.primaryColor)
                            }
                        }

                        HorizontalDivider(color = tpl.inkColor.copy(alpha = 0.12f))

                        invoice.items.forEach { item ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium, color = tpl.inkColor, fontWeight = FontWeight.Medium)
                                    Text("${item.qty} × ${FormatUtils.inr(item.rate)} (GST ${item.gstPercent.toInt()}%)", style = MaterialTheme.typography.bodySmall, color = tpl.inkColor.copy(alpha = 0.65f))
                                }
                                Text(
                                    FormatUtils.inr(item.qty * item.rate * (1.0 + item.gstPercent / 100.0)),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = tpl.inkColor
                                )
                            }
                        }

                        HorizontalDivider(color = tpl.inkColor.copy(alpha = 0.12f))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = tpl.inkColor.copy(alpha = 0.75f))
                            Text(FormatUtils.inr(invoice.subtotal), style = MaterialTheme.typography.bodySmall, color = tpl.inkColor)
                        }
                        if (invoice.gstMode == "inter") {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("IGST", style = MaterialTheme.typography.bodySmall, color = tpl.inkColor.copy(alpha = 0.75f))
                                Text(FormatUtils.inr(invoice.igstTotal), style = MaterialTheme.typography.bodySmall, color = tpl.inkColor)
                            }
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CGST + SGST", style = MaterialTheme.typography.bodySmall, color = tpl.inkColor.copy(alpha = 0.75f))
                                Text(FormatUtils.inr(invoice.cgstTotal + invoice.sgstTotal), style = MaterialTheme.typography.bodySmall, color = tpl.inkColor)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total", style = MaterialTheme.typography.titleMedium, color = tpl.primaryColor)
                            Text(FormatUtils.inr(invoice.grandTotal), style = MaterialTheme.typography.titleLarge, color = tpl.primaryColor)
                        }
                    }
                }
            }

            if (invoice.paymentStatus != "paid" && settings.upiId.isNotBlank()) {
                val due = (invoice.grandTotal - invoice.paidAmount).coerceAtLeast(0.0)
                UpiPaymentQrCard(
                    upiId = settings.upiId,
                    payeeName = settings.businessName,
                    amount = due,
                    note = "${invoice.invoiceNumber} ${invoice.clientName}".trim(),
                    onResultMessage = onUpiMessage
                )
            }

            if (invoice.docType == "quotation" || invoice.docType == "proforma") {
                Button(
                    onClick = onConvertToInvoice,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("invoice_detail_convert_button")
                ) {
                    Text("Convert ${invoice.docType.replaceFirstChar { it.uppercase() }} → Tax Invoice")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPrint,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.weight(1f).testTag("invoice_detail_print_button")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Print")
                }
                OutlinedButton(
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1f).testTag("invoice_detail_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("PDF")
                }
                OutlinedButton(
                    onClick = onShareReminder,
                    modifier = Modifier.weight(1f).testTag("invoice_detail_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (invoice.paymentStatus != "paid") {
                    Button(
                        onClick = onMarkPaid,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        modifier = Modifier.weight(1.1f).testTag("invoice_detail_mark_paid_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Paid")
                    }
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).testTag("invoice_detail_edit_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onDuplicate,
                    modifier = Modifier.weight(1f).testTag("invoice_detail_duplicate_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Clone")
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("invoice_detail_trash_button")
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Trash", tint = DangerRed)
                }
            }
        }
    }
}
