package com.tagtrack.nfc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tagtrack.nfc.data.Item
import com.tagtrack.nfc.ui.components.ItemCard
import com.tagtrack.nfc.ui.components.TagTrackHeader
import com.tagtrack.nfc.ui.theme.*

private val ButtonCornerRadius = 6.dp

@Composable
fun HomeScreen(
    totalUnits: Int,
    totalRecords: Int,
    inventoryItems: List<Item>,
    onNavigateToInventory: (String) -> Unit, // Tumatanggap ng String (status o condition)
    onNavigateToScanner: () -> Unit,
    onNavigateToForm: () -> Unit,
    onItemClick: (Item) -> Unit
) {
    val availableCount = inventoryItems.count { it.status.equals("Available", true) }
    val rentedCount = inventoryItems.count { it.status.equals("Rented", true) }
    val unavailableCount = inventoryItems.count { it.status.equals("Unavailable", true) || it.status.equals("Maintenance", true) }

    val attentionItems = inventoryItems.filter {
        it.condition.equals("Needs Cleaning", true) ||
                it.condition.equals("Damaged", true) ||
                it.condition.equals("Poor", true)
    }

    val recentItems = inventoryItems.sortedByDescending { it.dateAdded }.take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TagTrackHeader(
            title = "TagTrack NFC",
            subtitle = "Inventory & Asset Management",
            actions = {
                IconButton(onClick = onNavigateToScanner) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Scan NFC",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. SUMMARY STATS CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = totalUnits.toString(),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total Units",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. ASSET STATUS OVERVIEW
            item {
                Text(
                    text = "Asset Status Overview",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusTile(
                        modifier = Modifier.weight(1f),
                        title = "Available",
                        count = availableCount,
                        color = Green,
                        icon = Icons.Rounded.CheckCircle,
                        onClick = { onNavigateToInventory("Available") }
                    )
                    StatusTile(
                        modifier = Modifier.weight(1f),
                        title = "Rented",
                        count = rentedCount,
                        color = Amber,
                        icon = Icons.Rounded.Schedule,
                        onClick = { onNavigateToInventory("Rented") }
                    )
                    StatusTile(
                        modifier = Modifier.weight(1f),
                        title = "Unavailable",
                        count = unavailableCount,
                        color = Red,
                        icon = Icons.Rounded.Block,
                        onClick = { onNavigateToInventory("Unavailable") }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. ATTENTION CARD
            if (attentionItems.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CardCornerRadius),
                        colors = CardDefaults.cardColors(
                            containerColor = Red.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, Red.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Attention",
                                tint = Red,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Attention Required",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Red
                                    )
                                )
                                Text(
                                    text = "${attentionItems.size} items require cleaning, repair or inspection.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            Button(
                                onClick = { onNavigateToInventory("Attention Required") }, // BINAGO
                                colors = ButtonDefaults.buttonColors(containerColor = Red),
                                shape = RoundedCornerShape(ButtonCornerRadius),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("View", fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 4. RECENT ACTIVITY SECTION HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Added",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    TextButton(onClick = { onNavigateToInventory("") }) { // Walang filter
                        Text("See All", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 5. RECENT ITEMS LIST
            if (recentItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items in inventory yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                items(recentItems, key = { it.nfcCode }) { item ->
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        ItemCard(item = item, onClick = { onItemClick(item) })
                    }
                }
            }
        }
    }
}

@Composable
fun StatusTile(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(TileCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}