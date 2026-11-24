package com.tagtrack.nfc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tagtrack.nfc.data.Item
import com.tagtrack.nfc.ui.theme.*

@Composable
fun TagTrackHeader(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(CardCornerRadius))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = actions
            )
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    type: BadgeType = BadgeType.STATUS
) {

    val ( bgColor, textColor) = when (type) {
        BadgeType.STATUS -> when (text.lowercase()) {
            "available" -> Green.copy(alpha = 0.15f) to Green
            "rented" -> Amber.copy(alpha = 0.15f) to Amber
            "unavailable", "damaged", "maintenance" -> Red.copy(alpha = 0.15f) to Red
            else -> BlueBronze.copy(alpha = 0.15f) to BlueBronze
        }
        BadgeType.CONDITION -> when (text.lowercase()) {
            "new", "excellent", "good" -> Green.copy(alpha = 0.15f) to Green
            "fair", "needs cleaning" -> Amber.copy(alpha = 0.15f) to Amber
            "poor", "damaged", "needs repair" -> Red.copy(alpha = 0.15f) to Red
            else -> BlueBronze.copy(alpha = 0.15f) to BlueBronze
        }
    }
//    val bgColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BadgeCornerRadius))
            //.background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                fontSize = 11.sp
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

enum class BadgeType {
    STATUS, CONDITION
}

@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit,
    onWriteNfcClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon / Thumbnail placeholder
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
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
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.itemName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))


                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(text = item.status, type = BadgeType.STATUS)
                    StatusBadge(text = item.condition, type = BadgeType.CONDITION)
                    if (!item.size.isNullOrBlank()) {
                        Text(
                            text = "Size: ${item.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            if (onWriteNfcClick != null) {
                IconButton(onClick = onWriteNfcClick) {
                    Icon(
                        imageVector = Icons.Rounded.Nfc,
                        contentDescription = "Write NFC",
                        tint = BlueBronze
                    )
                }
            }
        }
    }
}

@Composable
fun ItemTile(
    item: Item,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.25f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(4.dp))
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    StatusBadge(text = item.status, type = BadgeType.STATUS)
                    StatusBadge(text = item.condition, type = BadgeType.CONDITION)
                    Text(
                        text = "Qty: ${item.unitQuantity}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.color.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Color: ${item.color}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TagTrackBottomBar(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        NavigationBarItem(
            selected = currentTab == BottomNavTab.HOME,
            onClick = { onTabSelected(BottomNavTab.HOME) },
            icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BlueBronze,
                selectedTextColor = BlueBronze,
                indicatorColor = GoldSoft
            )
        )
        NavigationBarItem(
            selected = currentTab == BottomNavTab.INVENTORY,
            onClick = { onTabSelected(BottomNavTab.INVENTORY) },
            icon = { Icon(Icons.Rounded.Inventory2, contentDescription = "Inventory") },
            label = { Text("Inventory") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BlueBronze,
                selectedTextColor = BlueBronze,
                indicatorColor = GoldSoft
            )
        )
        NavigationBarItem(
            selected = currentTab == BottomNavTab.SCANNER,
            onClick = { onTabSelected(BottomNavTab.SCANNER) },
            icon = { Icon(Icons.Rounded.Nfc, contentDescription = "Scan NFC") },
            label = { Text("Scan NFC") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BlueBronze,
                selectedTextColor = BlueBronze,
                indicatorColor = GoldSoft
            )
        )
        NavigationBarItem(
            selected = currentTab == BottomNavTab.SETTINGS,
            onClick = { onTabSelected(BottomNavTab.SETTINGS) },
            icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BlueBronze,
                selectedTextColor = BlueBronze,
                indicatorColor = GoldSoft
            )
        )
    }
}

enum class BottomNavTab {
    HOME, INVENTORY, SCANNER, SETTINGS
}