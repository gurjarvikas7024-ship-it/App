package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderEntity
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputBottomSheet(
    userName: String,
    onDismiss: () -> Unit,
    onParseVoicePrompt: (String, (ReminderEntity?) -> Unit) -> Unit,
    onSaveReminder: (ReminderEntity) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var selectedVoicePreset by remember { mutableStateOf("Studio Female") }
    var isRecording by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var previewReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val samplePrompts = listOf(
        "Test on Jan 20 at 6 AM",
        "Cook dinner at 7 PM",
        "Take medicine at 10 PM",
        "Wake me up tomorrow at 5 AM"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AmberAccent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Yaad AI Voice & Smart Reminder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Speak or type your reminder:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Voice Recording Circle Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(96.dp)
                    .scale(if (isRecording) pulseScale else 1f)
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isRecording) listOf(Color(0xFFE53935), Color(0xFFFF7043))
                            else listOf(IndigoPrimary, IndigoDark)
                        ),
                        shape = CircleShape
                    )
                    .clickable {
                        isRecording = !isRecording
                        if (!isRecording && textInput.isBlank()) {
                            textInput = samplePrompts.random()
                        }
                    }
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = if (isRecording) "Listening... Speak now!" else "Tap microphone to speak",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Text Input Box
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("e.g. Test on January 20 at 6 AM") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (textInput.isNotEmpty()) {
                        IconButton(onClick = { textInput = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sample Chips
            Text(
                text = "Suggestions (Tap to try):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(samplePrompts) { prompt ->
                    FilterChip(
                        selected = textInput == prompt,
                        onClick = {
                            textInput = prompt
                        },
                        label = { Text(prompt, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                Text("AI is processing date and time...", style = MaterialTheme.typography.bodyMedium)
            } else if (previewReminder != null) {
                // AI Result Preview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Detected Reminder:", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Title: ${previewReminder?.title}", fontWeight = FontWeight.Medium)
                        Text("Announcement: \"${previewReminder?.customVoiceScript}\"", style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                previewReminder?.let { onSaveReminder(it) }
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AlarmAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set Reminder")
                        }
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            isProcessing = true
                            errorMessage = null
                            onParseVoicePrompt(textInput) { reminder ->
                                isProcessing = false
                                if (reminder != null) {
                                    previewReminder = reminder.copy(voicePreset = selectedVoicePreset)
                                } else {
                                    errorMessage = "Error parsing reminder prompt with AI."
                                }
                            }
                        }
                    },
                    enabled = textInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create AI Reminder", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
