package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BkashTransaction
import com.example.ui.theme.BkashPink
import com.example.ui.theme.BkashPinkDark
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueContainer
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.viewmodel.BkashUiState
import com.example.ui.viewmodel.BkashViewModel
import com.example.ui.viewmodel.TransactionFilter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BkashSyncScreen(
    viewModel: BkashViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSimulateMenu by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    // Reactive check for READ_SMS runtime permission
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Refresh permission state whenever app resumes (e.g. after returning from settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSmsPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Runtime Permission Request Launcher for READ_SMS
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
        if (isGranted) {
            showPermissionRationaleDialog = false
            scope.launch {
                snackbarHostState.showSnackbar("SMS permission granted. Scanning inbox for bKash messages...")
            }
            viewModel.scanSmsInbox()
        } else {
            showPermissionRationaleDialog = true
            scope.launch {
                snackbarHostState.showSnackbar("READ_SMS permission is required to read bKash transactions.")
            }
        }
    }

    // Handle feedback notifications from ViewModel
    LaunchedEffect(uiState.userFeedbackMessage) {
        uiState.userFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    fun handleScanRequest() {
        if (hasSmsPermission) {
            viewModel.scanSmsInbox()
        } else {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    Scaffold(
        modifier = modifier.testTag("bkash_main_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "bK",
                                color = BkashPink,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "bKash SMS Sync",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (hasSmsPermission) "SMS Access Active" else "SMS Permission Needed",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                },
                actions = {
                    // Simulate bKash SMS button
                    Box {
                        IconButton(
                            onClick = { showSimulateMenu = true },
                            modifier = Modifier.testTag("simulate_sms_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Simulate SMS Message",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showSimulateMenu,
                            onDismissRequest = { showSimulateMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Simulate Cash In SMS (+৳ 1,500)") },
                                onClick = {
                                    showSimulateMenu = false
                                    viewModel.simulateBkashSms(BkashTransaction.TYPE_CASH_IN)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simulate Received Money SMS (+৳ 1,200)") },
                                onClick = {
                                    showSimulateMenu = false
                                    viewModel.simulateBkashSms(BkashTransaction.TYPE_RECEIVED_MONEY)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simulate Payment SMS (-৳ 450)") },
                                onClick = {
                                    showSimulateMenu = false
                                    viewModel.simulateBkashSms(BkashTransaction.TYPE_PAYMENT)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simulate Cash Out SMS (-৳ 2,000)") },
                                onClick = {
                                    showSimulateMenu = false
                                    viewModel.simulateBkashSms(BkashTransaction.TYPE_CASH_OUT)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simulate Mobile Recharge (-৳ 100)") },
                                onClick = {
                                    showSimulateMenu = false
                                    viewModel.simulateBkashSms(BkashTransaction.TYPE_RECHARGE)
                                }
                            )
                        }
                    }

                    // Settings sheet button
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "API Settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BkashPink,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("transaction_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Permission Banner if permission not granted
            if (!hasSmsPermission) {
                item {
                    PermissionBannerCard(
                        onRequestPermission = {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        }
                    )
                }
            }

            // Financial Summary Card
            item {
                StatsOverviewCard(uiState = uiState, hasSmsPermission = hasSmsPermission)
            }

            // Primary Action Buttons (Scan SMS & Sync to API)
            item {
                ActionControlsBar(
                    isScanning = uiState.isScanningSms,
                    isSyncing = uiState.isSyncingApi,
                    pendingCount = uiState.pendingCount,
                    hasSmsPermission = hasSmsPermission,
                    onScanClick = { handleScanRequest() },
                    onSyncApiClick = { viewModel.syncPendingToApi() }
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input"),
                    placeholder = { Text("Search by TrxID, mobile number, type...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Text("✕", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Filter Chips
            item {
                FilterChipsRow(
                    selectedFilter = uiState.filter,
                    pendingCount = uiState.pendingCount,
                    onSelectFilter = { viewModel.setFilter(it) }
                )
            }

            // Empty state or transaction list
            if (uiState.transactions.isEmpty()) {
                item {
                    EmptyTransactionsCard(
                        isScanning = uiState.isScanningSms,
                        hasSmsPermission = hasSmsPermission,
                        onScanClick = { handleScanRequest() },
                        onSimulateClick = { viewModel.simulateBkashSms(BkashTransaction.TYPE_CASH_IN) }
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "bKash Messages (${uiState.transactions.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.pendingCount > 0) {
                            Text(
                                text = "${uiState.pendingCount} pending API",
                                style = MaterialTheme.typography.labelSmall,
                                color = PendingAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                items(
                    items = uiState.transactions,
                    key = { it.trxId }
                ) { transaction ->
                    TransactionItemCard(
                        transaction = transaction,
                        onSyncToApi = { viewModel.syncSingleTransaction(transaction) },
                        onDelete = { viewModel.deleteTransaction(transaction) }
                    )
                }
            }
        }
    }

    // Permission Rationale / Settings Dialog
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "SMS Permission",
                    tint = BkashPink,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("SMS Permission Required") },
            text = {
                Text(
                    "To read and process your incoming and inbox bKash payment alerts, Android requires the READ_SMS permission. Please grant permission in App Settings or allow it when prompted."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationaleDialog = false
                        openAppSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BkashPink),
                    modifier = Modifier.testTag("open_settings_permission_button")
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionRationaleDialog = false
                        smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                    },
                    modifier = Modifier.testTag("retry_permission_button")
                ) {
                    Text("Try Again")
                }
            }
        )
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        ApiSettingsBottomSheet(
            currentUrl = uiState.apiUrl,
            currentKey = uiState.apiKey,
            autoSync = uiState.autoSyncEnabled,
            pingStatus = uiState.testPingStatus,
            isTesting = uiState.isTestingPing,
            onSave = { url, key, auto ->
                viewModel.saveApiSettings(url, key, auto)
                showSettingsSheet = false
            },
            onTestPing = { url -> viewModel.testApiPing(url) },
            onClearAll = {
                showClearConfirmDialog = true
            },
            onDismiss = { showSettingsSheet = false }
        )
    }

    // Confirm Clear All Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All bKash Data?") },
            text = { Text("This will permanently remove all stored transactions from the local database.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllTransactions()
                        showClearConfirmDialog = false
                        showSettingsSheet = false
                    },
                    modifier = Modifier.testTag("confirm_clear_button")
                ) {
                    Text("Clear All", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PermissionBannerCard(
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_banner_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = InfoBlueContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Permission needed",
                    tint = InfoBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SMS Access Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = InfoBlue
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Grant READ_SMS permission to read your bKash transactions, save them to the database, and sync them to your API.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                    modifier = Modifier.testTag("grant_sms_permission_button")
                ) {
                    Text("Allow SMS Access", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatsOverviewCard(
    uiState: BkashUiState,
    hasSmsPermission: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stats_overview_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Financial Summary",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (hasSmsPermission) SuccessGreenContainer else PendingAmberContainer
                ) {
                    Text(
                        text = if (hasSmsPermission) "● SMS Active" else "● SMS Not Permitted",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasSmsPermission) SuccessGreen else PendingAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Inflow
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(SuccessGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Received",
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Received",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("৳ %,.2f", uiState.totalReceived),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }

                // Outflow
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ErrorRedContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Sent",
                                tint = ErrorRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sent / Paid",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("৳ %,.2f", uiState.totalSpent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Sync Status Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.pendingCount == 0 && uiState.totalCount > 0)
                            Icons.Default.CloudDone
                        else
                            Icons.Default.CloudSync,
                        contentDescription = "API Status",
                        tint = if (uiState.pendingCount == 0 && uiState.totalCount > 0) SuccessGreen else PendingAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.pendingCount == 0 && uiState.totalCount > 0)
                            "All synced to API"
                        else
                            "${uiState.pendingCount} pending API sync",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${uiState.totalCount} in Database",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionControlsBar(
    isScanning: Boolean,
    isSyncing: Boolean,
    pendingCount: Int,
    hasSmsPermission: Boolean,
    onScanClick: () -> Unit,
    onSyncApiClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onScanClick,
            enabled = !isScanning,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("scan_inbox_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BkashPink,
                contentColor = Color.White
            )
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scanning...", maxLines = 1)
            } else {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = "Scan SMS",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (hasSmsPermission) "Scan SMS Inbox" else "Grant & Scan SMS", maxLines = 1)
            }
        }

        OutlinedButton(
            onClick = onSyncApiClick,
            enabled = !isSyncing,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("sync_api_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = BkashPinkDark
            )
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = BkashPink,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sending...", maxLines = 1)
            } else {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Send to API",
                    modifier = Modifier.size(18.dp),
                    tint = BkashPink
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync API ($pendingCount)", maxLines = 1)
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: TransactionFilter,
    pendingCount: Int,
    onSelectFilter: (TransactionFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedFilter == TransactionFilter.ALL,
                onClick = { onSelectFilter(TransactionFilter.ALL) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BkashPink,
                    selectedLabelColor = Color.White
                )
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == TransactionFilter.INCOMING,
                onClick = { onSelectFilter(TransactionFilter.INCOMING) },
                label = { Text("Received / Inflow") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BkashPink,
                    selectedLabelColor = Color.White
                )
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == TransactionFilter.OUTGOING,
                onClick = { onSelectFilter(TransactionFilter.OUTGOING) },
                label = { Text("Spent / Outflow") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BkashPink,
                    selectedLabelColor = Color.White
                )
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == TransactionFilter.PENDING_API,
                onClick = { onSelectFilter(TransactionFilter.PENDING_API) },
                label = { Text("Pending API ($pendingCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BkashPink,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun TransactionItemCard(
    transaction: BkashTransaction,
    onSyncToApi: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val iconVector = when (transaction.type) {
        BkashTransaction.TYPE_CASH_IN, BkashTransaction.TYPE_RECEIVED_MONEY -> Icons.Default.ArrowDownward
        BkashTransaction.TYPE_PAYMENT -> Icons.Default.ShoppingBag
        BkashTransaction.TYPE_CASH_OUT -> Icons.Default.ArrowUpward
        BkashTransaction.TYPE_RECHARGE -> Icons.Default.PhoneAndroid
        else -> Icons.Default.Sync
    }

    val iconBgColor = if (transaction.isIncoming) SuccessGreenContainer else ErrorRedContainer
    val iconColor = if (transaction.isIncoming) SuccessGreen else ErrorRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(200))
            .clickable { expanded = !expanded }
            .testTag("transaction_item_${transaction.trxId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = transaction.displayType,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Type and Counterparty
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.displayType,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (transaction.isIncoming) "From ${transaction.counterparty}" else "To ${transaction.counterparty}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = transaction.formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (transaction.isIncoming) "+" else "-") + transaction.formattedAmount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.isIncoming) SuccessGreen else ErrorRed
                    )

                    // API Status Chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (transaction.apiSyncStatus) {
                            BkashTransaction.SYNC_STATUS_SYNCED -> SuccessGreenContainer
                            BkashTransaction.SYNC_STATUS_FAILED -> ErrorRedContainer
                            else -> PendingAmberContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (transaction.apiSyncStatus) {
                                    BkashTransaction.SYNC_STATUS_SYNCED -> Icons.Default.CheckCircle
                                    BkashTransaction.SYNC_STATUS_FAILED -> Icons.Default.Error
                                    else -> Icons.Default.HourglassEmpty
                                },
                                contentDescription = transaction.apiSyncStatus,
                                tint = when (transaction.apiSyncStatus) {
                                    BkashTransaction.SYNC_STATUS_SYNCED -> SuccessGreen
                                    BkashTransaction.SYNC_STATUS_FAILED -> ErrorRed
                                    else -> PendingAmber
                                },
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (transaction.apiSyncStatus) {
                                    BkashTransaction.SYNC_STATUS_SYNCED -> "API Synced"
                                    BkashTransaction.SYNC_STATUS_FAILED -> "API Failed"
                                    else -> "API Pending"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (transaction.apiSyncStatus) {
                                    BkashTransaction.SYNC_STATUS_SYNCED -> SuccessGreen
                                    BkashTransaction.SYNC_STATUS_FAILED -> ErrorRed
                                    else -> PendingAmber
                                }
                            )
                        }
                    }
                }
            }

            // TrxID & Balance Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TrxID with copy action
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("TrxID", transaction.trxId)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "TrxID ${transaction.trxId} copied!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TrxID: ${transaction.trxId}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy TrxID",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (transaction.balance > 0) {
                    Text(
                        text = "Balance: ${transaction.formattedBalance}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Show less" else "Show more",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Expandable details (Raw SMS body & API message)
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Raw SMS Message:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = transaction.rawBody,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (transaction.reference != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reference: ${transaction.reference}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (transaction.fee > 0) {
                        Text(
                            text = "Fee: ${transaction.formattedFee}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (transaction.apiResponseMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "API Response: ${transaction.apiResponseMessage}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (transaction.apiSyncStatus == BkashTransaction.SYNC_STATUS_SYNCED) SuccessGreen else ErrorRed
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Actions: Send to API & Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_transaction_${transaction.trxId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete transaction",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Button(
                            onClick = onSyncToApi,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BkashPink)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send to API",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (transaction.apiSyncStatus == BkashTransaction.SYNC_STATUS_SYNCED) "Resend to API" else "Send to API",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsCard(
    isScanning: Boolean,
    hasSmsPermission: Boolean,
    onScanClick: () -> Unit,
    onSimulateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .testTag("empty_state_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(BkashPink.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = "No SMS yet",
                    tint = BkashPink,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No bKash Messages Found Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasSmsPermission)
                    "Tap 'Scan SMS Inbox' to read bKash transaction alerts from your phone messages, or tap 'Simulate Test SMS' to test parsing and API syncing immediately."
                else
                    "Grant SMS permission to read your incoming bKash messages, or use the simulator to test database storage and API sync.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onScanClick,
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(containerColor = BkashPink)
                ) {
                    Text(if (hasSmsPermission) "Scan SMS Inbox" else "Grant & Scan SMS")
                }

                OutlinedButton(onClick = onSimulateClick) {
                    Text("Simulate Test SMS")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiSettingsBottomSheet(
    currentUrl: String,
    currentKey: String,
    autoSync: Boolean,
    pingStatus: String?,
    isTesting: Boolean,
    onSave: (url: String, key: String, autoSync: Boolean) -> Unit,
    onTestPing: (url: String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputUrl by remember { mutableStateOf(currentUrl) }
    var inputKey by remember { mutableStateOf(currentKey) }
    var autoSyncEnabled by remember { mutableStateOf(autoSync) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "API & Sync Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Set the remote API endpoint where bKash transaction JSON will be sent via HTTP POST.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // API URL Input
            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                label = { Text("API Webhook URL") },
                placeholder = { Text("https://your-domain.com/api/bkash") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_url_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Optional Auth Token / API Key
            OutlinedTextField(
                value = inputKey,
                onValueChange = { inputKey = it },
                label = { Text("Bearer Token / API Key (Optional)") },
                placeholder = { Text("e.g. secret_token_abc123") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Test Ping Button & Result
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { onTestPing(inputUrl) },
                    enabled = !isTesting && inputUrl.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testing...", fontSize = 12.sp)
                    } else {
                        Text("Test Connection", fontSize = 12.sp)
                    }
                }

                if (pingStatus != null) {
                    Text(
                        text = pingStatus,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // Auto-sync switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-sync to API",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Automatically dispatch HTTP POST when new bKash SMS arrives.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoSyncEnabled,
                    onCheckedChange = { autoSyncEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BkashPink,
                        checkedTrackColor = BkashPinkDark.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save settings button
            Button(
                onClick = { onSave(inputUrl, inputKey, autoSyncEnabled) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_api_settings_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BkashPink)
            ) {
                Text("Save Configuration")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clear database data
            OutlinedButton(
                onClick = onClearAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clear_all_data_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear All Database Records")
            }
        }
    }
}
