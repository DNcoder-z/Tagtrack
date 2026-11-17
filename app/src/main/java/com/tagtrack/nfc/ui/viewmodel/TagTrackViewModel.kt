package com.tagtrack.nfc.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tagtrack.nfc.data.Item
import com.tagtrack.nfc.data.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface Screen {
    data object HomeScreen : Screen
    data object InventoryScreen : Screen
    data class ItemDetailsScreen(val nfcCode: Int) : Screen
    data class ItemFormScreen(val isEditing: Boolean = false, val item: Item? = null) : Screen
    data class ScannerScreen(val mode: ScanMode = ScanMode.IDLE) : Screen
    data class ScanResultScreen(val scannedNfcCode: Int, val item: Item? = null) : Screen
    data object SettingsScreen : Screen
}

enum class ScanMode {
    IDLE, READ, WRITE, ERASE
}

sealed interface ScanResultState {
    data object Idle : ScanResultState
    data class Registered(val item: Item) : ScanResultState
    data class Unregistered(val scannedCode: Int) : ScanResultState
    data class Failed(val reason: String) : ScanResultState
}

data class TagTrackUiState(
    val currentScreen: Screen = Screen.HomeScreen,
    val inventoryItems: List<Item> = emptyList(),
    val searchQuery: String = "",
    val filteredItems: List<Item> = emptyList(),
    val selectedItem: Item? = null,
    val scanMode: ScanMode = ScanMode.IDLE,
    val scanResultState: ScanResultState = ScanResultState.Idle,
    val snackbarMessage: String? = null,
    val totalUnits: Int = 0,
    val totalRecords: Int = 0,
    val isBusy: Boolean = false
)

class TagTrackViewModel(private val repository: ItemRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TagTrackUiState())
    val uiState: StateFlow<TagTrackUiState> = _uiState.asStateFlow()

    init {
        observeRepository()
    }

    private fun observeRepository() {
        viewModelScope.launch {
            repository.allItems.collect { items ->
                _uiState.update { state ->
                    val filtered = filterItems(items, state.searchQuery)
                    state.copy(
                        inventoryItems = items,
                        filteredItems = filtered,
                        totalRecords = items.size,
                        totalUnits = items.sumOf { it.unitQuantity }
                    )
                }
            }
        }
    }

    private fun filterItems(items: List<Item>, query: String): List<Item> {
        if (query.isBlank()) return items
        val trimmed = query.trim().lowercase()
        return items.filter { item ->
            item.itemName.lowercase().contains(trimmed) ||
                    item.category.lowercase().contains(trimmed) ||
                    item.nfcCode.toString().contains(trimmed) ||
                    item.unqCode.toString().contains(trimmed) ||
                    (item.color?.lowercase()?.contains(trimmed) == true) ||
                    (item.size?.lowercase()?.contains(trimmed) == true) ||
                    (item.condition.lowercase().contains(trimmed)) ||
                    (item.status.lowercase().contains(trimmed)) ||
                    (item.storageLocation?.lowercase()?.contains(trimmed) == true)
        }
    }

    fun navigateTo(screen: Screen) {
        _uiState.update { state ->
            state.copy(currentScreen = screen, snackbarMessage = null)
        }
        if (screen is Screen.ItemDetailsScreen) {
            loadItemByNfcCode(screen.nfcCode)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = filterItems(state.inventoryItems, query)
            state.copy(searchQuery = query, filteredItems = filtered)
        }
    }

    fun selectItem(item: Item?) {
        _uiState.update { it.copy(selectedItem = item) }
    }

    fun setScanMode(mode: ScanMode) {
        _uiState.update { it.copy(scanMode = mode) }
    }

    fun loadItemByNfcCode(nfcCode: Int) {
        viewModelScope.launch {
            val item = repository.getItemByNfcCode(nfcCode)
            _uiState.update { it.copy(selectedItem = item) }
        }
    }

    suspend fun getNextNfcCode(): Int {
        return repository.getNextNfcCode()
    }

    suspend fun getNextUnqCode(): Int {
        return repository.getNextUnqCode()
    }

    fun insertItem(item: Item, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                repository.insertItem(item)
                _uiState.update {
                    it.copy(
                        snackbarMessage = "Item '${item.itemName}' saved successfully",
                        isBusy = false,
                        currentScreen = Screen.InventoryScreen
                    )
                }
                onComplete()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        snackbarMessage = "Error saving item: ${e.localizedMessage}",
                        isBusy = false
                    )
                }
            }
        }
    }

    fun saveAndWriteItem(item: Item, onWriteReady: (Item) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                repository.insertItem(item)
                _uiState.update {
                    it.copy(
                        selectedItem = item,
                        snackbarMessage = "Item '${item.itemName}' saved. Ready to write NFC tag.",
                        isBusy = false
                    )
                }
                onWriteReady(item)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        snackbarMessage = "Error saving item: ${e.localizedMessage}",
                        isBusy = false
                    )
                }
            }
        }
    }

    fun updateItem(item: Item, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                repository.updateItem(item)
                _uiState.update {
                    it.copy(
                        snackbarMessage = "Item '${item.itemName}' updated successfully",
                        isBusy = false,
                        currentScreen = Screen.InventoryScreen
                    )
                }
                onComplete()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        snackbarMessage = "Error updating item: ${e.localizedMessage}",
                        isBusy = false
                    )
                }
            }
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.deleteItem(item)
                _uiState.update {
                    it.copy(
                        snackbarMessage = "Item '${item.itemName}' deleted",
                        selectedItem = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(snackbarMessage = "Error deleting item: ${e.localizedMessage}")
                }
            }
        }
    }

    fun deleteItemByNfcCode(nfcCode: Int) {
        viewModelScope.launch {
            try {
                repository.deleteItemByNfcCode(nfcCode)
                _uiState.update {
                    it.copy(
                        snackbarMessage = "Item deleted successfully",
                        selectedItem = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(snackbarMessage = "Error deleting item: ${e.localizedMessage}")
                }
            }
        }
    }

    fun mergeSimilarItems(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val currentItems = _uiState.value.inventoryItems
                val grouped = currentItems.groupBy { 
                    Pair(it.itemName.trim().lowercase(), it.category.trim().lowercase()) 
                }
                
                var removedCount = 0
                for ((_, group) in grouped) {
                    if (group.size > 1) {
                        val sortedGroup = group.sortedWith(compareBy({ it.nfcCode }, { it.dateAdded }))
                        val primary = sortedGroup.first()
                        val duplicates = sortedGroup.drop(1)
                        
                        val totalQty = group.sumOf { it.unitQuantity }
                        val updatedPrimary = primary.copy(
                            unitQuantity = totalQty,
                            dateUpdated = System.currentTimeMillis()
                        )
                        
                        repository.updateItem(updatedPrimary)
                        
                        for (dup in duplicates) {
                            repository.deleteItem(dup)
                            removedCount++
                        }
                    }
                }
                
                val message = if (removedCount > 0) {
                    "Successfully merged $removedCount duplicate items."
                } else {
                    "No duplicate items found to merge."
                }
                
                _uiState.update {
                    it.copy(
                        snackbarMessage = message,
                        isBusy = false
                    )
                }
                onComplete(removedCount)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        snackbarMessage = "Error merging items: ${e.localizedMessage}",
                        isBusy = false
                    )
                }
            }
        }
    }

    fun handleNfcScanResult(scannedNfcCode: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val item = repository.getItemByNfcCode(scannedNfcCode)
            if (item != null) {
                _uiState.update {
                    it.copy(
                        selectedItem = item,
                        scanResultState = ScanResultState.Registered(item),
                        currentScreen = Screen.ScanResultScreen(scannedNfcCode, item),
                        isBusy = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        selectedItem = null,
                        scanResultState = ScanResultState.Unregistered(scannedNfcCode),
                        currentScreen = Screen.ScanResultScreen(scannedNfcCode, null),
                        isBusy = false
                    )
                }
            }
        }
    }

    fun handleNfcWriteOperation(item: Item) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val nfcCodeToUse = if (item.nfcCode <= 0) repository.getNextNfcCode() else item.nfcCode
                val unqCodeToUse = if (item.unqCode <= 0) repository.getNextUnqCode() else item.unqCode
                val finalItem = item.copy(
                    nfcCode = nfcCodeToUse,
                    unqCode = unqCodeToUse,
                    dateUpdated = System.currentTimeMillis()
                )
                repository.insertItem(finalItem)
                _uiState.update {
                    it.copy(
                        selectedItem = finalItem,
                        scanResultState = ScanResultState.Registered(finalItem),
                        snackbarMessage = "NFC Tag successfully written for #${finalItem.nfcCode}",
                        currentScreen = Screen.ItemDetailsScreen(finalItem.nfcCode),
                        isBusy = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        scanResultState = ScanResultState.Failed(e.localizedMessage ?: "Unknown error"),
                        snackbarMessage = "NFC Write failed: ${e.localizedMessage}",
                        isBusy = false
                    )
                }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    companion object {
        fun provideFactory(repository: ItemRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TagTrackViewModel(repository) as T
                }
            }
    }
}
