package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.FormatUtils
import com.example.ui.components.ErpStatusBadge
import com.example.ui.components.HealthScoreRing
import com.example.ui.components.PaymentDonutChart
import com.example.ui.components.RevenueExpenseBarChart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoft
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoft
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealMint
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoft
import com.example.ui.viewmodel.ErpUiState

@Composable
fun DashboardScreen(
    state: ErpUiState,
    dashFilter: String,
    onSelectFilter: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onNavigate: (String) -> Unit,
    onUpdateDailyTarget: (Double) -> Unit
) {
    var showHealthDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    val s = state.stats

    val activeInvoices = remember(state.invoices) {
        state.invoices.filter { !it.isDeleted && it.status == "final" }
    }
    val lowStockItems = remember(state.products) {
        state.products.filter { !it.isDeleted && !it.isService && it.stock <= it.lowStockThreshold }
    }
    val topSellers = remember(activeInvoices) {
        activeInvoices.flatMap { it.items }
            .groupBy { it.name }
            .map { (name, items) ->
                Triple(name, items.sumOf { it.qty }, items.sumOf { it.qty * it.rate })
            }
            .sortedByDescending { it.third }
            .take(5)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Period Filter + Floating Search Pill
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = onOpenSearch,
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dashboard_search_pill")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = TealPrimary)
                            Text(
                                text = "Search invoices, products, clients...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            ErpStatusBadge("Ctrl+K", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("all" to "All Time", "this_month" to "This Month", "last_month" to "Last Month").forEach { (key, label) ->
                            FilterChip(
                                selected = dashFilter == key,
                                onClick = { onSelectFilter(key) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("dash_filter_$key")
                            )
                        }
                    }

                    val quickShortcuts = remember(state.settings.enabledModules) {
                        val all = listOf(
                            Triple("invoice", "New Bill", Icons.AutoMirrored.Filled.ReceiptLong),
                            Triple("pos", "Quick POS", Icons.Default.PointOfSale),
                            Triple("expenses", "Expense", Icons.Default.RequestQuote),
                            Triple("gst", "GST Filing", Icons.Default.AccountBalance),
                            Triple("daybook", "Daybook", Icons.Default.Book),
                            Triple("reports", "Reports", Icons.AutoMirrored.Filled.TrendingUp),
                            Triple("website", "Store", Icons.Default.Storefront)
                        )
                        val enabled = state.settings.enabledModules
                        if (enabled.isEmpty()) all else all.filter { it.first in enabled }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickShortcuts) { (route, label, icon) ->
                            Surface(
                                onClick = { onNavigate(route) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier.testTag("dash_shortcut_$route")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                    Text(label, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            // Hero Net Profit Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(listOf(TealDark, TealPrimary, TealBright))
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "NET PROFIT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TealMint
                                    )
                                    Text(
                                        text = FormatUtils.inr(s.netProfit),
                                        style = MaterialTheme.typography.displaySmall,
                                        color = Color.White
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = "${s.collectionRatePct}% Collected",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HeroStatChip(
                                    label = "Revenue",
                                    value = FormatUtils.inr(s.totalRevenue),
                                    modifier = Modifier.weight(1f)
                                )
                                HeroStatChip(
                                    label = "Expenses",
                                    value = FormatUtils.inr(s.totalExpenses),
                                    modifier = Modifier.weight(1f)
                                )
                                HeroStatChip(
                                    label = "Today",
                                    value = FormatUtils.inr(s.todayRevenue),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Outstanding Strip
            item {
                Card(
                    onClick = { onNavigate("records") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (s.unpaidAmount > 0) WarningSoft else EmeraldSoft
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_outstanding_strip")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = if (s.unpaidAmount > 0) WarningAmber else EmeraldAccent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Outstanding Receivables",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF0B1B22)
                            )
                            Text(
                                text = if (s.unpaidAmount > 0) "Tap to collect or send reminders" else "All invoices settled!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                        }
                        Text(
                            text = FormatUtils.inr(s.unpaidAmount),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (s.unpaidAmount > 0) WarningAmber else EmeraldAccent
                        )
                    }
                }
            }

            // Business Health Score + Daily Target
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        onClick = { showHealthDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_health_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                HealthScoreRing(score = s.healthScore)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Health Score", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(s.healthLabel, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                                }
                            }
                            Text(
                                text = "Tap for 4-pillar breakdown",
                                style = MaterialTheme.typography.bodySmall,
                                color = TealPrimary
                            )
                        }
                    }

                    val target = state.settings.dailyTarget.coerceAtLeast(1.0)
                    val progress by animateFloatAsState(
                        targetValue = (s.todayRevenue / target).toFloat().coerceIn(0f, 1f),
                        animationSpec = tween(700),
                        label = "target_progress"
                    )
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daily Target",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { showTargetDialog = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("edit_daily_target_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Daily Target", modifier = Modifier.size(16.dp), tint = TealPrimary)
                                }
                            }
                            Text(
                                text = "${FormatUtils.inr(s.todayRevenue)} / ${FormatUtils.inr(target)}",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                color = EmeraldAccent,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp))
                            )
                            Text(
                                text = "${(progress * 100).toInt()}% of today's goal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // GST Summary + AI Insights Shortcut
            item {
                Card(
                    onClick = { onNavigate("gst") },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_gst_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = TealPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GST Liability Summary", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            ErpStatusBadge("Net: ${FormatUtils.inr(s.netGstPayable)}", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MiniMetric("CGST", FormatUtils.inr(s.cgstOutput))
                            MiniMetric("SGST", FormatUtils.inr(s.sgstOutput))
                            MiniMetric("IGST", FormatUtils.inr(s.igstOutput))
                            MiniMetric("ITC Credit", FormatUtils.inr(s.itcCredit))
                        }
                    }
                }
            }

            // Cash Flow Forecast
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = EmeraldAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cash Flow Forecast", style = MaterialTheme.typography.titleMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ForecastPill("7 Days", FormatUtils.inr(s.forecast7d), Modifier.weight(1f))
                            ForecastPill("15 Days", FormatUtils.inr(s.forecast15d), Modifier.weight(1f))
                            ForecastPill("30 Days", FormatUtils.inr(s.forecast30d), Modifier.weight(1f))
                        }
                    }
                }
            }

            // 7-Day Revenue vs Expense Chart
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("7-Day Revenue vs Expense", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LegendDot("Rev", TealPrimary)
                                LegendDot("Exp", DangerRed)
                            }
                        }
                        RevenueExpenseBarChart(points = s.last7Days)
                    }
                }
            }

            // Payment Distribution Donut + Business Stats
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Payment Mode Split", style = MaterialTheme.typography.titleMedium)
                        PaymentDonutChart(paymentTotals = s.paymentTotals)
                    }
                }
            }

            // Quick Stats Strip + Receivable Aging
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile("Avg Bill", FormatUtils.inr(s.avgInvoiceValue), IndigoAccent, Modifier.weight(1f))
                    StatTile("Units Sold", s.totalUnitsSold.toInt().toString(), TealPrimary, Modifier.weight(1f))
                    StatTile("Clients", state.clients.count { !it.isDeleted }.toString(), EmeraldAccent, Modifier.weight(1f))
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Receivable Aging", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { onNavigate("reports") }) { Text("Full Report") }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MiniMetric("0–30 Days", FormatUtils.inr(s.aging0To30))
                            MiniMetric("31–60 Days", FormatUtils.inr(s.aging31To60))
                            MiniMetric("60+ Days", FormatUtils.inr(s.aging60Plus))
                        }
                    }
                }
            }

            // AI Advisor CTA Banner
            item {
                Card(
                    onClick = { onNavigate("ai-report") },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_ai_report_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Business Advisor", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Generate live profit, margin & GST insights powered by Gemini", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open AI Report", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Low Stock Alerts
            if (lowStockItems.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = DangerRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Low Stock Alerts (${lowStockItems.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                TextButton(onClick = { onNavigate("inventory") }) { Text("Restock") }
                            }
                            lowStockItems.take(4).forEach { prod ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(prod.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    ErpStatusBadge("${prod.stock.toInt()} ${prod.unit}", DangerSoft, DangerRed)
                                }
                            }
                        }
                    }
                }
            }

            // Top Sellers
            if (topSellers.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Top Selling Items", style = MaterialTheme.typography.titleMedium)
                            topSellers.forEach { (name, qty, rev) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("$name (${qty.toInt()})", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(FormatUtils.inr(rev), style = MaterialTheme.typography.titleSmall, color = TealPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // Recent Activity
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent Invoices", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { onNavigate("records") }) { Text("View All") }
                        }
                        if (activeInvoices.isEmpty()) {
                            Text(
                                text = "No invoices yet. Tap + to create your first GST bill!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            activeInvoices.take(5).forEach { inv ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigate("records") }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${inv.invoiceNumber} • ${inv.clientName.ifBlank { "Walk-in" }}", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(inv.dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(FormatUtils.inr(inv.grandTotal), style = MaterialTheme.typography.titleSmall)
                                        val isPaid = inv.paymentStatus == "paid"
                                        ErpStatusBadge(
                                            inv.paymentStatus.uppercase(),
                                            if (isPaid) SuccessSoft else WarningSoft,
                                            if (isPaid) SuccessGreen else WarningAmber
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

    if (showHealthDialog) {
        AlertDialog(
            onDismissRequest = { showHealthDialog = false },
            title = { Text("Business Health: ${s.healthScore}/100") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(s.healthLabel, style = MaterialTheme.typography.titleSmall, color = TealPrimary)
                    s.healthBreakdown.forEach { metric ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(metric.label, style = MaterialTheme.typography.labelLarge)
                                Text("${metric.score}/100 (${metric.detail})", style = MaterialTheme.typography.bodySmall)
                            }
                            LinearProgressIndicator(
                                progress = { metric.score / 100f },
                                color = TealPrimary,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp))
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHealthDialog = false }) { Text("Close") }
            }
        )
    }

    if (showTargetDialog) {
        var targetInput by remember { mutableStateOf(state.settings.dailyTarget.toLong().toString()) }
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text("Set Daily Revenue Target") },
            text = {
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("Target Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = targetInput.toDoubleOrNull()
                    if (v != null && v >= 0) onUpdateDailyTarget(v)
                    showTargetDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HeroStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
            Text(value, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ForecastPill(horizon: String, amount: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(horizon, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(amount, style = MaterialTheme.typography.titleSmall, color = TealPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
