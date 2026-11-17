package com.tagtrack.nfc.data

import kotlinx.coroutines.flow.Flow

class ItemRepository(private val itemDao: ItemDao) {
    val allItems: Flow<List<Item>> = itemDao.getAllItems()
    val totalUnitsFlow: Flow<Int?> = itemDao.getTotalUnitsFlow()
    val totalRecordsFlow: Flow<Int> = itemDao.getTotalRecordsFlow()

    suspend fun getItemByNfcCode(nfcCode: Int): Item? = itemDao.getItemByNfcCode(nfcCode)

    suspend fun getItemByUnqCode(unqCode: Int): Item? = itemDao.getItemByUnqCode(unqCode)

    suspend fun getNextNfcCode(): Int {
        val maxNfc = itemDao.getMaxNfcCode() ?: 0
        return if (maxNfc < 100000) 100001 else maxNfc + 1
    }

    suspend fun getNextUnqCode(): Int {
        val maxUnq = itemDao.getMaxUnqCode() ?: 0
        return if (maxUnq < 500000) 500001 else maxUnq + 1
    }

    suspend fun insertItem(item: Item) {
        itemDao.insertItem(item)
    }

    suspend fun updateItem(item: Item) {
        itemDao.updateItem(item)
    }

    suspend fun deleteItem(item: Item) {
        itemDao.deleteItem(item)
    }

    suspend fun deleteItemByNfcCode(nfcCode: Int) {
        itemDao.deleteItemByNfcCode(nfcCode)
    }

    fun getCountByStatusFlow(status: String): Flow<Int> = itemDao.getCountByStatusFlow(status)

    fun getCountByConditionFlow(condition: String): Flow<Int> = itemDao.getCountByConditionFlow(condition)
}
