package com.tagtrack.nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tagtrack.nfc.data.Item
import com.tagtrack.nfc.ui.components.ItemCard
import com.tagtrack.nfc.ui.components.TagTrackHeader
import com.tagtrack.nfc.ui.theme.CardCornerRadius
import com.tagtrack.nfc.ui.theme.Green
import com.tagtrack.nfc.ui.theme.Red

@Composable
fun ScanResultScreen(
    scannedNfcCode: Int,
    item: Item?,
    onBackClick: () -> Unit,
    onViewDetails: (Item) -> Unit,
    onRegisterItem: (Int) -> Unit,
    onScanAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TagTrackHeader(
            title = "Scan Result",
            subtitle = "NFC Code: #$scannedNfcCode",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (item != null) {
                // Registered Item Found
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Registered",
                    tint = Green,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Registered Asset Found",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This tag is linked to an existing inventory item.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                ItemCard(item = item, onClick = { onViewDetails(item) })

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onViewDetails(item) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(CardCornerRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Full Details", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onScanAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(CardCornerRadius)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Another Tag")
                }
            } else {
                // Unregistered Tag
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Unregistered",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Unregistered NFC Tag",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tag #$scannedNfcCode is not yet registered in the TagTrack database.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onRegisterItem(scannedNfcCode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(CardCornerRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register Asset with this Tag", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onScanAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(CardCornerRadius)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Another Tag")
                }
            }
        }
    }
}
