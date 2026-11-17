package com.tagtrack.nfc

import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tagtrack.nfc.data.AppDatabase
import com.tagtrack.nfc.data.ItemRepository
import com.tagtrack.nfc.ui.components.BottomNavTab
import com.tagtrack.nfc.ui.components.TagTrackBottomBar
import com.tagtrack.nfc.ui.screens.*
import com.tagtrack.nfc.ui.theme.TagtrackTheme
import com.tagtrack.nfc.ui.viewmodel.ScanMode
import com.tagtrack.nfc.ui.viewmodel.Screen
import com.tagtrack.nfc.nfc.NfcTagIO
import com.tagtrack.nfc.ui.viewmodel.TagTrackViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ItemRepository(database.itemDao())

        setContent {
            TagtrackTheme {
                val viewModel: TagTrackViewModel = viewModel(
                    factory = TagTrackViewModel.provideFactory(repository)
                )
                val uiState by viewModel.uiState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()
                val activity = this@MainActivity
                val nfcTagIO = remember { NfcTagIO(activity) }
                val isOperationInProgress = remember { AtomicBoolean(false) }

                // Overwrite confirmation dialog states
                var showOverwriteDialog by remember { mutableStateOf(false) }
                var pendingWriteTag by remember { mutableStateOf<Tag?>(null) }
                var existingTagCode by remember { mutableStateOf<Int?>(null) }

                // Integrate NFC reader mode lifecycle
                LaunchedEffect(uiState.currentScreen) {
                    if (uiState.currentScreen is Screen.ScannerScreen) {
                        val scanScreen = uiState.currentScreen as Screen.ScannerScreen
                        val mode = scanScreen.mode
                        if (mode != ScanMode.IDLE) {
                            nfcTagIO.enableReaderMode(
                                onTagScanned = { tag ->
                                    if (isOperationInProgress.compareAndSet(false, true)) {
                                        coroutineScope.launch {
                                            try {
                                                when (mode) {
                                                    ScanMode.READ -> {
                                                        nfcTagIO.readTag(tag).fold(
                                                            onSuccess = { nfcCode ->
                                                                viewModel.handleNfcScanResult(nfcCode)
                                                            },
                                                            onFailure = { error ->
                                                                viewModel.showSnackbar(error.message ?: "Failed to read tag")
                                                            }
                                                        )
                                                    }
                                                    ScanMode.WRITE -> {
                                                        val selectedItem = uiState.selectedItem
                                                        if (selectedItem != null) {
                                                            // Check if tag has an existing code
                                                            val readResult = nfcTagIO.readTag(tag)
                                                            if (readResult.isSuccess) {
                                                                val existingCode = readResult.getOrNull()
                                                                if (existingCode != null && existingCode != selectedItem.nfcCode) {
                                                                    pendingWriteTag = tag
                                                                    existingTagCode = existingCode
                                                                    showOverwriteDialog = true
                                                                    return@launch
                                                                }
                                                            }

                                                            nfcTagIO.writeTag(tag, selectedItem.nfcCode, onConfirmOverwrite = { _ -> true }).fold(
                                                                onSuccess = {
                                                                    viewModel.handleNfcWriteOperation(selectedItem)
                                                                },
                                                                onFailure = { error ->
                                                                    viewModel.showSnackbar(error.message ?: "Failed to write tag")
                                                                }
                                                            )
                                                        } else {
                                                            viewModel.showSnackbar("No item selected for writing")
                                                        }
                                                    }
                                                    ScanMode.ERASE -> {
                                                        nfcTagIO.eraseTag(tag).fold(
                                                            onSuccess = {
                                                                viewModel.showSnackbar("NFC Tag successfully erased")
                                                                viewModel.navigateTo(Screen.InventoryScreen)
                                                            },
                                                            onFailure = { error ->
                                                                viewModel.showSnackbar(error.message ?: "Failed to erase tag")
                                                            }
                                                        )
                                                    }
                                                    ScanMode.IDLE -> {}
                                                }
                                            } finally {
                                                isOperationInProgress.set(false)
                                            }
                                        }
                                    }
                                },
                                onError = { errorMsg ->
                                    viewModel.showSnackbar(errorMsg)
                                }
                            )
                        } else {
                            nfcTagIO.disableReaderMode()
                        }
                    } else {
                        nfcTagIO.disableReaderMode()
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        nfcTagIO.disableReaderMode()
                    }
                }

                // Show snackbar messages
                LaunchedEffect(uiState.snackbarMessage) {
                    uiState.snackbarMessage?.let { message ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(message)
                            viewModel.clearSnackbar()
                        }
                    }
                }

                // Overwrite confirmation dialog
                if (showOverwriteDialog && existingTagCode != null && pendingWriteTag != null && uiState.selectedItem != null) {
                    AlertDialog(
                        onDismissRequest = {
                            showOverwriteDialog = false
                            pendingWriteTag = null
                            existingTagCode = null
                        },
                        title = { Text("Overwrite NFC Tag?") },
                        text = { Text("This tag currently contains NFC Code #$existingTagCode. Do you want to overwrite it with NFC Code #${uiState.selectedItem!!.nfcCode}?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val tagToWr = pendingWriteTag!!
                                    val itemToWr = uiState.selectedItem!!
                                    showOverwriteDialog = false
                                    pendingWriteTag = null
                                    existingTagCode = null

                                    coroutineScope.launch {
                                        if (isOperationInProgress.compareAndSet(false, true)) {
                                            try {
                                                nfcTagIO.writeTag(tagToWr, itemToWr.nfcCode, onConfirmOverwrite = { true }).fold(
                                                    onSuccess = {
                                                        viewModel.handleNfcWriteOperation(itemToWr)
                                                    },
                                                    onFailure = { error ->
                                                        viewModel.showSnackbar(error.message ?: "Failed to write tag")
                                                    }
                                                )
                                            } finally {
                                                isOperationInProgress.set(false)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Overwrite")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showOverwriteDialog = false
                                    pendingWriteTag = null
                                    existingTagCode = null
                                    viewModel.showSnackbar("Overwrite cancelled")
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Determine active bottom nav tab based on current screen
                val currentTab = when (uiState.currentScreen) {
                    is Screen.HomeScreen -> BottomNavTab.HOME
                    is Screen.InventoryScreen -> BottomNavTab.INVENTORY
                    is Screen.ScannerScreen, is Screen.ScanResultScreen -> BottomNavTab.SCANNER
                    is Screen.SettingsScreen -> BottomNavTab.SETTINGS
                    else -> BottomNavTab.HOME
                }

                // Handle back press to return home when on sub-screens
                BackHandler(enabled = uiState.currentScreen !is Screen.HomeScreen) {
                    viewModel.navigateTo(Screen.HomeScreen)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (uiState.currentScreen is Screen.HomeScreen ||
                            uiState.currentScreen is Screen.InventoryScreen ||
                            uiState.currentScreen is Screen.ScannerScreen ||
                            uiState.currentScreen is Screen.SettingsScreen
                        ) {
                            TagTrackBottomBar(
                                currentTab = currentTab,
                                onTabSelected = { tab ->
                                    when (tab) {
                                        BottomNavTab.HOME -> viewModel.navigateTo(Screen.HomeScreen)
                                        BottomNavTab.INVENTORY -> viewModel.navigateTo(Screen.InventoryScreen)
                                        BottomNavTab.SCANNER -> viewModel.navigateTo(Screen.ScannerScreen(ScanMode.READ))
                                        BottomNavTab.SETTINGS -> viewModel.navigateTo(Screen.SettingsScreen)
                                    }
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
                        when (val screen = uiState.currentScreen) {
                            is Screen.HomeScreen -> {
                                HomeScreen(
                                    totalUnits = uiState.totalUnits,
                                    totalRecords = uiState.totalRecords,
                                    inventoryItems = uiState.inventoryItems,
                                    onNavigateToInventory = { viewModel.navigateTo(Screen.InventoryScreen) },
                                    onNavigateToScanner = { viewModel.navigateTo(Screen.ScannerScreen(ScanMode.READ)) },
                                    onNavigateToForm = { viewModel.navigateTo(Screen.ItemFormScreen(isEditing = false)) },
                                    onItemClick = { item -> viewModel.navigateTo(Screen.ItemDetailsScreen(item.nfcCode)) }
                                )
                            }
                            is Screen.InventoryScreen -> {
                                InventoryScreen(
                                    items = uiState.filteredItems,
                                    searchQuery = uiState.searchQuery,
                                    onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
                                    onItemClick = { item -> viewModel.navigateTo(Screen.ItemDetailsScreen(item.nfcCode)) },
                                    onAddItemClick = { viewModel.navigateTo(Screen.ItemFormScreen(isEditing = false)) },
                                    onNavigateToScanner = { viewModel.navigateTo(Screen.ScannerScreen(ScanMode.READ)) },
                                    onMergeSimilarItems = { viewModel.mergeSimilarItems() }
                                )
                            }
                            is Screen.ItemDetailsScreen -> {
                                ItemDetailsScreen(
                                    item = uiState.selectedItem,
                                    onBackClick = { viewModel.navigateTo(Screen.InventoryScreen) },
                                    onEditClick = { item -> viewModel.navigateTo(Screen.ItemFormScreen(isEditing = true, item = item)) },
                                    onDeleteClick = { item -> viewModel.deleteItem(item); viewModel.navigateTo(Screen.InventoryScreen) },
                                    onWriteNfcClick = { item ->
                                        viewModel.selectItem(item)
                                        viewModel.navigateTo(Screen.ScannerScreen(ScanMode.WRITE))
                                    },
                                    onEraseNfcClick = { item ->
                                        viewModel.selectItem(item)
                                        viewModel.navigateTo(Screen.ScannerScreen(ScanMode.ERASE))
                                    }
                                )
                            }
                            is Screen.ItemFormScreen -> {
                                ItemFormScreen(
                                    itemToEdit = screen.item,
                                    defaultNfcCode = 100009,
                                    defaultUnqCode = 500009,
                                    onBackClick = { viewModel.navigateTo(Screen.InventoryScreen) },
                                    onSaveItem = { item ->
                                        if (screen.isEditing) {
                                            viewModel.updateItem(item)
                                        } else {
                                            viewModel.insertItem(item)
                                        }
                                    },
                                    onSaveAndWriteNfc = { item ->
                                        if (screen.isEditing) {
                                            viewModel.updateItem(item) {
                                                viewModel.selectItem(item)
                                                viewModel.navigateTo(Screen.ScannerScreen(ScanMode.WRITE))
                                            }
                                        } else {
                                            viewModel.saveAndWriteItem(item) { saved ->
                                                viewModel.selectItem(saved)
                                                viewModel.navigateTo(Screen.ScannerScreen(ScanMode.WRITE))
                                            }
                                        }
                                    }
                                )
                            }
                            is Screen.ScannerScreen -> {
                                ScannerScreen(
                                    scanMode = screen.mode,
                                    onBackClick = { viewModel.navigateTo(Screen.HomeScreen) },
                                    onSimulateScan = { nfcCode ->
                                        if (screen.mode == ScanMode.WRITE && uiState.selectedItem != null) {
                                            viewModel.handleNfcWriteOperation(uiState.selectedItem!!)
                                        } else if (screen.mode == ScanMode.ERASE) {
                                            viewModel.showSnackbar("NFC Tag successfully erased")
                                            viewModel.navigateTo(Screen.InventoryScreen)
                                        } else {
                                            viewModel.handleNfcScanResult(nfcCode)
                                        }
                                    }
                                )
                            }
                            is Screen.ScanResultScreen -> {
                                ScanResultScreen(
                                    scannedNfcCode = screen.scannedNfcCode,
                                    item = screen.item,
                                    onBackClick = { viewModel.navigateTo(Screen.ScannerScreen(ScanMode.READ)) },
                                    onViewDetails = { item -> viewModel.navigateTo(Screen.ItemDetailsScreen(item.nfcCode)) },
                                    onRegisterItem = { code ->
                                        viewModel.navigateTo(Screen.ItemFormScreen(isEditing = false, item = null))
                                    },
                                    onScanAgain = { viewModel.navigateTo(Screen.ScannerScreen(ScanMode.READ)) }
                                )
                            }
                            is Screen.SettingsScreen -> {
                                SettingsScreen(
                                    totalRecords = uiState.totalRecords,
                                    totalUnits = uiState.totalUnits,
                                    onBackClick = { viewModel.navigateTo(Screen.HomeScreen) },
                                    onEraseNfcClick = { viewModel.navigateTo(Screen.ScannerScreen(ScanMode.ERASE)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
