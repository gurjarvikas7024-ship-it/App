package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onSaveUser: (String, String, String, Boolean, String, String) -> Unit
) {
    var nameInput by remember { mutableStateOf("User") }
    var selectedLanguage by remember { mutableStateOf("hi") }
    var selectedVoiceGender by remember { mutableStateOf("FEMALE") }
    var authProvider by remember { mutableStateOf("GUEST") }
    var userEmail by remember { mutableStateOf("user@example.com") }
    var currentStep by remember { mutableIntStateOf(1) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Hero Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Yaad AI Logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "याद AI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "आपका अपना स्मार्ट AI वॉइस अलार्म और रिमाइंडर",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (currentStep == 1) {
                // Step 1: Enter Name & Auth Choice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "आपका नाम क्या है?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "अलार्म के समय AI आपको इसी नाम से पुकारेगा",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("आपका नाम (e.g. Rahul)") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "लॉगिन का माध्यम चुनें:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Auth Option Buttons
                        OutlinedButton(
                            onClick = { authProvider = "GOOGLE" },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (authProvider == "GOOGLE") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Google Account से जारी रखें")
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = { authProvider = "PHONE" },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (authProvider == "PHONE") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("मोबाइल नंबर (OTP) से लॉगिन")
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = { authProvider = "GUEST" },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (authProvider == "GUEST") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.PersonOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("गेस्ट के रूप में शुरू करें")
                        }
                    }
                }
            } else {
                // Step 2: Language and Voice Gender Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "आवाज़ और भाषा चुनें",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("पसंदीदा भाषा:", fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            FilterChip(
                                selected = selectedLanguage == "hi",
                                onClick = { selectedLanguage = "hi" },
                                label = { Text("हिंदी (Hindi)") },
                                leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedLanguage == "en",
                                onClick = { selectedLanguage = "en" },
                                label = { Text("English") },
                                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("AI की आवाज़:", fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            FilterChip(
                                selected = selectedVoiceGender == "FEMALE",
                                onClick = { selectedVoiceGender = "FEMALE" },
                                label = { Text("महिला (Female)") },
                                leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedVoiceGender == "MALE",
                                onClick = { selectedVoiceGender = "MALE" },
                                label = { Text("पुरुष (Male)") },
                                leadingIcon = { Icon(Icons.Default.VoiceOverOff, contentDescription = null) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 1) {
                    TextButton(onClick = { currentStep = 1 }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("पीछे जाएं")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStep == 1) {
                            currentStep = 2
                        } else {
                            val finalName = nameInput.ifBlank { "User" }
                            onSaveUser(finalName, selectedLanguage, selectedVoiceGender, true, userEmail, authProvider)
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (currentStep == 1) "आगे बढ़ें" else "ऐप शुरू करें")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
