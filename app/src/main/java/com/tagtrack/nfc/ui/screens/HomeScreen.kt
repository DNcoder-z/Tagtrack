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
    onNavigateToInventory: () -> Unit,
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
                        imageVector = Icons.Rounded.Nfc,
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
            // Summary Stats Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(
                            label = "Total Units",
                            value = totalUnits.toString(),
                            icon = Icons.Rounded.Inventory2
                        )
                        HorizontalDivider(
                            modifier = Modifier
                                .height(40.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                        StatItem(
                            label = "Unique Records",
                            value = totalRecords.toString(),
                            icon = Icons.Rounded.Category
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Status Tiles Grid
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
                        onClick = onNavigateToInventory
                    )
                    StatusTile(
                        modifier = Modifier.weight(1f),
                        title = "Rented",
                        count = rentedCount,
                        color = Amber,
                        icon = Icons.Rounded.Schedule,
                        onClick = onNavigateToInventory
                    )
                    StatusTile(
                        modifier = Modifier.weight(1f),
                        title = "Unavailable",
                        count = unavailableCount,
                        color = Red,
                        icon = Icons.Rounded.Block,
                        onClick = onNavigateToInventory
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Attention Card (if any items need cleaning/repair)
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
                                onClick = onNavigateToInventory,
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

            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateToScanner,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(CardCornerRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.Nfc, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan NFC")
                    }
                    OutlinedButton(
                        onClick = onNavigateToScanner,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(CardCornerRadius)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Item")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Recent Activity Section Header
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
                    TextButton(onClick = onNavigateToInventory) {
                        Text("See All", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Recent Items List
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
fun StatItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
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
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(BadgeCornerRadius))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
