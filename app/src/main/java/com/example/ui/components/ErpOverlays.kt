package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.FormatUtils
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AriaMessage
import com.example.ui.viewmodel.ErpNotification
import com.example.ui.viewmodel.ErpUiState
import java.util.Locale

data class ModuleTileSpec(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val accent: Color
)

val allModuleTiles = listOf(
    ModuleTileSpec("invoice", "New Invoice", Icons.AutoMirrored.Filled.ReceiptLong, TealPrimary),
    ModuleTileSpec("pos", "Quick POS", Icons.Default.PointOfSale, EmeraldAccent),
    ModuleTileSpec("expenses", "Expenses", Icons.Default.RequestQuote, WarningAmber),
    ModuleTileSpec("daybook", "Daybook", Icons.Default.Book, IndigoAccent),
    ModuleTileSpec("services", "Services", Icons.Default.DesignServices, PurpleAccent),
    ModuleTileSpec("purchase-orders", "Purchase Orders", Icons.Default.ShoppingCartCheckout, TealPrimary),
    ModuleTileSpec("appointments", "Appointments", Icons.Default.CalendarMonth, IndigoAccent),
    ModuleTileSpec("gst", "GST Filing", Icons.Default.AccountBalance, EmeraldAccent),
    ModuleTileSpec("reports", "Reports & P&L", Icons.AutoMirrored.Filled.TrendingUp, TealPrimary),
    ModuleTileSpec("ai-report", "AI Advisor", Icons.Default.AutoAwesome, PurpleAccent),
    ModuleTileSpec("tools", "Business Tools", Icons.Default.Build, WarningAmber),
    ModuleTileSpec("website", "Online Store", Icons.Default.Storefront, EmeraldAccent),
    ModuleTileSpec("branches", "Branches", Icons.Default.Business, IndigoAccent),
    ModuleTileSpec("employees", "Employees", Icons.Default.Badge, TealPrimary),
    ModuleTileSpec("ca-portal", "CA Portal", Icons.Default.Receipt, PurpleAccent),
    ModuleTileSpec("data-module", "Import/Export", Icons.Default.CloudUpload, IndigoAccent),
    ModuleTileSpec("settings", "Settings", Icons.Default.Settings, TealPrimary)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreModulesSheet(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "All ERP Modules",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Billing, operations, GST compliance, AI & store",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 104.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 460.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(allModuleTiles) { tile ->
                    Surface(
                        onClick = {
                            onDismiss()
                            onNavigate(tile.route)
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("more_tile_${tile.route}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tile.accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tile.icon,
                                    contentDescription = null,
                                    tint = tile.accent,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            Text(
                                text = tile.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AriaVoiceSheet(
    messages: List<AriaMessage>,
    isBusy: Boolean,
    onSendQuery: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var speechError by remember { mutableStateOf<String?>(null) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun startListening() {
        val sr = speechRecognizer
        if (sr == null) {
            speechError = "Voice input unavailable on this device. Type your command below."
            return
        }
        speechError = null
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                speechError = "Couldn't catch voice ($error). Try typing or tap mic again."
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    onSendQuery(spoken)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        sr.startListening(intent)
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
        else speechError = "Microphone permission is needed for voice input. You can still type below."
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TealPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Aria — Voice ERP Assistant", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (isListening) "Listening..." else "English & Hinglish ERP Commands",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isListening) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close Aria")
                }
            }

            if (isBusy || isListening) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = TealPrimary)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Today's sales", "Low stock items", "GST payable", "New invoice", "Open POS").forEach { chip ->
                    AssistChip(
                        onClick = { onSendQuery(chip) },
                        label = { Text(chip, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (msg.isUser) TealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }

            if (speechError != null) {
                Text(
                    text = speechError!!,
                    color = DangerRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledIconButton(
                    onClick = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) startListening() else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isListening) DangerRed else TealPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("aria_mic_button")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Speak to Aria")
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Ask Aria anything...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("aria_text_input")
                )

                FilledIconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            onSendQuery(input)
                            input = ""
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = EmeraldAccent, contentColor = Color.White),
                    modifier = Modifier.testTag("aria_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send to Aria")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchSheet(
    state: ErpUiState,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()

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
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search invoices, products, clients, expenses...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_search_input")
            )

            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val invMatches = state.invoices.filter { !it.isDeleted && (q.isBlank() || it.invoiceNumber.lowercase().contains(q) || it.clientName.lowercase().contains(q)) }.take(5)
                val prodMatches = state.products.filter { !it.isDeleted && (q.isBlank() || it.name.lowercase().contains(q) || it.sku.lowercase().contains(q) || it.barcode.lowercase().contains(q)) }.take(5)
                val cliMatches = state.clients.filter { !it.isDeleted && (q.isBlank() || it.name.lowercase().contains(q) || it.phone.contains(q)) }.take(5)

                items(invMatches) { inv ->
                    SearchHitCard("Invoice #${inv.invoiceNumber}", "${inv.clientName.ifBlank { "Walk-in" }} • ${FormatUtils.inr(inv.grandTotal)}") {
                        onDismiss(); onNavigate("records")
                    }
                }
                items(prodMatches) { prod ->
                    SearchHitCard(prod.name, "${if (prod.isService) "Service" else "Stock: ${prod.stock.toInt()} ${prod.unit}"} • ${FormatUtils.inr(prod.sellingPrice)}") {
                        onDismiss(); onNavigate(if (prod.isService) "services" else "inventory")
                    }
                }
                items(cliMatches) { cli ->
                    SearchHitCard(cli.name, cli.phone.ifBlank { "Client CRM" }) {
                        onDismiss(); onNavigate("clients")
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHitCard(title: String, sub: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    notifications: List<ErpNotification>,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Business Alerts", style = MaterialTheme.typography.headlineSmall)
            if (notifications.isEmpty()) {
                Text(
                    text = "All caught up! No pending alerts right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                notifications.forEach { n ->
                    Card(
                        onClick = { onDismiss(); onNavigate(n.targetTab) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = TealPrimary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(n.title, style = MaterialTheme.typography.titleSmall)
                                Text(n.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            ErpStatusBadge(n.category, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinLockGate(
    expectedPin: String,
    onUnlocked: () -> Unit
) {
    var entered by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = TealPrimary, modifier = Modifier.size(32.dp))
            }
            Text("Enter ERP PIN", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = if (wrong) "Incorrect PIN. Try again." else "Enter your 4-digit security PIN",
                style = MaterialTheme.typography.bodySmall,
                color = if (wrong) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(4) { idx ->
                    val filled = idx < entered.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (filled) TealPrimary else MaterialTheme.colorScheme.outline)
                    )
                }
            }

            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "<")
            )
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { key ->
                        Surface(
                            onClick = {
                                wrong = false
                                when (key) {
                                    "C" -> entered = ""
                                    "<" -> if (entered.isNotEmpty()) entered = entered.dropLast(1)
                                    else -> {
                                        if (entered.length < 4) {
                                            entered += key
                                            if (entered.length == 4) {
                                                if (entered == expectedPin) {
                                                    onUnlocked()
                                                } else {
                                                    wrong = true
                                                    entered = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .size(66.dp)
                                .testTag("pin_key_$key")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (key == "<") {
                                    Icon(Icons.Default.Backspace, contentDescription = "Backspace")
                                } else {
                                    Text(key, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
