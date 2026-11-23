package com.tagtrack.nfc.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.tagtrack.nfc.data.Item
import com.tagtrack.nfc.ui.components.TagTrackHeader
import com.tagtrack.nfc.ui.theme.BlueBronze
import com.tagtrack.nfc.ui.theme.CardCornerRadius
import com.tagtrack.nfc.util.ImageStorageManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormScreen(
    itemToEdit: Item? = null,
    prefilledNfcCode: String? = null,
    defaultUnqCode: Int = 500001,
    onBackClick: () -> Unit,
    onSaveItem: (Item) -> Unit
) {
    val isEditing = itemToEdit != null
    val context = LocalContext.current

    var itemName by remember { mutableStateOf(itemToEdit?.itemName ?: "") }
    var category by remember { mutableStateOf(itemToEdit?.category ?: "Costume") }
    var nfcCodeStr by remember { mutableStateOf(itemToEdit?.nfcCode ?: prefilledNfcCode ?: "04A224B2C16180") }
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
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionExplanation by remember { mutableStateOf(false) }
    var showUpdateConfirmation by remember { mutableStateOf(false) }
    var pendingItemToSave by remember { mutableStateOf<Item?>(null) }

    if (showUpdateConfirmation && pendingItemToSave != null) {
        AlertDialog(
            onDismissRequest = { showUpdateConfirmation = false },
            title = { Text("Confirm Item Update") },
            text = { Text("This item will be updated, and its existing data will be overwritten with your new changes. Are you sure you want to continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val itemToSave = pendingItemToSave
                        showUpdateConfirmation = false
                        pendingItemToSave = null
                        if (itemToSave != null) {
                            onSaveItem(itemToSave)
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUpdateConfirmation = false
                        pendingItemToSave = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val permanentPath = ImageStorageManager.saveUriToInternalStorage(context, uri)
            if (permanentPath != null) {
                photoUri = permanentPath
            } else {
                showError = true
                errorMessage = "Failed to copy gallery image to internal storage."
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val permanentPath = ImageStorageManager.saveUriToInternalStorage(context, tempCameraUri!!)
            if (permanentPath != null) {
                try {
                    context.contentResolver.delete(tempCameraUri!!, null, null)
                } catch (_: Exception) {}
                photoUri = permanentPath
            } else {
                showError = true
                errorMessage = "Failed to save captured photo."
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val photoFile = File.createTempFile(
                    "JPEG_${System.currentTimeMillis()}_",
                    ".jpg",
                    context.cacheDir
                )
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                showError = true
                errorMessage = "Failed to create camera file: ${e.localizedMessage}"
            }
        } else {
            showPermissionExplanation = true
        }
    }

    if (showPermissionExplanation) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanation = false },
            title = { Text("Camera Permission Required") },
            text = { Text("Camera permission is needed to capture photos of items for your inventory. Please grant camera permission to use this feature.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionExplanation = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionExplanation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
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
            title = if (isEditing) "Edit Item " else "Add New Item",
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

                    OutlinedTextField(
                        value = nfcCodeStr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("NFC Hardware UID *") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "UID Locked"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CardCornerRadius),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unqCodeStr,
                        onValueChange = { unqCodeStr = it },
                        label = { Text("Unique Code *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(CardCornerRadius),
                        singleLine = true
                    )
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
                    Text("Item Photo *", style = MaterialTheme.typography.labelMedium)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
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

                        // Buttons sa ibaba ng photo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(CardCornerRadius),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gallery", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = {
                                    val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                        try {
                                            val photoFile = File.createTempFile(
                                                "JPEG_${System.currentTimeMillis()}_",
                                                ".jpg",
                                                context.cacheDir
                                            )
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                photoFile
                                            )
                                            tempCameraUri = uri
                                            cameraLauncher.launch(uri)
                                        } catch (e: Exception) {
                                            showError = true
                                            errorMessage = "Failed to create camera file: ${e.localizedMessage}"
                                        }
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(CardCornerRadius),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Camera", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    ScrollableChipRow(
                        items = categories,
                        selectedItem = category,
                        onSelected = { category = it }
                    )

//                    // Inayos ang layout ng Color at Quantity
//                    Column(
//                        modifier = Modifier.fillMaxWidth(),
//                        verticalArrangement = Arrangement.spacedBy(16.dp)
//                    ) {
//                        OutlinedTextField(
//                            value = color,
//                            onValueChange = { color = it },
//                            label = { Text("Color / Tone") },
//                            modifier = Modifier.fillMaxWidth(),
//                            shape = RoundedCornerShape(CardCornerRadius),
//                            singleLine = true
//                        )
//
//                        val currentQty = unitQuantityStr.toIntOrNull() ?: 0
//                        Column(
//                            modifier = Modifier.fillMaxWidth(),
//                            verticalArrangement = Arrangement.spacedBy(4.dp)
//                        ) {
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.Center
//                            ) {
//                                FilledTonalIconButton(
//                                    onClick = {
//                                        val current = unitQuantityStr.toIntOrNull() ?: 1
//                                        if (current > 1) {
//                                            unitQuantityStr = (current - 1).toString()
//                                        }
//                                    },
//                                    enabled = currentQty > 1,
//                                    modifier = Modifier.size(48.dp),
//                                    shape = RoundedCornerShape(CardCornerRadius)
//                                ) {
//                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease Quantity")
//                                }
//
//                                Spacer(modifier = Modifier.width(12.dp))
//
//                                OutlinedTextField(
//                                    value = unitQuantityStr,
//                                    onValueChange = { newValue ->
//                                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
//                                            unitQuantityStr = newValue
//                                        }
//                                    },
//                                    label = { Text("Qty *", textAlign = TextAlign.Center) },
//                                    modifier = Modifier.width(100.dp),
//                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                                    shape = RoundedCornerShape(CardCornerRadius),
//                                    singleLine = true,
//                                    isError = currentQty < 1,
//                                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center)
//                                )
//
//                                Spacer(modifier = Modifier.width(12.dp))
//
//                                FilledTonalIconButton(
//                                    onClick = {
//                                        val current = unitQuantityStr.toIntOrNull() ?: 0
//                                        unitQuantityStr = (current + 1).coerceAtLeast(1).toString()
//                                    },
//                                    modifier = Modifier.size(48.dp),
//                                    shape = RoundedCornerShape(CardCornerRadius)
//                                ) {
//                                    Icon(Icons.Rounded.Add, contentDescription = "Increase Quantity")
//                                }
//                            }
//                            if (currentQty < 1) {
//                                Text(
//                                    text = "Minimum quantity is 1",
//                                    style = MaterialTheme.typography.bodySmall,
//                                    color = MaterialTheme.colorScheme.error,
//                                    modifier = Modifier.fillMaxWidth(),
//                                    textAlign = TextAlign.Center
//                                )
//                            }
//                        }
//                    }

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

            // Save Button
            val qty = unitQuantityStr.toIntOrNull() ?: 0
            val isFormValid = itemName.isNotBlank() &&
                    nfcCodeStr.isNotBlank() &&
                    (unqCodeStr.toIntOrNull() ?: 0) > 0 &&
                    qty >= 1 &&
                    photoUri.isNotBlank()

            Button(
                onClick = {
                    val unq = unqCodeStr.toIntOrNull()

                    if (itemName.isBlank()) {
                        showError = true
                        errorMessage = "Item Name is required."
                        return@Button
                    }
                    if (nfcCodeStr.isBlank()) {
                        showError = true
                        errorMessage = "NFC Hardware UID is required."
                        return@Button
                    }
                    if (photoUri.isBlank()) {
                        showError = true
                        errorMessage = "Item Photo is required."
                        return@Button
                    }
                    if (unq == null || unq <= 0) {
                        showError = true
                        errorMessage = "Valid Unique Code is required."
                        return@Button
                    }
                    if (qty < 1) {
                        showError = true
                        errorMessage = "Unit quantity cannot be less than 1."
                        return@Button
                    }

                    val currentTime = System.currentTimeMillis()
                    val finalItem = Item(
                        nfcCode = nfcCodeStr.trim().uppercase(),
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
                        photoUri = photoUri.trim(),
                        dateAdded = itemToEdit?.dateAdded ?: currentTime,
                        dateUpdated = if (isEditing) currentTime else null
                    )
                    if (isEditing) {
                        pendingItemToSave = finalItem
                        showUpdateConfirmation = true
                    } else {
                        onSaveItem(finalItem)
                    }
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(CardCornerRadius),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditing) "Update Item" else "Save Item", fontWeight = FontWeight.Bold)
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