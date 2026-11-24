package com.tagtrack.nfc.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY itemName ASC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE nfcCode = :nfcCode")
    suspend fun getItemByNfcCode(nfcCode: String): Item?

    @Query("SELECT * FROM items WHERE unqCode = :unqCode")
    suspend fun getItemByUnqCode(unqCode: Int): Item?

    @Query("SELECT MAX(unqCode) FROM items")
    suspend fun getMaxUnqCode(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<Item>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<Item>)

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("DELETE FROM items WHERE nfcCode = :nfcCode")
    suspend fun deleteItemByNfcCode(nfcCode: String)

    @Query("SELECT SUM(unitQuantity) FROM items")
    fun getTotalUnitsFlow(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM items")
    fun getTotalRecordsFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE status = :status")
    fun getCountByStatusFlow(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE condition = :condition")
    fun getCountByConditionFlow(condition: String): Flow<Int>
}
