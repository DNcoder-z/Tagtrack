package com.tagtrack.nfc.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tagtrack.nfc.data.Item
import com.tagtrack.nfc.ui.components.TagTrackHeader
import com.tagtrack.nfc.ui.theme.BlueBronze
import com.tagtrack.nfc.ui.theme.CardCornerRadius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormScreen(
    itemToEdit: Item? = null,
    defaultNfcCode: Int = 1000,
    defaultUnqCode: Int = 1000,
    onBackClick: () -> Unit,
    onSaveItem: (Item) -> Unit,
    onSaveAndWriteNfc: (Item) -> Unit
) {
    val isEditing = itemToEdit != null

    var itemName by remember { mutableStateOf(itemToEdit?.itemName ?: "") }
    var category by remember { mutableStateOf(itemToEdit?.category ?: "Costume") }
    var nfcCodeStr by remember { mutableStateOf(itemToEdit?.nfcCode?.toString() ?: defaultNfcCode.toString()) }
    var unqCodeStr by remember { mutableStateOf(itemToEdit?.unqCode?.toString() ?: defaultUnqCode.toString()) }
    var ageGroup by remember { mutableStateOf(itemToEdit?.ageGroup ?: "Adults") }
    var size by remember { mutableStateOf(itemToEdit?.size ?: "M") }
    var color by remember { mutableStateOf(itemToEdit?.color ?: "") }
    var condition by remember { mutableStateOf(itemToEdit?.condition ?: "Good") }
    var status by remember { mutableStateOf(itemToEdit?.status ?: "Available") }
    var unitQuantityStr by remember { mutableStateOf(itemToEdit?.unitQuantity?.toString() ?: "1") }
    var storageLocation by remember { mutableStateOf(itemToEdit?.storageLocation ?: "") }
    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }
    var photoUri by remember { mutableStateOf(itemToEdit?.photoUri ?: "") }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    val categories = listOf("Costume", "Prop", "Equipment", "Garment", "Accessory", "Other")
    val ageGroups = listOf("Kids", "Teens", "Adults", "Unisex")
    val sizes = listOf("XS", "S", "M", "L", "XL", "XXL", "Free")
    val conditions = listOf("New", "Good", "Fair", "Needs Cleaning", "Damaged")
    val statuses = listOf("Available", "Rented", "Unavailable", "Maintenance")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TagTrackHeader(
            title = if (isEditing) "Edit Item #${itemToEdit?.nfcCode}" else "Add New Item",
            subtitle = if (isEditing) "Update asset information" else "Register a new asset",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showError) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(CardCornerRadius)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onErrorContainer)
                        )
                    }
                }
            }

            // Identification Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Identification",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = nfcCodeStr,
                            onValueChange = { nfcCodeStr = it },
                            label = { Text("NFC Code *") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(CardCornerRadius),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unqCodeStr,
                            onValueChange = { unqCodeStr = it },
                            label = { Text("Unique Code *") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(CardCornerRadius),
                            singleLine = true
                        )
                    }
                }
            }

            // Item Information Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Item Information",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Item Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CardCornerRadius),
                        singleLine = true
                    )

                    // Photo & Media Section
                    Text("Item Photo (Optional)", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                              .size(72.dp)
                              .clip(RoundedCornerShape(CardCornerRadius))
                              .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUri.isNotBlank()) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.LocalOffer,
                                    contentDescription = null,
                                    tint = BlueBronze,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = photoUri,
                                onValueChange = { photoUri = it },
                                label = { Text("Photo URI or URL") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(CardCornerRadius),
                                singleLine = true
                            )
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(CardCornerRadius)
                            ) {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick from Gallery")
                            }
                        }
                    }

                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    ScrollableChipRow(
                        items = categories,
                        selectedItem = category,
                        onSelected = { category = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = color,
                            onValueChange = { color = it },
                            label = { Text("Color / Tone") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(CardCornerRadius),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unitQuantityStr,
                            onValueChange = { unitQuantityStr = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(CardCornerRadius),
                            singleLine = true
                        )
                    }

                    Text("Age Group", style = MaterialTheme.typography.labelMedium)
                    ScrollableChipRow(
                        items = ageGroups,
                        selectedItem = ageGroup,
                        onSelected = { ageGroup = it }
                    )

                    Text("Size", style = MaterialTheme.typography.labelMedium)
                    ScrollableChipRow(
                        items = sizes,
                        selectedItem = size,
                        onSelected = { size = it }
                    )
                }
            }

            // Inventory Status Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Condition & Status",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Text("Condition", style = MaterialTheme.typography.labelMedium)
                    ScrollableChipRow(
                        items = conditions,
                        selectedItem = condition,
                        onSelected = { condition = it }
                    )

                    Text("Status", style = MaterialTheme.typography.labelMedium)
                    ScrollableChipRow(
                        items = statuses,
                        selectedItem = status,
                        onSelected = { status = it }
                    )

                    OutlinedTextField(
                        value = storageLocation,
                        onValueChange = { storageLocation = it },
                        label = { Text("Storage Location (e.g. Rack A-3)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CardCornerRadius),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CardCornerRadius),
                        minLines = 3
                    )
                }
            }

            // Save Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val nfc = nfcCodeStr.toIntOrNull()
                        val unq = unqCodeStr.toIntOrNull()
                        val qty = unitQuantityStr.toIntOrNull() ?: 1

                        if (itemName.isBlank()) {
                            showError = true
                            errorMessage = "Item Name is required."
                            return@OutlinedButton
                        }
                        if (nfc == null || nfc <= 0) {
                            showError = true
                            errorMessage = "Valid NFC Code is required."
                            return@OutlinedButton
                        }
                        if (unq == null || unq <= 0) {
                            showError = true
                            errorMessage = "Valid Unique Code is required."
                            return@OutlinedButton
                        }

                        val currentTime = System.currentTimeMillis()
                        val finalItem = Item(
                            nfcCode = nfc,
                            unqCode = unq,
                            itemName = itemName.trim(),
                            category = category,
                            ageGroup = ageGroup,
                            size = size,
                            color = color.ifBlank { null },
                            condition = condition,
                            status = status,
                            unitQuantity = qty,
                            storageLocation = storageLocation.ifBlank { null },
                            description = description.ifBlank { null },
                            photoUri = photoUri.ifBlank { null },
                            dateAdded = itemToEdit?.dateAdded ?: currentTime,
                            dateUpdated = if (isEditing) currentTime else null
                        )
                        onSaveItem(finalItem)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(CardCornerRadius)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEditing) "Update" else "Save Only")
                }

                Button(
                    onClick = {
                        val nfc = nfcCodeStr.toIntOrNull()
                        val unq = unqCodeStr.toIntOrNull()
                        val qty = unitQuantityStr.toIntOrNull() ?: 1

                        if (itemName.isBlank()) {
                            showError = true
                            errorMessage = "Item Name is required."
                            return@Button
                        }
                        if (nfc == null || nfc <= 0) {
                            showError = true
                            errorMessage = "Valid NFC Code is required."
                            return@Button
                        }
                        if (unq == null || unq <= 0) {
                            showError = true
                            errorMessage = "Valid Unique Code is required."
                            return@Button
                        }

                        val currentTime = System.currentTimeMillis()
                        val finalItem = Item(
                            nfcCode = nfc,
                            unqCode = unq,
                            itemName = itemName.trim(),
                            category = category,
                            ageGroup = ageGroup,
                            size = size,
                            color = color.ifBlank { null },
                            condition = condition,
                            status = status,
                            unitQuantity = qty,
                            storageLocation = storageLocation.ifBlank { null },
                            description = description.ifBlank { null },
                            photoUri = photoUri.ifBlank { null },
                            dateAdded = itemToEdit?.dateAdded ?: currentTime,
                            dateUpdated = if (isEditing) currentTime else null
                        )
                        onSaveAndWriteNfc(finalItem)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(CardCornerRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Nfc, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save & Write NFC")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollableChipRow(
    items: List<String>,
    selectedItem: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            FilterChip(
                selected = item == selectedItem,
                onClick = { onSelected(item) },
                label = { Text(item) },
                shape = RoundedCornerShape(CardCornerRadius)
            )
        }
    }
}
