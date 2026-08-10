package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPaywallScreen(
    isCurrentlyPremium: Boolean,
    onBack: () -> Unit,
    onActivatePremium: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf("YEARLY") } // "MONTHLY" or "YEARLY"
    var showBillingDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yaad AI Premium", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Crown Badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(AmberAccent, Color(0xFFFF6F00))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Unlock Unlimited AI Features",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Premium experience for students, work & productivity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Feature Highlights List
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FeatureRow("Unlimited Active Reminders")
                        FeatureRow("Unlimited AI Voice Parsing & Announcements")
                        FeatureRow("Natural High-Definition Voice Presets")
                        FeatureRow("100% Ad-Free Experience")
                        FeatureRow("Full Screen Alarm Voice Alerts")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Plan Options
                Text("Select Plan:", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))

                Spacer(modifier = Modifier.height(8.dp))

                // Monthly Plan Option
                Card(
                    onClick = { selectedPlan = "MONTHLY" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (selectedPlan == "MONTHLY") 2.dp else 1.dp,
                            color = if (selectedPlan == "MONTHLY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPlan == "MONTHLY") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedPlan == "MONTHLY",
                                onClick = { selectedPlan = "MONTHLY" }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Monthly Plan", fontWeight = FontWeight.Bold)
                                Text("Auto-renews monthly", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("$1.99 / mo", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Yearly Plan Option (Discounted)
                Card(
                    onClick = { selectedPlan = "YEARLY" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (selectedPlan == "YEARLY") 2.dp else 1.dp,
                            color = if (selectedPlan == "YEARLY") AmberAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPlan == "YEARLY") MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedPlan == "YEARLY",
                                onClick = { selectedPlan = "YEARLY" }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Yearly Plan", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = AmberAccent,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("BEST VALUE (38% OFF)", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Text("$0.99 / mo ($11.99/yr)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("$11.99 / yr", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AmberAccent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subscribe Button
            Button(
                onClick = { showBillingDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IndigoPrimary
                )
            ) {
                Icon(Icons.Default.Payment, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCurrentlyPremium) "Currently Subscribed" else "Pay ${if (selectedPlan == "MONTHLY") "$1.99" else "$11.99"}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Google Play Billing Checkout Simulation Dialog
        if (showBillingDialog) {
            AlertDialog(
                onDismissRequest = { showBillingDialog = false },
                icon = { Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Google Play Billing") },
                text = {
                    Column {
                        Text("Yaad AI Premium Subscription (${if (selectedPlan == "MONTHLY") "$1.99/mo" else "$11.99/yr"})")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Processing purchase securely via Google Play...", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBillingDialog = false
                            onActivatePremium()
                            onBack()
                        }
                    ) {
                        Text("1-Tap Buy / Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBillingDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
