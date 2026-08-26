package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppPermission
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class ScreenRoute {
    MAIN,
    EXPENSES,
    STAFF,
    BUSINESS_BRANCHES,
    AI_ASSISTANT,
    BACKUP_SYNC,
    LOGIN,
    REGISTER_STAFF
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: AmarDokanViewModel
) {
    val isFirstRun by viewModel.isFirstRun.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val currentBranch by viewModel.currentBranch.collectAsState()
    val syncStatus by viewModel.syncState.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currentBusinessId by viewModel.currentBusinessId.collectAsState()
    val currentBranchId by viewModel.currentBranchId.collectAsState()
    val activeDestination by viewModel.activeDestination.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreenRoute by remember { mutableStateOf(ScreenRoute.MAIN) }
    var showNotificationCenter by remember { mutableStateOf(false) }

    // Notification Permission Request (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showSnackbar("বিজ্ঞপ্তি চালু হয়েছে")
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Quick Action dialog states
    var showQuickAddProduct by remember { mutableStateOf(false) }
    var showQuickAddCustomer by remember { mutableStateOf(false) }
    var showQuickAddExpense by remember { mutableStateOf(false) }

    // Invoice View Dialog
    val activeInvoiceSale by viewModel.selectedSaleForInvoice.collectAsState()
    val activeInvoiceItems by viewModel.invoiceSaleItems.collectAsState()
    var showInvoiceDialog by remember { mutableStateOf(false) }

    // Show snackbar message reactively
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnackbar()
        }
    }

    if (isFirstRun == null) {
        // Loading Splash State
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    if (isFirstRun == true) {
        // First Run Setup Screen
        SetupScreen(viewModel = viewModel) {
            viewModel.checkFirstRun()
        }
        return
    }

    if (currentUser == null) {
        // Login Screen
        if (currentScreenRoute == ScreenRoute.REGISTER_STAFF) {
            StaffManagementScreen(viewModel = viewModel, onBack = { currentScreenRoute = ScreenRoute.LOGIN })
        } else {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { currentScreenRoute = ScreenRoute.MAIN },
                onNavigateToRegisterStaff = { currentScreenRoute = ScreenRoute.REGISTER_STAFF }
            )
        }
        return
    }

    // Modal Navigation Drawer for Full Features
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(310.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(20.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentBusiness?.name ?: "আমার দোকান",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "🏢 ${currentBranch?.name ?: "প্রধান শাখা"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary) {
                            Text(
                                text = "${currentUser?.name} (${currentUser?.role?.getDisplayNameBn()})",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!currentUser?.email.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📧 ${currentUser?.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }

                Divider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Store, contentDescription = null) },
                        label = { Text("হোম ড্যাশবোর্ড") },
                        selected = currentScreenRoute == ScreenRoute.MAIN && activeDestination == AppDestination.HOME,
                        onClick = {
                            viewModel.activeDestination.value = AppDestination.HOME
                            currentScreenRoute = ScreenRoute.MAIN
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                        label = { Text("স্টক ইনভেন্টরি") },
                        selected = currentScreenRoute == ScreenRoute.MAIN && activeDestination == AppDestination.STOCK,
                        onClick = {
                            viewModel.activeDestination.value = AppDestination.STOCK
                            currentScreenRoute = ScreenRoute.MAIN
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                        label = { Text("পয়েন্ট অব সেল (POS)") },
                        selected = currentScreenRoute == ScreenRoute.MAIN && activeDestination == AppDestination.POS,
                        onClick = {
                            viewModel.activeDestination.value = AppDestination.POS
                            currentScreenRoute = ScreenRoute.MAIN
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.People, contentDescription = null) },
                        label = { Text("কাস্টমার ও বাকি খাতা") },
                        selected = currentScreenRoute == ScreenRoute.MAIN && activeDestination == AppDestination.CUSTOMERS,
                        onClick = {
                            viewModel.activeDestination.value = AppDestination.CUSTOMERS
                            currentScreenRoute = ScreenRoute.MAIN
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                        label = { Text("বিক্রয় ও লাভ রিপোর্ট") },
                        selected = currentScreenRoute == ScreenRoute.MAIN && activeDestination == AppDestination.REPORTS,
                        onClick = {
                            viewModel.activeDestination.value = AppDestination.REPORTS
                            currentScreenRoute = ScreenRoute.MAIN
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 6.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                        label = { Text("দোকানের খরচ (Expenses)") },
                        selected = currentScreenRoute == ScreenRoute.EXPENSES,
                        onClick = {
                            currentScreenRoute = ScreenRoute.EXPENSES
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    if (viewModel.hasPermission(AppPermission.MANAGE_STAFF)) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            label = { Text("স্টাফ ও পারমিশন") },
                            selected = currentScreenRoute == ScreenRoute.STAFF,
                            onClick = {
                                currentScreenRoute = ScreenRoute.STAFF
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    if (viewModel.hasPermission(AppPermission.MANAGE_BRANCH)) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Apartment, contentDescription = null) },
                            label = { Text("ব্যবসা ও শাখা সুইচ") },
                            selected = currentScreenRoute == ScreenRoute.BUSINESS_BRANCHES,
                            onClick = {
                                currentScreenRoute = ScreenRoute.BUSINESS_BRANCHES
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    NavigationDrawerItem(
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationCount > 0) {
                                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                                            Text("$unreadNotificationCount", color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null)
                            }
                        },
                        label = { Text("বিজ্ঞপ্তি ও অ্যালার্ট") },
                        selected = false,
                        onClick = {
                            showNotificationCenter = true
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        label = { Text("এআই বিজনেস সহকারী") },
                        selected = currentScreenRoute == ScreenRoute.AI_ASSISTANT,
                        onClick = {
                            currentScreenRoute = ScreenRoute.AI_ASSISTANT
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.CloudSync, contentDescription = null) },
                        label = { Text("ক্লাউড সিঙ্ক ও ব্যাকআপ") },
                        selected = currentScreenRoute == ScreenRoute.BACKUP_SYNC,
                        onClick = {
                            currentScreenRoute = ScreenRoute.BACKUP_SYNC
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 6.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = DangerRed) },
                        label = { Text("লগআউট", color = DangerRed) },
                        selected = false,
                        onClick = {
                            viewModel.logout()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (currentScreenRoute == ScreenRoute.MAIN) {
                    DokanTopBar(
                        currentBusiness = currentBusiness,
                        currentBranch = currentBranch,
                        currentUser = currentUser,
                        syncStatus = syncStatus,
                        pendingSyncCount = pendingSyncCount,
                        unreadNotificationCount = unreadNotificationCount,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onBranchClick = { currentScreenRoute = ScreenRoute.BUSINESS_BRANCHES },
                        onAiClick = { currentScreenRoute = ScreenRoute.AI_ASSISTANT },
                        onSyncClick = { currentScreenRoute = ScreenRoute.BACKUP_SYNC },
                        onNotificationClick = { showNotificationCenter = true }
                    )
                }
            },
            bottomBar = {
                if (currentScreenRoute == ScreenRoute.MAIN) {
                    DokanBottomBar(
                        currentDestination = activeDestination,
                        onNavigate = { dest -> viewModel.activeDestination.value = dest }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Crossfade(targetState = currentScreenRoute, label = "screen_crossfade") { route ->
                    when (route) {
                        ScreenRoute.MAIN -> {
                            when (activeDestination) {
                                AppDestination.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigate = { dest -> viewModel.activeDestination.value = dest },
                                    onQuickAddProduct = { showQuickAddProduct = true },
                                    onQuickAddCustomer = { showQuickAddCustomer = true },
                                    onQuickAddExpense = { showQuickAddExpense = true },
                                    onQuickCollectDue = { viewModel.activeDestination.value = AppDestination.CUSTOMERS },
                                    onViewSaleInvoice = { sale ->
                                        viewModel.loadInvoice(sale)
                                        showInvoiceDialog = true
                                    },
                                    onOpenAiAssistant = { currentScreenRoute = ScreenRoute.AI_ASSISTANT }
                                )
                                AppDestination.STOCK -> StockScreen(viewModel = viewModel)
                                AppDestination.POS -> PosScreen(viewModel = viewModel)
                                AppDestination.CUSTOMERS -> CustomerScreen(viewModel = viewModel)
                                AppDestination.REPORTS -> ReportsScreen(viewModel = viewModel)
                            }
                        }
                        ScreenRoute.EXPENSES -> ExpenseScreen(viewModel = viewModel, onBack = { currentScreenRoute = ScreenRoute.MAIN })
                        ScreenRoute.STAFF -> StaffManagementScreen(viewModel = viewModel, onBack = { currentScreenRoute = ScreenRoute.MAIN })
                        ScreenRoute.BUSINESS_BRANCHES -> BusinessBranchScreen(viewModel = viewModel, onBack = { currentScreenRoute = ScreenRoute.MAIN })
                        ScreenRoute.AI_ASSISTANT -> AiAssistantScreen(viewModel = viewModel, onBack = { currentScreenRoute = ScreenRoute.MAIN })
                        ScreenRoute.BACKUP_SYNC -> BackupSyncScreen(viewModel = viewModel, onBack = { currentScreenRoute = ScreenRoute.MAIN })
                        else -> {}
                    }
                }
            }
        }
    }

    // Quick Add Product Dialog
    if (showQuickAddProduct) {
        AddEditProductDialog(
            categories = categories,
            businessId = currentBusinessId,
            branchId = currentBranchId,
            onDismiss = { showQuickAddProduct = false },
            onSave = { prod ->
                viewModel.saveProduct(prod) {
                    showQuickAddProduct = false
                }
            }
        )
    }

    // Quick Add Customer Dialog
    if (showQuickAddCustomer) {
        AddEditCustomerDialog(
            businessId = currentBusinessId,
            branchId = currentBranchId,
            onDismiss = { showQuickAddCustomer = false },
            onSave = { cust ->
                viewModel.saveCustomer(cust) {
                    showQuickAddCustomer = false
                }
            }
        )
    }

    // Quick Add Expense Dialog
    if (showQuickAddExpense) {
        AddExpenseDialog(
            onDismiss = { showQuickAddExpense = false },
            onConfirm = { title, cat, amount, note ->
                viewModel.addExpense(title, cat, amount, note) {
                    showQuickAddExpense = false
                }
            }
        )
    }

    // Invoice View Dialog
    if (showInvoiceDialog && activeInvoiceSale != null) {
        InvoiceDialog(
            sale = activeInvoiceSale!!,
            items = activeInvoiceItems,
            business = currentBusiness,
            branch = currentBranch,
            onDismiss = { showInvoiceDialog = false }
        )
    }

    // Notification Center BottomSheet
    if (showNotificationCenter) {
        NotificationCenterSheet(
            viewModel = viewModel,
            onDismiss = { showNotificationCenter = false },
            onNavigateToDestination = { destination ->
                viewModel.activeDestination.value = destination
                currentScreenRoute = ScreenRoute.MAIN
            },
            onOpenStaffDialog = {
                currentScreenRoute = ScreenRoute.STAFF
            }
        )
    }
}
