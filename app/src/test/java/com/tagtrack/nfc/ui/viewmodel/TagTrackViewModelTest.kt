package com.tagtrack.nfc.ui.viewmodel

import com.tagtrack.nfc.data.Item
import com.tagtrack.nfc.data.ItemDao
import com.tagtrack.nfc.data.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TagTrackViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeItemDao : ItemDao {
        private val itemsFlow = MutableStateFlow<List<Item>>(emptyList())
        private val itemsMap = mutableMapOf<Int, Item>()

        override fun getAllItems(): Flow<List<Item>> = itemsFlow

        override suspend fun getItemByNfcCode(nfcCode: Int): Item? = itemsMap[nfcCode]

        override suspend fun getItemByUnqCode(unqCode: Int): Item? = itemsMap.values.find { it.unqCode == unqCode }

        override suspend fun getMaxNfcCode(): Int? = itemsMap.keys.maxOrNull()

        override suspend fun getMaxUnqCode(): Int? = itemsMap.values.maxOfOrNull { it.unqCode }

        override suspend fun insertItem(item: Item) {
            itemsMap[item.nfcCode] = item
            itemsFlow.update { itemsMap.values.sortedBy { it.itemName } }
        }

        override suspend fun insertItems(items: List<Item>) {
            items.forEach { itemsMap[it.nfcCode] = it }
            itemsFlow.update { itemsMap.values.sortedBy { it.itemName } }
        }

        override suspend fun updateItem(item: Item) {
            itemsMap[item.nfcCode] = item
            itemsFlow.update { itemsMap.values.sortedBy { it.itemName } }
        }

        override suspend fun deleteItem(item: Item) {
            itemsMap.remove(item.nfcCode)
            itemsFlow.update { itemsMap.values.sortedBy { it.itemName } }
        }

        override suspend fun deleteItemByNfcCode(nfcCode: Int) {
            itemsMap.remove(nfcCode)
            itemsFlow.update { itemsMap.values.sortedBy { it.itemName } }
        }

        override fun getTotalUnitsFlow(): Flow<Int?> = MutableStateFlow(itemsMap.values.sumOf { it.unitQuantity })

        override fun getTotalRecordsFlow(): Flow<Int> = MutableStateFlow(itemsMap.size)

        override fun getCountByStatusFlow(status: String): Flow<Int> = MutableStateFlow(itemsMap.values.count { it.status == status })

        override fun getCountByConditionFlow(condition: String): Flow<Int> = MutableStateFlow(itemsMap.values.count { it.condition == condition })
    }

    private lateinit var fakeDao: FakeItemDao
    private lateinit var repository: ItemRepository
    private lateinit var viewModel: TagTrackViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeItemDao()
        repository = ItemRepository(fakeDao)
        viewModel = TagTrackViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() = runTest {
        val state = viewModel.uiState.value
        assertEquals(Screen.HomeScreen, state.currentScreen)
        assertTrue(state.inventoryItems.isEmpty())
    }

    @Test
    fun testNavigation() = runTest {
        viewModel.navigateTo(Screen.InventoryScreen)
        assertEquals(Screen.InventoryScreen, viewModel.uiState.value.currentScreen)
    }

    @Test
    fun testCodeGeneration() = runTest {
        val nextNfc = repository.getNextNfcCode()
        val nextUnq = repository.getNextUnqCode()
        assertEquals(100001, nextNfc)
        assertEquals(500001, nextUnq)
    }

    @Test
    fun testAlphabeticalSorting() = runTest {
        val item1 = Item(
            nfcCode = 1, unqCode = 1, itemName = "Zebra Coat", category = "Outerwear",
            ageGroup = "Adult", size = "L", color = "Black", condition = "New",
            status = "Available", unitQuantity = 1, storageLocation = "Rack 1",
            description = "Desc", dateAdded = 100L, dateUpdated = null
        )
        val item2 = Item(
            nfcCode = 2, unqCode = 2, itemName = "Apple Shirt", category = "Tops",
            ageGroup = "Adult", size = "M", color = "White", condition = "New",
            status = "Available", unitQuantity = 1, storageLocation = "Rack 2",
            description = "Desc", dateAdded = 200L, dateUpdated = null
        )

        viewModel.insertItem(item1)
        viewModel.insertItem(item2)
        testDispatcher.scheduler.advanceUntilIdle()

        val items = viewModel.uiState.value.inventoryItems
        assertEquals("Apple Shirt", items[0].itemName)
        assertEquals("Zebra Coat", items[1].itemName)
    }

    @Test
    fun testMergeSimilarItems() = runTest {
        val item1 = Item(
            nfcCode = 1, unqCode = 1, itemName = "Blue Jeans", category = "Apparel",
            ageGroup = "Adult", size = "32", color = "Blue", condition = "New",
            status = "Available", unitQuantity = 2, storageLocation = "Rack A",
            description = "Desc", dateAdded = 100L, dateUpdated = null
        )
        val item2 = Item(
            nfcCode = 2, unqCode = 2, itemName = "blue jeans", category = "apparel",
            ageGroup = "Adult", size = "32", color = "Blue", condition = "Good",
            status = "Available", unitQuantity = 3, storageLocation = "Rack B",
            description = "Desc", dateAdded = 200L, dateUpdated = null
        )
        val item3 = Item(
            nfcCode = 3, unqCode = 3, itemName = "Red Hat", category = "Accessories",
            ageGroup = "Adult", size = "Free", color = "Red", condition = "New",
            status = "Available", unitQuantity = 1, storageLocation = "Rack C",
            description = "Desc", dateAdded = 300L, dateUpdated = null
        )

        viewModel.insertItem(item1)
        viewModel.insertItem(item2)
        viewModel.insertItem(item3)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.inventoryItems.size)

        viewModel.mergeSimilarItems()
        testDispatcher.scheduler.advanceUntilIdle()

        val items = viewModel.uiState.value.inventoryItems
        assertEquals(2, items.size)
        val mergedBlueJeans = items.find { it.itemName.equals("Blue Jeans", ignoreCase = true) }
        assertEquals(5, mergedBlueJeans?.unitQuantity)
    }

    @Test
    fun testInsertAndScanItem() = runTest {
        val newItem = Item(
            nfcCode = 100001,
            unqCode = 500001,
            itemName = "Test Jacket",
            category = "Outerwear",
            ageGroup = "Adult",
            size = "L",
            color = "Black",
            condition = "New",
            status = "Available",
            unitQuantity = 1,
            storageLocation = "Rack 1",
            description = "Test description",
            dateAdded = System.currentTimeMillis(),
            dateUpdated = null
        )

        viewModel.insertItem(newItem)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.inventoryItems.size)

        // Test scan registered
        viewModel.handleNfcScanResult(100001)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.scanResultState is ScanResultState.Registered)
        assertEquals("Test Jacket", (state.scanResultState as ScanResultState.Registered).item.itemName)

        // Test scan unregistered
        viewModel.handleNfcScanResult(999999)
        testDispatcher.scheduler.advanceUntilIdle()

        val state2 = viewModel.uiState.value
        assertTrue(state2.scanResultState is ScanResultState.Unregistered)
        assertEquals(999999, (state2.scanResultState as ScanResultState.Unregistered).scannedCode)
    }
}
