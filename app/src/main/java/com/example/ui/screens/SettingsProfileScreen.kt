package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.TTSManager
import com.example.ui.theme.AmberAccent
import com.example.ui.viewmodel.UiState

data class VoicePresetOption(
    val name: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsProfileScreen(
    uiState: UiState,
    onBack: () -> Unit,
    onSaveName: (String) -> Unit,
    onSaveVoiceSettings: (String, String, String) -> Unit,
    onToggleDarkMode: (Boolean?) -> Unit,
    onOpenPaywall: () -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember(uiState.userName) { mutableStateOf(uiState.userName) }
    var selectedPreset by remember(uiState.voicePreset) { mutableStateOf(uiState.voicePreset) }

    val voicePresets = listOf(
        VoicePresetOption("Studio Female", "Crisp & Natural Sounding", Icons.Default.VolumeUp),
        VoicePresetOption("Executive Male", "Deep & Professional Tone", Icons.Default.VoiceOverOff),
        VoicePresetOption("Soft Narrator", "Warm & Calming Tone", Icons.Default.RecordVoiceOver),
        VoicePresetOption("Bold Leader", "Expressive & Clear Speech", Icons.Default.Campaign)
    )

    var ttsManager by remember { mutableStateOf<TTSManager?>(null) }

    DisposableEffect(Unit) {
        val manager = TTSManager(context)
        ttsManager = manager
        onDispose {
            manager.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Voice Options", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Profile Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Your Name") },
                        placeholder = { Text("Enter your name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        trailingIcon = {
                            if (nameInput != uiState.userName) {
                                IconButton(onClick = { onSaveName(nameInput) }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save Name", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            // High-Definition AI Voice Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Voice Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "AI Voice reads out your exact script or reminder title word-for-word when the alarm triggers.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    voicePresets.forEach { preset ->
                        val isSelected = selectedPreset == preset.name
                        Card(
                            onClick = {
                                selectedPreset = preset.name
                                onSaveVoiceSettings(
                                    uiState.language,
                                    if (preset.name.contains("Female") || preset.name.contains("Soft")) "FEMALE" else "MALE",
                                    preset.name
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedPreset = preset.name
                                            onSaveVoiceSettings(
                                                uiState.language,
                                                if (preset.name.contains("Female") || preset.name.contains("Soft")) "FEMALE" else "MALE",
                                                preset.name
                                            )
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(preset.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(preset.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(preset.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Test Selected Voice Button
                    OutlinedButton(
                        onClick = {
                            val sampleText = "I will remind you at 5:00 AM."
                            ttsManager?.speak(sampleText, selectedPreset)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Selected Voice ('I will remind you at 5:00 AM')")
                    }
                }
            }

            // Subscription & Membership Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Account Status", fontWeight = FontWeight.Bold)
                            Text(
                                if (uiState.isPremium) "Premium Unlimited Subscriber" else "Free Plan (Max 5 active reminders)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!uiState.isPremium) {
                            Button(
                                onClick = onOpenPaywall,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Upgrade")
                            }
                        }
                    }
                }
            }
        }
    }
}
