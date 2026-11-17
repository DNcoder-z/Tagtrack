package com.tagtrack.nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tagtrack.nfc.ui.components.TagTrackHeader
import com.tagtrack.nfc.ui.theme.BlueBronze
import com.tagtrack.nfc.ui.theme.CardCornerRadius
import com.tagtrack.nfc.ui.viewmodel.ScanMode

@Composable
fun ScannerScreen(
    scanMode: ScanMode = ScanMode.READ,
    onBackClick: () -> Unit,
    onSimulateScan: (Int) -> Unit
) {
    var sampleNfcInput by remember { mutableStateOf("100001") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TagTrackHeader(
            title = when (scanMode) {
                ScanMode.WRITE -> "Write NFC Tag"
                ScanMode.ERASE -> "Erase NFC Tag"
                else -> "Scan NFC Tag"
            },
            subtitle = when (scanMode) {
                ScanMode.WRITE -> "Hold tag near device to write"
                ScanMode.ERASE -> "Hold tag near device to erase"
                else -> "Ready to scan NFC tags"
            },
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
            // Pulse / NFC animation icon container
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(BlueBronze.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(BlueBronze.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (scanMode) {
                            ScanMode.ERASE -> Icons.Rounded.DeleteSweep
                            else -> Icons.Rounded.Nfc
                        },
                        contentDescription = null,
                        tint = BlueBronze,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = when (scanMode) {
                    ScanMode.WRITE -> "Ready to Write Tag"
                    ScanMode.ERASE -> "Ready to Erase Tag"
                    else -> "Listening for NFC Tag..."
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (scanMode) {
                    ScanMode.WRITE -> "Ensure the target NFC tag is close to the back of your device. Data will be encoded securely."
                    ScanMode.ERASE -> "Ensure the target NFC tag is close to the back of your device to clear its data."
                    else -> "Bring an NFC-enabled asset or tag close to your phone to view or verify its details instantly."
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = BlueBronze,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Simulator for testing without physical NFC tags
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Simulator / Test Scan",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (scanMode == ScanMode.READ) {
                        OutlinedTextField(
                            value = sampleNfcInput,
                            onValueChange = { sampleNfcInput = it },
                            label = { Text("Simulate NFC Code") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(CardCornerRadius),
                            singleLine = true
                        )
                    }
                    Button(
                        onClick = {
                            val code = sampleNfcInput.toIntOrNull() ?: 100001
                            onSimulateScan(code)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CardCornerRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = when (scanMode) {
                                ScanMode.ERASE -> Icons.Rounded.DeleteSweep
                                else -> Icons.Rounded.TouchApp
                            },
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when (scanMode) {
                                ScanMode.WRITE -> "Simulate Write"
                                ScanMode.ERASE -> "Simulate Erase"
                                else -> "Simulate Tap"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CardCornerRadius)
            ) {
                Text("Cancel")
            }
        }
    }
}
