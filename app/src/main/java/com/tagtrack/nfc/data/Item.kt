package com.tagtrack.nfc.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey
    val nfcCode: String,
    val unqCode: Int,
    val itemName: String,
    val category: String,
    val ageGroup: String,
    val size: String?,
    val color: String?,
    val condition: String,
    val status: String,
    val unitQuantity: Int = 1,
    val storageLocation: String?,
    val description: String?,
    val photoUri: String? = null,
    val dateAdded: Long,
    val dateUpdated: Long?
)
