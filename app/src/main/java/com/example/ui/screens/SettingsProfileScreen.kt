package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.TTSManager
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsProfileScreen(
    uiState: UiState,
    onBack: () -> Unit,
    onSaveName: (String) -> Unit,
    onSetVoiceSettings: (String, String) -> Unit,
    onSetDarkMode: (Boolean?) -> Unit,
    onOpenPaywall: () -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf(uiState.userName) }
    var language by remember { mutableStateOf(uiState.language) }
    var voiceGender by remember { mutableStateOf(uiState.voiceGender) }
    var isTestingTTS by remember { mutableStateOf(false) }

    val ttsManager = remember { TTSManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("सेटिंग्स और प्रोफाइल (Settings)") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = nameInput.ifBlank { "U" }.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = nameInput,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.userEmail} (${uiState.loginProvider})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AssistChip(
                            onClick = onOpenPaywall,
                            label = {
                                Text(
                                    if (uiState.isPremium) "👑 Premium Active" else "Free Plan (5 Limit)",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Star, contentDescription = null, tint = AmberAccent)
                            }
                        )
                    }
                }
            }

            // Edit Profile Name
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("आपका नाम बदलें (Edit Name)", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = { onSaveName(nameInput) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("सेव")
                        }
                    }
                }
            }

            // Voice & Speech Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI की भाषा और आवाज़ (Voice Settings)", fontWeight = FontWeight.Bold)
                    }

                    Text("भाषा chọn करें:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = language == "hi",
                            onClick = {
                                language = "hi"
                                onSetVoiceSettings(language, voiceGender)
                            },
                            label = { Text("हिंदी (Hindi)") }
                        )
                        FilterChip(
                            selected = language == "en",
                            onClick = {
                                language = "en"
                                onSetVoiceSettings(language, voiceGender)
                            },
                            label = { Text("English") }
                        )
                    }

                    Text("आवाज़ चुने (Voice Gender):", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = voiceGender == "FEMALE",
                            onClick = {
                                voiceGender = "FEMALE"
                                onSetVoiceSettings(language, voiceGender)
                            },
                            label = { Text("महिला (Female)") }
                        )
                        FilterChip(
                            selected = voiceGender == "MALE",
                            onClick = {
                                voiceGender = "MALE"
                                onSetVoiceSettings(language, voiceGender)
                            },
                            label = { Text("पुरुष (Male)") }
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val sampleText = "$nameInput जी, आपकी आवाज़ और भाषा की सेटिंग सेव हो गई है!"
                            ttsManager.speak(sampleText, voiceGender)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("आवाज़ टेस्ट करें (Test TTS Voice)")
                    }
                }
            }

            // Appearance Mode
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("थीम सेटिंग्स (Theme Mode)", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uiState.isDarkMode == null,
                            onClick = { onSetDarkMode(null) },
                            label = { Text("System Default") }
                        )
                        FilterChip(
                            selected = uiState.isDarkMode == false,
                            onClick = { onSetDarkMode(false) },
                            label = { Text("Light Mode ☀️") }
                        )
                        FilterChip(
                            selected = uiState.isDarkMode == true,
                            onClick = { onSetDarkMode(true) },
                            label = { Text("Dark Mode 🌙") }
                        )
                    }
                }
            }

            // Premium Plan Upgrade Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = AmberAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("याद AI Premium Plan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Unlimited Reminders & AI Voice Parsing\n• Cloud Backup & Restore\n• Family Reminder Sharing\n• No Ads & Priority Support")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenPaywall,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (uiState.isPremium) "Manage Subscription" else "₹40/महीना - अभी अपग्रेड करें")
                    }
                }
            }
        }
    }
}
