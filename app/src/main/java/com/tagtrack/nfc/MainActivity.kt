package com.tagtrack.nfc

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
        val repository = ItemRepository(database.itemDao(), applicationContext)

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
                                                nfcTagIO.readTagUid(tag).fold(
                                                    onSuccess = { uid ->
                                                        viewModel.handleNfcScanResult(uid)
                                                    },
                                                    onFailure = { error ->
                                                        viewModel.showSnackbar(error.message ?: "Failed to read tag UID")
                                                    }
                                                )
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

                                        // I-reset ang filters bago pumunta sa Inventory
                                        BottomNavTab.INVENTORY -> {
                                            viewModel.updateSelectedStatus("All")
                                            viewModel.updateSelectedCategory("All Categories")
                                            viewModel.updateSelectedCondition("All") // BAGONG DAGDAG
                                            viewModel.updateSearchQuery("")
                                            viewModel.navigateTo(Screen.InventoryScreen)
                                        }

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
                                    onNavigateToInventory = { filterType ->
                                        // 1. I-reset muna lahat ng filters
                                        viewModel.updateSelectedCategory("All Categories")
                                        viewModel.updateSelectedStatus("All")
                                        viewModel.updateSelectedCondition("All")
                                        viewModel.updateSearchQuery("")

                                        // 2. I-set ang tamang filter base sa pinindot
                                        when (filterType) {
                                            "Available", "Rented", "Unavailable" -> {
                                                viewModel.updateSelectedStatus(filterType)
                                            }
                                            "Attention Required" -> {
                                                viewModel.updateSelectedCondition("Attention Required")
                                            }
                                            else -> {
                                                // "See All" - walang filter
                                            }
                                        }

                                        // 3. Lumipat sa Inventory Screen
                                        viewModel.navigateTo(Screen.InventoryScreen)
                                    },
                                    onNavigateToScanner = { viewModel.navigateTo(Screen.ScannerScreen(ScanMode.READ)) },
                                    onNavigateToForm = { viewModel.navigateTo(Screen.ItemFormScreen(isEditing = false)) },
                                    onItemClick = { item -> viewModel.navigateTo(Screen.ItemDetailsScreen(item.nfcCode)) }
                                )
                            }
                            is Screen.InventoryScreen -> {
                                InventoryScreen(
                                    items = uiState.filteredItems,
                                    searchQuery = uiState.searchQuery,
                                    selectedCategory = uiState.selectedCategory,
                                    availableCategories = uiState.availableCategories,
                                    onCategorySelected = { viewModel.updateSelectedCategory(it) },
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
                                    onDeleteClick = { item -> viewModel.deleteItem(item); viewModel.navigateTo(Screen.InventoryScreen) }
                                )
                            }
                            is Screen.ItemFormScreen -> {
                                ItemFormScreen(
                                    itemToEdit = screen.item,
                                    prefilledNfcCode = screen.prefilledNfcCode,
                                    defaultUnqCode = 500009,
                                    onBackClick = { viewModel.navigateTo(Screen.InventoryScreen) },
                                    onSaveItem = { item ->
                                        if (screen.isEditing) {
                                            viewModel.updateItem(item)
                                        } else {
                                            viewModel.insertItem(item)
                                        }
                                    }
                                )
                            }
                            is Screen.ScannerScreen -> {
                                ScannerScreen(
                                    scanMode = screen.mode,
                                    onBackClick = { viewModel.navigateTo(Screen.HomeScreen) },
                                    onSimulateScan = { scannedUid ->
                                        viewModel.handleNfcScanResult(scannedUid)
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
                                        viewModel.navigateTo(Screen.ItemFormScreen(isEditing = false, prefilledNfcCode = code))
                                    },
                                    onScanAgain = { viewModel.navigateTo(Screen.ScannerScreen(ScanMode.READ)) }
                                )
                            }
                            is Screen.SettingsScreen -> {
                                SettingsScreen(
                                    totalRecords = uiState.totalRecords,
                                    totalUnits = uiState.totalUnits,
                                    onBackClick = { viewModel.navigateTo(Screen.HomeScreen) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}