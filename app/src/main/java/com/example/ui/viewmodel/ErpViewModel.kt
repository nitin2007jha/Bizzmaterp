package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.FormatUtils
import com.example.data.model.BusinessSettings
import com.example.data.model.ErpAppointment
import com.example.data.model.ErpBranch
import com.example.data.model.ErpClient
import com.example.data.model.ErpCoupon
import com.example.data.model.ErpEmployee
import com.example.data.model.ErpExpense
import com.example.data.model.ErpInvoice
import com.example.data.model.ErpOnlineOrder
import com.example.data.model.ErpProduct
import com.example.data.model.ErpPurchaseOrder
import com.example.data.model.ErpRecurringInvoice
import com.example.data.model.ErpWarranty
import com.example.data.model.InvoiceLineItem
import com.example.data.repository.AiInsightsRepository
import com.example.data.repository.ErpRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DailyChartPoint(val label: String, val revenue: Double, val expense: Double)

data class HealthMetric(val label: String, val score: Int, val detail: String)

data class ErpDashboardStats(
    val totalRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val unpaidAmount: Double = 0.0,
    val todayRevenue: Double = 0.0,
    val healthScore: Int = 78,
    val healthLabel: String = "Good Business Health",
    val healthBreakdown: List<HealthMetric> = emptyList(),
    val cgstOutput: Double = 0.0,
    val sgstOutput: Double = 0.0,
    val igstOutput: Double = 0.0,
    val itcCredit: Double = 0.0,
    val netGstPayable: Double = 0.0,
    val forecast7d: Double = 0.0,
    val forecast15d: Double = 0.0,
    val forecast30d: Double = 0.0,
    val last7Days: List<DailyChartPoint> = emptyList(),
    val paymentTotals: Map<String, Double> = emptyMap(),
    val aging0To30: Double = 0.0,
    val aging31To60: Double = 0.0,
    val aging60Plus: Double = 0.0,
    val avgInvoiceValue: Double = 0.0,
    val totalUnitsSold: Double = 0.0,
    val collectionRatePct: Int = 100
)

data class ErpNotification(
    val id: String,
    val title: String,
    val body: String,
    val category: String,
    val targetTab: String
)

sealed interface AiReportUiState {
    data object Idle : AiReportUiState
    data object Loading : AiReportUiState
    data class Success(val markdownText: String) : AiReportUiState
    data class Error(val message: String) : AiReportUiState
}

data class AriaMessage(
    val isUser: Boolean,
    val text: String
)

data class ErpUiState(
    val isLoading: Boolean = true,
    val needsAccountSetup: Boolean = false,
    val errorMessage: String? = null,
    val settings: BusinessSettings = BusinessSettings(),
    val invoices: List<ErpInvoice> = emptyList(),
    val products: List<ErpProduct> = emptyList(),
    val clients: List<ErpClient> = emptyList(),
    val expenses: List<ErpExpense> = emptyList(),
    val purchaseOrders: List<ErpPurchaseOrder> = emptyList(),
    val appointments: List<ErpAppointment> = emptyList(),
    val branches: List<ErpBranch> = emptyList(),
    val employees: List<ErpEmployee> = emptyList(),
    val recurring: List<ErpRecurringInvoice> = emptyList(),
    val warranties: List<ErpWarranty> = emptyList(),
    val coupons: List<ErpCoupon> = emptyList(),
    val onlineOrders: List<ErpOnlineOrder> = emptyList(),
    val stats: ErpDashboardStats = ErpDashboardStats(),
    val notifications: List<ErpNotification> = emptyList()
)

data class ToastEvent(val message: String, val isError: Boolean = false, val triggerConfetti: Boolean = false)

class ErpViewModel(
    private val repository: ErpRepository,
    private val aiRepository: AiInsightsRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _dashFilter = MutableStateFlow("all")
    val dashFilter: StateFlow<String> = _dashFilter.asStateFlow()

    private val _toastEvents = MutableSharedFlow<ToastEvent>()
    val toastEvents: SharedFlow<ToastEvent> = _toastEvents.asSharedFlow()

    private val _aiReportState = MutableStateFlow<AiReportUiState>(AiReportUiState.Idle)
    val aiReportState: StateFlow<AiReportUiState> = _aiReportState.asStateFlow()

    private val _ariaMessages = MutableStateFlow(
        listOf(
            AriaMessage(
                isUser = false,
                text = "Namaste! I'm Aria, your Voice ERP Assistant. Ask about today's sales, low stock, GST payable, or say \"Open invoice\" / \"Naya bill\"."
            )
        )
    )
    val ariaMessages: StateFlow<List<AriaMessage>> = _ariaMessages.asStateFlow()

    private val _ariaBusy = MutableStateFlow(false)
    val ariaBusy: StateFlow<Boolean> = _ariaBusy.asStateFlow()

    private val _pendingNavRoute = MutableSharedFlow<String>()
    val pendingNavRoute: SharedFlow<String> = _pendingNavRoute.asSharedFlow()

    private val coreFlow = combine(
        repository.observeSettingsWithExists(currentUserId),
        repository.observeInvoices(currentUserId),
        repository.observeProducts(currentUserId),
        repository.observeClients(currentUserId),
        repository.observeExpenses(currentUserId)
    ) { settingsPair, invoices, products, clients, expenses ->
        CoreBundle(settingsPair.first, settingsPair.second, invoices, products, clients, expenses)
    }

    private val opsFlow = combine(
        repository.observePurchaseOrders(currentUserId),
        repository.observeAppointments(currentUserId),
        repository.observeBranches(currentUserId),
        repository.observeEmployees(currentUserId)
    ) { pos, appts, branches, emps ->
        OpsBundle(pos, appts, branches, emps)
    }

    private val toolsFlow = combine(
        repository.observeRecurring(currentUserId),
        repository.observeWarranties(currentUserId),
        repository.observeCoupons(currentUserId),
        repository.observeOnlineOrders(currentUserId)
    ) { rec, war, coup, ord ->
        ToolsBundle(rec, war, coup, ord)
    }

    val uiState: StateFlow<ErpUiState> = combine(
        coreFlow,
        opsFlow,
        toolsFlow,
        _dashFilter
    ) { core, ops, tools, filter ->
        val stats = computeStats(core.invoices, core.products, core.expenses, filter)
        val notifs = buildNotifications(core.invoices, core.products, tools.orders, ops.appts)
        ErpUiState(
            isLoading = false,
            needsAccountSetup = !core.settingsExists,
            errorMessage = null,
            settings = core.settings,
            invoices = core.invoices.sortedByDescending { it.dateStr },
            products = core.products.sortedBy { it.name.lowercase() },
            clients = core.clients.sortedBy { it.name.lowercase() },
            expenses = core.expenses.sortedByDescending { it.dateStr },
            purchaseOrders = ops.pos.sortedByDescending { it.dateStr },
            appointments = ops.appts.sortedBy { it.dateStr },
            branches = ops.branches,
            employees = ops.employees,
            recurring = tools.rec,
            warranties = tools.war,
            coupons = tools.coup,
            onlineOrders = tools.orders,
            stats = stats,
            notifications = notifs
        )
    }.catch { error ->
        emit(ErpUiState(isLoading = false, errorMessage = error.localizedMessage ?: "Failed to sync ERP data"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ErpUiState(isLoading = true)
    )

    fun setDashFilter(filter: String) {
        _dashFilter.value = filter
    }

    fun emitToast(message: String, isError: Boolean = false, confetti: Boolean = false) {
        viewModelScope.launch {
            _toastEvents.emit(ToastEvent(message, isError, confetti))
        }
    }

    fun saveSettings(settings: BusinessSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
                .onSuccess { emitToast("Settings saved", confetti = false) }
                .onFailure { emitToast(it.localizedMessage ?: "Failed to save settings", isError = true) }
        }
    }

    fun saveInvoice(invoice: ErpInvoice, incrementCounter: Boolean = false, decrementStock: Boolean = false) {
        viewModelScope.launch {
            val isNewFinal = invoice.id.isBlank() && invoice.status == "final"
            repository.saveInvoice(invoice, incrementCounter)
                .onSuccess {
                    if (decrementStock && invoice.status == "final" && invoice.docType == "invoice") {
                        val currentProducts = uiState.value.products
                        invoice.items.forEach { line ->
                            val match = currentProducts.firstOrNull {
                                (line.productId.isNotBlank() && it.id == line.productId) ||
                                    (!it.isService && it.name.equals(line.name, ignoreCase = true))
                            }
                            if (match != null && !match.isService) {
                                val updatedStock = (match.stock - line.qty).coerceAtLeast(0.0)
                                repository.saveProduct(match.copy(stock = updatedStock))
                            }
                        }
                    }
                    emitToast(
                        if (invoice.status == "draft") "Draft saved" else "Invoice ${invoice.invoiceNumber} saved!",
                        confetti = isNewFinal
                    )
                }
                .onFailure { emitToast(it.localizedMessage ?: "Failed to save invoice", isError = true) }
        }
    }

    fun markInvoicePaid(invoice: ErpInvoice) {
        viewModelScope.launch {
            repository.saveInvoice(
                invoice.copy(paymentStatus = "paid", paidAmount = invoice.grandTotal),
                incrementCounter = false
            ).onSuccess {
                emitToast("Marked ${invoice.invoiceNumber} as Paid", confetti = true)
            }.onFailure {
                emitToast(it.localizedMessage ?: "Update failed", isError = true)
            }
        }
    }

    fun softDeleteInvoice(invoice: ErpInvoice, deleted: Boolean = true) {
        viewModelScope.launch {
            repository.saveInvoice(invoice.copy(isDeleted = deleted))
                .onSuccess { emitToast(if (deleted) "Moved to Recycle Bin" else "Invoice restored") }
                .onFailure { emitToast(it.localizedMessage ?: "Action failed", isError = true) }
        }
    }

    fun saveProduct(product: ErpProduct) {
        viewModelScope.launch {
            repository.saveProduct(product)
                .onSuccess { emitToast(if (product.isService) "Service saved" else "Product saved") }
                .onFailure { emitToast(it.localizedMessage ?: "Failed to save item", isError = true) }
        }
    }

    fun adjustProductStock(product: ErpProduct, delta: Double) {
        viewModelScope.launch {
            val next = (product.stock + delta).coerceAtLeast(0.0)
            repository.saveProduct(product.copy(stock = next))
                .onSuccess { emitToast("Stock updated to ${next.toInt()} ${product.unit}") }
                .onFailure { emitToast(it.localizedMessage ?: "Stock update failed", isError = true) }
        }
    }

    fun softDeleteProduct(product: ErpProduct, deleted: Boolean = true) {
        viewModelScope.launch {
            repository.saveProduct(product.copy(isDeleted = deleted))
                .onSuccess { emitToast(if (deleted) "Moved to Recycle Bin" else "Item restored") }
                .onFailure { emitToast(it.localizedMessage ?: "Action failed", isError = true) }
        }
    }

    fun saveClient(client: ErpClient) {
        viewModelScope.launch {
            repository.saveClient(client)
                .onSuccess { emitToast("Client saved") }
                .onFailure { emitToast(it.localizedMessage ?: "Failed to save client", isError = true) }
        }
    }

    fun softDeleteClient(client: ErpClient, deleted: Boolean = true) {
        viewModelScope.launch {
            repository.saveClient(client.copy(isDeleted = deleted))
                .onSuccess { emitToast(if (deleted) "Moved to Recycle Bin" else "Client restored") }
                .onFailure { emitToast(it.localizedMessage ?: "Action failed", isError = true) }
        }
    }

    fun saveExpense(expense: ErpExpense) {
        viewModelScope.launch {
            repository.saveExpense(expense)
                .onSuccess { emitToast("Expense recorded") }
                .onFailure { emitToast(it.localizedMessage ?: "Failed to save expense", isError = true) }
        }
    }

    fun softDeleteExpense(expense: ErpExpense, deleted: Boolean = true) {
        viewModelScope.launch {
            repository.saveExpense(expense.copy(isDeleted = deleted))
                .onSuccess { emitToast(if (deleted) "Moved to Recycle Bin" else "Expense restored") }
                .onFailure { emitToast(it.localizedMessage ?: "Action failed", isError = true) }
        }
    }

    fun savePurchaseOrder(po: ErpPurchaseOrder) {
        viewModelScope.launch {
            repository.savePurchaseOrder(po)
                .onSuccess { emitToast("Purchase Order ${po.poNumber} saved") }
                .onFailure { emitToast(it.localizedMessage ?: "PO save failed", isError = true) }
        }
    }

    fun receivePurchaseOrder(po: ErpPurchaseOrder) {
        viewModelScope.launch {
            repository.savePurchaseOrder(po.copy(status = "received"))
                .onSuccess {
                    val prods = uiState.value.products
                    po.items.forEach { line ->
                        val p = prods.firstOrNull {
                            (line.productId.isNotBlank() && it.id == line.productId) ||
                                it.name.equals(line.name, ignoreCase = true)
                        }
                        if (p != null) {
                            repository.saveProduct(p.copy(stock = p.stock + line.qty))
                        }
                    }
                    emitToast("PO ${po.poNumber} received & stock updated!", confetti = true)
                }
                .onFailure { emitToast(it.localizedMessage ?: "Failed to receive PO", isError = true) }
        }
    }

    fun saveAppointment(appointment: ErpAppointment) {
        viewModelScope.launch {
            repository.saveAppointment(appointment)
                .onSuccess { emitToast("Appointment saved") }
                .onFailure { emitToast(it.localizedMessage ?: "Failed to save appointment", isError = true) }
        }
    }

    fun saveBranch(branch: ErpBranch) {
        viewModelScope.launch {
            repository.saveBranch(branch)
                .onSuccess { emitToast("Branch saved") }
                .onFailure { emitToast(it.localizedMessage ?: "Failed to save branch", isError = true) }
        }
    }

    fun saveEmployee(employee: ErpEmployee) {
        viewModelScope.launch {
            repository.saveEmployee(employee)
                .onSuccess { emitToast("Employee saved") }
                .onFailure { emitToast(it.localizedMessage ?: "Failed to save employee", isError = true) }
        }
    }

    fun saveRecurring(item: ErpRecurringInvoice) {
        viewModelScope.launch {
            repository.saveRecurring(item)
                .onSuccess { emitToast("Recurring schedule saved") }
                .onFailure { emitToast(it.localizedMessage ?: "Failed to save schedule", isError = true) }
        }
    }

    fun saveWarranty(warranty: ErpWarranty) {
        viewModelScope.launch {
            repository.saveWarranty(warranty)
                .onSuccess { emitToast("Warranty record saved") }
                .onFailure { emitToast(it.localizedMessage ?: "Failed to save warranty", isError = true) }
        }
    }

    fun saveCoupon(coupon: ErpCoupon) {
        viewModelScope.launch {
            repository.saveCoupon(coupon)
                .onSuccess { emitToast("Coupon ${coupon.code} saved") }
                .onFailure { emitToast(it.localizedMessage ?: "Failed to save coupon", isError = true) }
        }
    }

    fun saveOnlineOrder(order: ErpOnlineOrder) {
        viewModelScope.launch {
            repository.saveOnlineOrder(order)
                .onSuccess { emitToast("Order status updated") }
                .onFailure { emitToast(it.localizedMessage ?: "Failed to update order", isError = true) }
        }
    }

    fun permanentlyDelete(collectionName: String, docId: String) {
        viewModelScope.launch {
            repository.deleteDocument(collectionName, docId)
                .onSuccess { emitToast("Permanently deleted") }
                .onFailure { emitToast(it.localizedMessage ?: "Delete failed", isError = true) }
        }
    }

    fun generateAiReport(periodLabel: String) {
        val snap = uiState.value
        val s = snap.stats
        val topProducts = snap.invoices
            .filter { !it.isDeleted && it.status == "final" }
            .flatMap { it.items }
            .groupBy { it.name }
            .mapValues { (_, list) -> list.sumOf { it.qty * it.rate } }
            .entries.sortedByDescending { it.value }.take(5)
            .joinToString(", ") { "${it.key}: ${FormatUtils.inr(it.value)}" }
            .ifBlank { "No sales recorded yet" }

        val expBreakdown = snap.expenses
            .filter { !it.isDeleted }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }.take(5)
            .joinToString(", ") { "${it.key}: ${FormatUtils.inr(it.value)}" }
            .ifBlank { "No expenses recorded yet" }

        val lowStock = snap.products.count { !it.isDeleted && !it.isService && it.stock <= it.lowStockThreshold }
        val finalCount = snap.invoices.count { !it.isDeleted && it.status == "final" }

        viewModelScope.launch {
            _aiReportState.value = AiReportUiState.Loading
            aiRepository.generateBusinessReport(
                businessName = snap.settings.businessName,
                periodLabel = periodLabel,
                totalRevenue = s.totalRevenue,
                totalExpenses = s.totalExpenses,
                netProfit = s.netProfit,
                invoiceCount = finalCount,
                unpaidAmount = s.unpaidAmount,
                lowStockCount = lowStock,
                topProductsSummary = topProducts,
                expenseBreakdown = expBreakdown
            ).onSuccess { report ->
                _aiReportState.value = AiReportUiState.Success(report)
            }.onFailure { err ->
                _aiReportState.value = AiReportUiState.Error(err.localizedMessage ?: "Failed to generate AI report")
            }
        }
    }

    fun handleAriaCommand(rawQuery: String) {
        val q = rawQuery.trim()
        if (q.isBlank()) return
        val lower = q.lowercase()
        _ariaMessages.value = _ariaMessages.value + AriaMessage(isUser = true, text = q)

        val snap = uiState.value
        val navTarget: Pair<String, String>? = when {
            listOf("new invoice", "naya bill", "create invoice", "bill banao", "invoice").any { it in lower } ->
                "invoice" to "Opening Invoice Builder."
            listOf("dashboard", "home", "dash").any { it in lower } ->
                "dashboard" to "Opening Dashboard."
            listOf("inventory", "stock", "products", "product").any { it in lower && "low" !in lower } ->
                "inventory" to "Opening Inventory."
            listOf("client", "customer", "grahak").any { it in lower } ->
                "clients" to "Opening Clients CRM."
            listOf("expense", "kharch").any { it in lower && "total" !in lower && "kitna" !in lower } ->
                "expenses" to "Opening Expenses."
            listOf("record", "history", "bills").any { it in lower } ->
                "records" to "Opening Sales Records."
            listOf("gst", "tax", "gstr").any { it in lower && "payable" !in lower && "kitna" !in lower } ->
                "gst" to "Opening GST Filing Center."
            listOf("daybook", "cashbook").any { it in lower } ->
                "daybook" to "Opening Daybook."
            listOf("pos", "quick bill").any { it in lower } ->
                "pos" to "Opening Quick POS mode."
            listOf("ai report", "report").any { it in lower } ->
                "ai-report" to "Opening AI Business Report."
            listOf("setting", "profile").any { it in lower } ->
                "settings" to "Opening Settings."
            else -> null
        }

        if (navTarget != null) {
            _ariaMessages.value = _ariaMessages.value + AriaMessage(isUser = false, text = navTarget.second)
            viewModelScope.launch { _pendingNavRoute.emit(navTarget.first) }
            return
        }

        val instantStat: String? = when {
            listOf("today", "aaj", "sales", "revenue").any { it in lower } ->
                "Today's sales are ${FormatUtils.inr(snap.stats.todayRevenue)}. Overall revenue is ${FormatUtils.inr(snap.stats.totalRevenue)} with net profit ${FormatUtils.inr(snap.stats.netProfit)}."
            listOf("low stock", "kam stock").any { it in lower } -> {
                val low = snap.products.filter { !it.isDeleted && !it.isService && it.stock <= it.lowStockThreshold }
                if (low.isEmpty()) "All inventory items are well stocked right now."
                else "${low.size} items are low on stock: ${low.take(4).joinToString { "${it.name} (${it.stock.toInt()})" }}."
            }
            listOf("unpaid", "pending", "udhar", "receivable").any { it in lower } ->
                "Total unpaid receivables stand at ${FormatUtils.inr(snap.stats.unpaidAmount)}."
            listOf("gst payable", "gst kitna").any { it in lower } ->
                "Net GST payable is ${FormatUtils.inr(snap.stats.netGstPayable)} after ${FormatUtils.inr(snap.stats.itcCredit)} Input Tax Credit."
            else -> null
        }

        if (instantStat != null) {
            _ariaMessages.value = _ariaMessages.value + AriaMessage(isUser = false, text = instantStat)
            return
        }

        // Route open-ended questions to Gemini via Firebase AI Logic
        viewModelScope.launch {
            _ariaBusy.value = true
            val contextSummary = """
                Business: ${snap.settings.businessName}
                Total Revenue: ${FormatUtils.inr(snap.stats.totalRevenue)}
                Today Revenue: ${FormatUtils.inr(snap.stats.todayRevenue)}
                Total Expenses: ${FormatUtils.inr(snap.stats.totalExpenses)}
                Net Profit: ${FormatUtils.inr(snap.stats.netProfit)}
                Unpaid Receivables: ${FormatUtils.inr(snap.stats.unpaidAmount)}
                Net GST Payable: ${FormatUtils.inr(snap.stats.netGstPayable)}
                Products Count: ${snap.products.count { !it.isDeleted && !it.isService }}
                Clients Count: ${snap.clients.count { !it.isDeleted }}
                Low Stock Count: ${snap.products.count { !it.isDeleted && !it.isService && it.stock <= it.lowStockThreshold }}
            """.trimIndent()
            aiRepository.askAriaAssistant(q, contextSummary)
                .onSuccess { reply ->
                    _ariaMessages.value = _ariaMessages.value + AriaMessage(isUser = false, text = reply)
                }
                .onFailure { err ->
                    _ariaMessages.value = _ariaMessages.value + AriaMessage(
                        isUser = false,
                        text = err.localizedMessage ?: "Sorry, I couldn't connect to Gemini right now."
                    )
                }
            _ariaBusy.value = false
        }
    }

    private fun computeStats(
        allInvoices: List<ErpInvoice>,
        allProducts: List<ErpProduct>,
        allExpenses: List<ErpExpense>,
        filter: String
    ): ErpDashboardStats {
        val today = FormatUtils.todayStr()
        val thisMonth = FormatUtils.currentMonthStr()
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        val lastMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)

        fun matchesPeriod(dateStr: String): Boolean = when (filter) {
            "this_month" -> dateStr.startsWith(thisMonth)
            "last_month" -> dateStr.startsWith(lastMonth)
            else -> true
        }

        val activeInv = allInvoices.filter { !it.isDeleted && it.status == "final" && it.docType == "invoice" }
        val filteredInv = activeInv.filter { matchesPeriod(it.dateStr) }
        val filteredExp = allExpenses.filter { !it.isDeleted && it.approvalStatus != "rejected" && matchesPeriod(it.dateStr) }

        val totalRev = filteredInv.sumOf { it.grandTotal }
        val totalExp = filteredExp.sumOf { it.amount }
        val netProfit = totalRev - totalExp
        val unpaid = activeInv.filter { it.paymentStatus != "paid" }.sumOf { (it.grandTotal - it.paidAmount).coerceAtLeast(0.0) }
        val todayRev = activeInv.filter { it.dateStr == today }.sumOf { it.grandTotal }

        val cgst = filteredInv.sumOf { it.cgstTotal }
        val sgst = filteredInv.sumOf { it.sgstTotal }
        val igst = filteredInv.sumOf { it.igstTotal }
        val itc = filteredExp.sumOf { it.gstAmount }
        val netGst = ((cgst + sgst + igst) - itc).coerceAtLeast(0.0)

        val collected = activeInv.sumOf {
            when (it.paymentStatus) {
                "paid" -> it.grandTotal
                "partial" -> it.paidAmount
                else -> 0.0
            }
        }
        val totalAll = activeInv.sumOf { it.grandTotal }
        val collectionPct = if (totalAll > 0) ((collected / totalAll) * 100).toInt().coerceIn(0, 100) else 100
        val marginPct = if (totalRev > 0) ((netProfit / totalRev) * 100).toInt().coerceIn(0, 100) else 75
        val goods = allProducts.filter { !it.isDeleted && !it.isService }
        val lowCount = goods.count { it.stock <= it.lowStockThreshold }
        val stockScore = if (goods.isEmpty()) 85 else (((goods.size - lowCount).toDouble() / goods.size) * 100).toInt().coerceIn(20, 100)
        val expScore = if (totalRev <= 0) 80 else (100 - ((totalExp / totalRev) * 100).toInt()).coerceIn(20, 100)
        val health = ((marginPct + collectionPct + stockScore + expScore) / 4).coerceIn(15, 100)
        val healthLabel = when {
            health >= 80 -> "Excellent Business Health"
            health >= 60 -> "Healthy & Growing"
            health >= 40 -> "Needs Attention"
            else -> "Action Required"
        }

        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val shortFmt = SimpleDateFormat("EEE", Locale.US)
        val chartPoints = (6 downTo 0).map { offset ->
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -offset)
            val dStr = dayFmt.format(c.time)
            val lbl = shortFmt.format(c.time)
            val r = activeInv.filter { it.dateStr == dStr }.sumOf { it.grandTotal }
            val e = allExpenses.filter { !it.isDeleted && it.dateStr == dStr }.sumOf { it.amount }
            DailyChartPoint(lbl, r, e)
        }

        val payMap = mutableMapOf("UPI" to 0.0, "Cash" to 0.0, "Credit" to 0.0, "Bank" to 0.0)
        filteredInv.forEach { inv ->
            when (inv.paymentMode) {
                "upi" -> payMap["UPI"] = (payMap["UPI"] ?: 0.0) + inv.grandTotal
                "cash" -> payMap["Cash"] = (payMap["Cash"] ?: 0.0) + inv.grandTotal
                "credit" -> payMap["Credit"] = (payMap["Credit"] ?: 0.0) + inv.grandTotal
                "bank" -> payMap["Bank"] = (payMap["Bank"] ?: 0.0) + inv.grandTotal
                "split" -> {
                    payMap["UPI"] = (payMap["UPI"] ?: 0.0) + inv.upiSplit
                    payMap["Cash"] = (payMap["Cash"] ?: 0.0) + inv.cashSplit
                    payMap["Credit"] = (payMap["Credit"] ?: 0.0) + inv.creditSplit
                }
            }
        }

        var a0 = 0.0
        var a31 = 0.0
        var a60 = 0.0
        val nowMs = System.currentTimeMillis()
        activeInv.filter { it.paymentStatus != "paid" }.forEach { inv ->
            val amt = (inv.grandTotal - inv.paidAmount).coerceAtLeast(0.0)
            val invMs = runCatching { dayFmt.parse(inv.dateStr)?.time }.getOrNull() ?: nowMs
            val days = ((nowMs - invMs) / (1000L * 60 * 60 * 24)).toInt()
            when {
                days <= 30 -> a0 += amt
                days <= 60 -> a31 += amt
                else -> a60 += amt
            }
        }

        val units = filteredInv.sumOf { inv -> inv.items.sumOf { it.qty } }
        val avgInv = if (filteredInv.isNotEmpty()) totalRev / filteredInv.size else 0.0

        return ErpDashboardStats(
            totalRevenue = totalRev,
            totalExpenses = totalExp,
            netProfit = netProfit,
            unpaidAmount = unpaid,
            todayRevenue = todayRev,
            healthScore = health,
            healthLabel = healthLabel,
            healthBreakdown = listOf(
                HealthMetric("Profitability", marginPct, "$marginPct% margin"),
                HealthMetric("Collection", collectionPct, "$collectionPct% collected"),
                HealthMetric("Stock Health", stockScore, "$lowCount low items"),
                HealthMetric("Expense Control", expScore, "Ratio ${100 - expScore}%")
            ),
            cgstOutput = cgst,
            sgstOutput = sgst,
            igstOutput = igst,
            itcCredit = itc,
            netGstPayable = netGst,
            forecast7d = unpaid * 0.45 + todayRev * 7.0,
            forecast15d = unpaid * 0.72 + todayRev * 15.0,
            forecast30d = unpaid + todayRev * 30.0,
            last7Days = chartPoints,
            paymentTotals = payMap,
            aging0To30 = a0,
            aging31To60 = a31,
            aging60Plus = a60,
            avgInvoiceValue = avgInv,
            totalUnitsSold = units,
            collectionRatePct = collectionPct
        )
    }

    private fun buildNotifications(
        invoices: List<ErpInvoice>,
        products: List<ErpProduct>,
        orders: List<ErpOnlineOrder>,
        appointments: List<ErpAppointment>
    ): List<ErpNotification> {
        val list = mutableListOf<ErpNotification>()
        val low = products.filter { !it.isDeleted && !it.isService && it.stock <= it.lowStockThreshold }
        if (low.isNotEmpty()) {
            list += ErpNotification(
                id = "low_stock",
                title = "${low.size} Low Stock Alert(s)",
                body = "${low.first().name} and ${ (low.size - 1).coerceAtLeast(0) } others need restock.",
                category = "Inventory",
                targetTab = "inventory"
            )
        }
        val unpaid = invoices.filter { !it.isDeleted && it.status == "final" && it.paymentStatus != "paid" }
        if (unpaid.isNotEmpty()) {
            list += ErpNotification(
                id = "unpaid_inv",
                title = "${unpaid.size} Pending Payment(s)",
                body = "Total receivable ${FormatUtils.inr(unpaid.sumOf { it.grandTotal - it.paidAmount })}.",
                category = "Receivables",
                targetTab = "records"
            )
        }
        val pendingOrd = orders.filter { it.status == "pending" }
        if (pendingOrd.isNotEmpty()) {
            list += ErpNotification(
                id = "online_orders",
                title = "${pendingOrd.size} New Online Order(s)",
                body = "Review incoming storefront orders.",
                category = "Store",
                targetTab = "website"
            )
        }
        val todayAppts = appointments.filter { it.status == "scheduled" && it.dateStr == FormatUtils.todayStr() }
        if (todayAppts.isNotEmpty()) {
            list += ErpNotification(
                id = "today_appts",
                title = "${todayAppts.size} Appointment(s) Today",
                body = "Next: ${todayAppts.first().clientName} at ${todayAppts.first().timeStr}.",
                category = "Schedule",
                targetTab = "appointments"
            )
        }
        return list
    }

    private data class CoreBundle(
        val settings: BusinessSettings,
        val settingsExists: Boolean,
        val invoices: List<ErpInvoice>,
        val products: List<ErpProduct>,
        val clients: List<ErpClient>,
        val expenses: List<ErpExpense>
    )

    private data class OpsBundle(
        val pos: List<ErpPurchaseOrder>,
        val appts: List<ErpAppointment>,
        val branches: List<ErpBranch>,
        val employees: List<ErpEmployee>
    )

    private data class ToolsBundle(
        val rec: List<ErpRecurringInvoice>,
        val war: List<ErpWarranty>,
        val coup: List<ErpCoupon>,
        val orders: List<ErpOnlineOrder>
    )
}
