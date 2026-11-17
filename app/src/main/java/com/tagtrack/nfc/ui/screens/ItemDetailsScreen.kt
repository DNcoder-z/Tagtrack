package com.tagtrack.nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tagtrack.nfc.data.Item
import com.tagtrack.nfc.ui.components.BadgeType
import com.tagtrack.nfc.ui.components.StatusBadge
import com.tagtrack.nfc.ui.components.TagTrackHeader
import com.tagtrack.nfc.ui.theme.BlueBronze
import com.tagtrack.nfc.ui.theme.CardCornerRadius
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ItemDetailsScreen(
    item: Item?,
    onBackClick: () -> Unit,
    onEditClick: (Item) -> Unit,
    onDeleteClick: (Item) -> Unit,
    onWriteNfcClick: (Item) -> Unit,
    onEraseNfcClick: (Item) -> Unit
) {
    if (item == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TagTrackHeader(title = "Item Details", onBackClick = onBackClick)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Item not found or deleted.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        return
    }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val dateAddedStr = dateFormat.format(Date(item.dateAdded))
    val dateUpdatedStr = item.dateUpdated?.let { dateFormat.format(Date(it)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TagTrackHeader(
            title = item.itemName,
            subtitle = "Unq #${item.unqCode} • NFC #${item.nfcCode}",
            onBackClick = onBackClick,
            actions = {
                IconButton(onClick = { onEditClick(item) }) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { onDeleteClick(item) }) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Image / Icon Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(CardCornerRadius))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!item.photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = item.photoUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.LocalOffer,
                                contentDescription = null,
                                tint = BlueBronze,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = item.itemName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(text = item.status, type = BadgeType.STATUS)
                        StatusBadge(text = item.condition, type = BadgeType.CONDITION)
                    }
                }
            }

            // Quick NFC Write Action
            Button(
                onClick = { onWriteNfcClick(item) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CardCornerRadius),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Rounded.Nfc, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Write to NFC Tag")
            }

            // Quick NFC Erase Action
            OutlinedButton(
                onClick = { onEraseNfcClick(item) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CardCornerRadius),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.Rounded.DeleteSweep, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Erase NFC Tag")
            }

            // Attributes Card
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
                        text = "Asset Specifications",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    AttributeRow(label = "NFC Code", value = "#${item.nfcCode}")
                    AttributeRow(label = "Unique Code", value = "#${item.unqCode}")
                    AttributeRow(label = "Category", value = item.category)
                    AttributeRow(label = "Age Group", value = item.ageGroup)
                    AttributeRow(label = "Size", value = item.size ?: "N/A")
                    AttributeRow(label = "Color", value = item.color ?: "N/A")
                    AttributeRow(label = "Unit Quantity", value = item.unitQuantity.toString())
                    AttributeRow(label = "Storage Location", value = item.storageLocation ?: "N/A")
                }
            }

            // Description Card
            if (!item.description.isNullOrBlank()) {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Description & Notes",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // History / Audit Card
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "History & Audit",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    AttributeRow(label = "Date Added", value = dateAddedStr)
                    if (dateUpdatedStr != null) {
                        AttributeRow(label = "Last Updated", value = dateUpdatedStr)
                    }
                }
            }
        }
    }
}

@Composable
fun AttributeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
