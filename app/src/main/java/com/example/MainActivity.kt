package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.configureAppCheck
import com.example.data.model.BusinessSettings
import com.example.data.model.ErpInvoice
import com.example.data.repository.AiInsightsRepository
import com.example.data.repository.ErpRepository
import com.example.ui.components.AriaVoiceSheet
import com.example.ui.components.ConfettiOverlay
import com.example.ui.components.ErpBottomNavBar
import com.example.ui.components.ErpNavigationRail
import com.example.ui.components.ErpTopBar
import com.example.ui.components.ExpandableQuickFab
import com.example.ui.components.GlobalSearchSheet
import com.example.ui.components.MoreModulesSheet
import com.example.ui.components.NotificationsSheet
import com.example.ui.components.PinLockGate
import com.example.ui.screens.AccountSetupScreen
import com.example.ui.screens.AiBusinessReportScreen
import com.example.ui.screens.AppointmentsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BranchesScreen
import com.example.ui.screens.BusinessToolsScreen
import com.example.ui.screens.CaPortalScreen
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DataModuleScreen
import com.example.ui.screens.DaybookScreen
import com.example.ui.screens.EmployeesScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.FinancialReportsScreen
import com.example.ui.screens.GstCenterScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.InvoiceBuilderScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.PurchaseOrdersScreen
import com.example.ui.screens.RecordsScreen
import com.example.ui.screens.RecycleBinScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WebsiteStoreScreen
import com.example.ui.screens.signOutFromErp
import com.example.ui.theme.MyBusinessErpTheme
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.ErpViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.erpPrefs by preferencesDataStore(name = "erp_local_prefs")
private val KEY_PIN_CODE = stringPreferencesKey("erp_pin_code")

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            val user by remember { Firebase.auth.authStateFlow() }
                .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
            var pendingOnboarding by remember { mutableStateOf<BusinessSettings?>(null) }

            val currentUser = user
            if (currentUser == null) {
                MyBusinessErpTheme(darkModeSetting = "light") {
                    AuthScreen(onAuthSuccess = { draft -> pendingOnboarding = draft })
                }
            } else {
                key(currentUser.uid) {
                    MainErpApp(
                        user = currentUser,
                        pendingOnboarding = pendingOnboarding,
                        onClearPendingOnboarding = { pendingOnboarding = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainErpApp(
    user: FirebaseUser,
    pendingOnboarding: BusinessSettings?,
    onClearPendingOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: ErpViewModel = viewModel(
        key = user.uid,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ErpViewModel(
                    repository = ErpRepository(context.applicationContext),
                    aiRepository = AiInsightsRepository(),
                    currentUserId = user.uid
                ) as T
            }
        }
    )

    LaunchedEffect(pendingOnboarding, user.uid) {
        if (pendingOnboarding != null) {
            viewModel.saveSettings(
                pendingOnboarding.copy(
                    userId = user.uid,
                    email = pendingOnboarding.email.ifBlank { user.email.orEmpty() },
                    ownerName = pendingOnboarding.ownerName.ifBlank { user.displayName.orEmpty() }
                )
            )
            onClearPendingOnboarding()
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dashFilter by viewModel.dashFilter.collectAsStateWithLifecycle()
    val aiReportState by viewModel.aiReportState.collectAsStateWithLifecycle()
    val ariaMessages by viewModel.ariaMessages.collectAsStateWithLifecycle()
    val ariaBusy by viewModel.ariaBusy.collectAsStateWithLifecycle()

    val savedPin by remember {
        context.erpPrefs.data.map { it[KEY_PIN_CODE] ?: "1234" }
    }.collectAsStateWithLifecycle(initialValue = "1234")

    var currentRoute by rememberSaveable { mutableStateOf("dashboard") }
    var editingInvoice by remember { mutableStateOf<ErpInvoice?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showAriaSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var confettiTrigger by remember { mutableIntStateOf(0) }
    var pinUnlocked by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { evt ->
            if (evt.triggerConfetti) confettiTrigger++
            snackbarHostState.showSnackbar(evt.message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.pendingNavRoute.collect { route ->
            showAriaSheet = false
            editingInvoice = null
            currentRoute = route
        }
    }

    val primaryRoutes = remember { setOf("dashboard", "records", "inventory", "clients") }
    BackHandler(enabled = currentRoute != "dashboard") {
        editingInvoice = null
        currentRoute = "dashboard"
    }

    val trashCount = remember(uiState.invoices, uiState.products, uiState.clients, uiState.expenses) {
        uiState.invoices.count { it.isDeleted } +
            uiState.products.count { it.isDeleted } +
            uiState.clients.count { it.isDeleted } +
            uiState.expenses.count { it.isDeleted }
    }

    val userInitials = remember(user.displayName, user.email) {
        val raw = user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "ER"
        raw.split(" ", "@").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    }

    MyBusinessErpTheme(darkModeSetting = uiState.settings.darkMode) {
        if (!uiState.isLoading && uiState.needsAccountSetup && pendingOnboarding == null) {
            AccountSetupScreen(
                initialName = user.displayName.orEmpty(),
                initialEmail = user.email.orEmpty(),
                onSaveProfile = { settings ->
                    viewModel.saveSettings(settings.copy(userId = user.uid))
                    confettiTrigger++
                },
                onSignOut = { signOutFromErp(credentialManager, scope) }
            )
        } else if (uiState.settings.pinLockEnabled && !pinUnlocked) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PinLockGate(expectedPin = savedPin, onUnlocked = { pinUnlocked = true })
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 700.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (useRail) {
                        ErpNavigationRail(
                            currentRoute = currentRoute,
                            onSelectRoute = { target ->
                                if (target == "more") showMoreSheet = true
                                else {
                                    editingInvoice = null
                                    currentRoute = target
                                }
                            }
                        )
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            ErpTopBar(
                                title = routeTitle(currentRoute),
                                subtitle = uiState.settings.businessName,
                                canNavigateBack = currentRoute !in primaryRoutes,
                                notificationCount = uiState.notifications.size,
                                trashCount = trashCount,
                                userInitials = userInitials,
                                onBack = {
                                    editingInvoice = null
                                    currentRoute = "dashboard"
                                },
                                onOpenNotifications = { showNotificationsSheet = true },
                                onOpenTrash = { currentRoute = "recycle-bin" },
                                onOpenSettings = { currentRoute = "settings" }
                            )
                        },
                        bottomBar = {
                            if (!useRail) {
                                ErpBottomNavBar(
                                    currentRoute = currentRoute,
                                    onSelectRoute = { target ->
                                        fabExpanded = false
                                        if (target == "more") showMoreSheet = true
                                        else {
                                            editingInvoice = null
                                            currentRoute = target
                                        }
                                    }
                                )
                            }
                        },
                        floatingActionButton = {
                            if (currentRoute == "dashboard" || currentRoute == "records") {
                                ExpandableQuickFab(
                                    expanded = fabExpanded,
                                    onToggleExpand = { fabExpanded = !fabExpanded },
                                    onOpenAria = {
                                        fabExpanded = false
                                        showAriaSheet = true
                                    },
                                    onNewInvoice = {
                                        fabExpanded = false
                                        editingInvoice = null
                                        currentRoute = "invoice"
                                    },
                                    onOpenPos = {
                                        fabExpanded = false
                                        currentRoute = "pos"
                                    },
                                    onAddExpense = {
                                        fabExpanded = false
                                        currentRoute = "expenses"
                                    },
                                    onAddProduct = {
                                        fabExpanded = false
                                        currentRoute = "inventory"
                                    },
                                    onAddClient = {
                                        fabExpanded = false
                                        currentRoute = "clients"
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (uiState.isLoading) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = TealPrimary)
                                }
                            } else {
                                when (currentRoute) {
                                    "dashboard" -> DashboardScreen(
                                        state = uiState,
                                        dashFilter = dashFilter,
                                        onSelectFilter = viewModel::setDashFilter,
                                        onOpenSearch = { showSearchSheet = true },
                                        onNavigate = {
                                            editingInvoice = null
                                            currentRoute = it
                                        },
                                        onUpdateDailyTarget = { target ->
                                            viewModel.saveSettings(uiState.settings.copy(dailyTarget = target))
                                        }
                                    )
                                    "records" -> RecordsScreen(
                                        state = uiState,
                                        onNewInvoice = {
                                            editingInvoice = null
                                            currentRoute = "invoice"
                                        },
                                        onEditInvoice = { inv ->
                                            editingInvoice = inv
                                            currentRoute = "invoice"
                                        },
                                        onMarkPaid = viewModel::markInvoicePaid,
                                        onSoftDelete = { viewModel.softDeleteInvoice(it, true) },
                                        onSharePdfError = { viewModel.emitToast(it, isError = true) }
                                    )
                                    "invoice" -> InvoiceBuilderScreen(
                                        state = uiState,
                                        editingInvoice = editingInvoice,
                                        onSaveInvoice = { inv, incCounter, decStock ->
                                            viewModel.saveInvoice(inv, incCounter, decStock)
                                            editingInvoice = null
                                            currentRoute = "records"
                                        },
                                        onCancel = {
                                            editingInvoice = null
                                            currentRoute = "records"
                                        }
                                    )
                                    "pos" -> PosScreen(
                                        state = uiState,
                                        onCheckoutInvoice = { inv ->
                                            viewModel.saveInvoice(inv, incrementCounter = true, decrementStock = true)
                                        }
                                    )
                                    "inventory" -> InventoryScreen(
                                        state = uiState,
                                        servicesOnly = false,
                                        onSaveProduct = viewModel::saveProduct,
                                        onAdjustStock = viewModel::adjustProductStock,
                                        onSoftDelete = { viewModel.softDeleteProduct(it, true) },
                                        onMessage = { viewModel.emitToast(it) }
                                    )
                                    "services" -> InventoryScreen(
                                        state = uiState,
                                        servicesOnly = true,
                                        onSaveProduct = viewModel::saveProduct,
                                        onAdjustStock = viewModel::adjustProductStock,
                                        onSoftDelete = { viewModel.softDeleteProduct(it, true) },
                                        onMessage = { viewModel.emitToast(it) }
                                    )
                                    "clients" -> ClientsScreen(
                                        state = uiState,
                                        onSaveClient = viewModel::saveClient,
                                        onSoftDelete = { viewModel.softDeleteClient(it, true) }
                                    )
                                    "daybook" -> DaybookScreen(state = uiState)
                                    "expenses" -> ExpensesScreen(
                                        state = uiState,
                                        onSaveExpense = viewModel::saveExpense,
                                        onSoftDelete = { viewModel.softDeleteExpense(it, true) }
                                    )
                                    "purchase-orders" -> PurchaseOrdersScreen(
                                        state = uiState,
                                        onSavePo = viewModel::savePurchaseOrder,
                                        onReceivePo = viewModel::receivePurchaseOrder
                                    )
                                    "appointments" -> AppointmentsScreen(
                                        state = uiState,
                                        onSaveAppointment = viewModel::saveAppointment,
                                        onConvertToInvoice = { inv ->
                                            viewModel.saveInvoice(inv, incrementCounter = true, decrementStock = false)
                                        }
                                    )
                                    "branches" -> BranchesScreen(
                                        state = uiState,
                                        onSaveBranch = viewModel::saveBranch
                                    )
                                    "employees" -> EmployeesScreen(
                                        state = uiState,
                                        onSaveEmployee = viewModel::saveEmployee
                                    )
                                    "gst" -> GstCenterScreen(
                                        state = uiState,
                                        onExportMessage = { viewModel.emitToast(it) }
                                    )
                                    "reports" -> FinancialReportsScreen(
                                        state = uiState,
                                        onExportMessage = { viewModel.emitToast(it) }
                                    )
                                    "ai-report" -> AiBusinessReportScreen(
                                        aiState = aiReportState,
                                        onGenerate = viewModel::generateAiReport
                                    )
                                    "ca-portal" -> CaPortalScreen(
                                        state = uiState,
                                        onExportMessage = { viewModel.emitToast(it) }
                                    )
                                    "tools" -> BusinessToolsScreen(
                                        state = uiState,
                                        onSaveRecurring = viewModel::saveRecurring,
                                        onRunRecurring = { inv -> viewModel.saveInvoice(inv, incrementCounter = true) },
                                        onSaveWarranty = viewModel::saveWarranty,
                                        onCreateAutoPo = viewModel::savePurchaseOrder
                                    )
                                    "website" -> WebsiteStoreScreen(
                                        state = uiState,
                                        onSaveSettings = viewModel::saveSettings,
                                        onSaveProduct = viewModel::saveProduct,
                                        onSaveCoupon = viewModel::saveCoupon,
                                        onSaveOrder = viewModel::saveOnlineOrder
                                    )
                                    "data-module" -> DataModuleScreen(
                                        state = uiState,
                                        onLoadSampleData = { prods, clis, invs, exps ->
                                            prods.forEach { viewModel.saveProduct(it) }
                                            clis.forEach { viewModel.saveClient(it) }
                                            invs.forEach { viewModel.saveInvoice(it, incrementCounter = true) }
                                            exps.forEach { viewModel.saveExpense(it) }
                                            viewModel.emitToast("Sample ERP dataset loaded!", confetti = true)
                                        },
                                        onMessage = { viewModel.emitToast(it) }
                                    )
                                    "settings" -> SettingsScreen(
                                        state = uiState,
                                        userEmail = user.email ?: user.displayName ?: "Signed in",
                                        pinCode = savedPin,
                                        onSaveSettings = viewModel::saveSettings,
                                        onSavePinCode = { newPin ->
                                            scope.launch {
                                                context.erpPrefs.edit { it[KEY_PIN_CODE] = newPin }
                                            }
                                        },
                                        onOpenTrash = { currentRoute = "recycle-bin" },
                                        onSignOut = {
                                            signOutFromErp(credentialManager, scope)
                                        }
                                    )
                                    "recycle-bin" -> RecycleBinScreen(
                                        state = uiState,
                                        onRestoreInvoice = { viewModel.softDeleteInvoice(it, false) },
                                        onPurgeInvoice = { viewModel.permanentlyDelete("invoices", it) },
                                        onRestoreProduct = { viewModel.softDeleteProduct(it, false) },
                                        onPurgeProduct = { viewModel.permanentlyDelete("products", it) },
                                        onRestoreClient = { viewModel.softDeleteClient(it, false) },
                                        onPurgeClient = { viewModel.permanentlyDelete("clients", it) },
                                        onRestoreExpense = { viewModel.softDeleteExpense(it, false) },
                                        onPurgeExpense = { viewModel.permanentlyDelete("expenses", it) }
                                    )
                                }
                            }
                        }
                    }
                }

                ConfettiOverlay(
                    triggerKey = confettiTrigger,
                    onFinished = { confettiTrigger = 0 }
                )
            }
        }
    }

    if (showMoreSheet) {
        MoreModulesSheet(
            onDismiss = { showMoreSheet = false },
            onNavigate = { target ->
                editingInvoice = null
                currentRoute = target
            }
        )
    }

    if (showAriaSheet) {
        AriaVoiceSheet(
            messages = ariaMessages,
            isBusy = ariaBusy,
            onSendQuery = viewModel::handleAriaCommand,
            onDismiss = { showAriaSheet = false }
        )
    }

    if (showSearchSheet) {
        GlobalSearchSheet(
            state = uiState,
            onDismiss = { showSearchSheet = false },
            onNavigate = { target ->
                editingInvoice = null
                currentRoute = target
            }
        )
    }

    if (showNotificationsSheet) {
        NotificationsSheet(
            notifications = uiState.notifications,
            onDismiss = { showNotificationsSheet = false },
            onNavigate = { target ->
                editingInvoice = null
                currentRoute = target
            }
        )
    }
}

private fun routeTitle(route: String): String = when (route) {
    "dashboard" -> "My Business ERP"
    "records" -> "Sales & Records"
    "invoice" -> "Invoice Builder"
    "pos" -> "Quick POS Billing"
    "inventory" -> "Inventory & Stock"
    "services" -> "Services Catalog"
    "clients" -> "Clients CRM"
    "daybook" -> "Daybook Ledger"
    "expenses" -> "Expenses & ITC"
    "purchase-orders" -> "Purchase Orders"
    "appointments" -> "Appointments"
    "branches" -> "Branches"
    "employees" -> "Staff & Permissions"
    "gst" -> "GST Filing Center"
    "reports" -> "Financial Reports"
    "ai-report" -> "AI Business Advisor"
    "ca-portal" -> "CA Portal"
    "tools" -> "Business Tools Hub"
    "website" -> "Online Storefront"
    "data-module" -> "Backup & Import"
    "settings" -> "Account & Settings"
    "recycle-bin" -> "Recycle Bin"
    else -> "My Business ERP"
}
