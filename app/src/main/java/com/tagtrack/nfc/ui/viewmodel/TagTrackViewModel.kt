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
    data class ItemDetailsScreen(val nfcCode: String) : Screen
    data class ItemFormScreen(val isEditing: Boolean = false, val item: Item? = null, val prefilledNfcCode: String? = null) : Screen
    data class ScannerScreen(val mode: ScanMode = ScanMode.READ) : Screen
    data class ScanResultScreen(val scannedNfcCode: String, val item: Item? = null) : Screen
    data object SettingsScreen : Screen
}

enum class ScanMode {
    IDLE, READ
}

sealed interface ScanResultState {
    data object Idle : ScanResultState
    data class Registered(val item: Item) : ScanResultState
    data class Unregistered(val scannedCode: String) : ScanResultState
    data class Failed(val reason: String) : ScanResultState
}

data class TagTrackUiState(
    val currentScreen: Screen = Screen.HomeScreen,
    val inventoryItems: List<Item> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "All Categories",
    val availableCategories: List<String> = listOf("All Categories"),
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
                    val categories = listOf("All Categories") + items.map { it.category }.distinct().sorted()
                    val filtered = filterItems(items, state.searchQuery, state.selectedCategory)
                    state.copy(
                        inventoryItems = items,
                        availableCategories = categories,
                        filteredItems = filtered,
                        totalRecords = items.size,
                        totalUnits = items.sumOf { it.unitQuantity }
                    )
                }
            }
        }
    }

    private fun filterItems(items: List<Item>, query: String, category: String): List<Item> {
        val categoryFiltered = if (category == "All Categories" || category.isBlank()) {
            items
        } else {
            items.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (query.isBlank()) return categoryFiltered
        val trimmed = query.trim().lowercase()
        return categoryFiltered.filter { item ->
            item.itemName.lowercase().contains(trimmed) ||
                    item.category.lowercase().contains(trimmed) ||
                    item.nfcCode.lowercase().contains(trimmed) ||
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
            val filtered = filterItems(state.inventoryItems, query, state.selectedCategory)
            state.copy(searchQuery = query, filteredItems = filtered)
        }
    }

    fun updateSelectedCategory(category: String) {
        _uiState.update { state ->
            val filtered = filterItems(state.inventoryItems, state.searchQuery, category)
            state.copy(selectedCategory = category, filteredItems = filtered)
        }
    }

    fun selectItem(item: Item?) {
        _uiState.update { it.copy(selectedItem = item) }
    }

    fun setScanMode(mode: ScanMode) {
        _uiState.update { it.copy(scanMode = mode) }
    }

    fun loadItemByNfcCode(nfcCode: String) {
        viewModelScope.launch {
            val item = repository.getItemByNfcCode(nfcCode)
            _uiState.update { it.copy(selectedItem = item) }
        }
    }

    suspend fun getNextUnqCode(): Int {
        return repository.getNextUnqCode()
    }

    fun insertItem(item: Item, onComplete: () -> Unit = {}) {
        if (item.unitQuantity < 1) {
            _uiState.update { it.copy(snackbarMessage = "Unit quantity cannot be less than 1") }
            return
        }
        if (item.nfcCode.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "NFC UID cannot be blank") }
            return
        }
        if (item.photoUri.isNullOrBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Item photo is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val existing = repository.getItemByNfcCode(item.nfcCode)
                if (existing != null) {
                    _uiState.update {
                        it.copy(
                            snackbarMessage = "Error: NFC UID '${item.nfcCode}' is already linked to item '${existing.itemName}'",
                            isBusy = false
                        )
                    }
                    return@launch
                }
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

    fun updateItem(item: Item, onComplete: () -> Unit = {}) {
        if (item.unitQuantity < 1) {
            _uiState.update { it.copy(snackbarMessage = "Unit quantity cannot be less than 1") }
            return
        }
        if (item.nfcCode.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "NFC UID cannot be blank") }
            return
        }
        if (item.photoUri.isNullOrBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Item photo is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val existing = repository.getItemByNfcCode(item.nfcCode)
                if (existing != null && existing.unqCode != item.unqCode) {
                    _uiState.update {
                        it.copy(
                            snackbarMessage = "Error: NFC UID '${item.nfcCode}' is already linked to item '${existing.itemName}'",
                            isBusy = false
                        )
                    }
                    return@launch
                }
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

    fun deleteItemByNfcCode(nfcCode: String) {
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

    fun handleNfcScanResult(scannedUid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val item = repository.getItemByNfcCode(scannedUid)
            if (item != null) {
                _uiState.update {
                    it.copy(
                        selectedItem = item,
                        scanResultState = ScanResultState.Registered(item),
                        currentScreen = Screen.ScanResultScreen(scannedUid, item),
                        isBusy = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        selectedItem = null,
                        scanResultState = ScanResultState.Unregistered(scannedUid),
                        currentScreen = Screen.ScanResultScreen(scannedUid, null),
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
