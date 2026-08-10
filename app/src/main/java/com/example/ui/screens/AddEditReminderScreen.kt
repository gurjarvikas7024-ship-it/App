package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.ReminderEntity
import com.example.data.model.RepeatType
import com.example.service.TTSManager
import com.example.ui.theme.OrangeAccent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    userName: String,
    existingReminder: ReminderEntity?,
    onBack: () -> Unit,
    onSave: (ReminderEntity) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(existingReminder?.title ?: "") }
    var description by remember { mutableStateOf(existingReminder?.description ?: "") }
    var customScript by remember { mutableStateOf(existingReminder?.customVoiceScript ?: "") }
    var repeatType by remember { mutableStateOf(existingReminder?.repeatType ?: RepeatType.ONCE.name) }
    var isVoiceEnabled by remember { mutableStateOf(existingReminder?.isVoiceEnabled ?: true) }
    var selectedVoicePreset by remember { mutableStateOf(existingReminder?.voicePreset ?: "Jethalal") }

    var ttsManager by remember { mutableStateOf<TTSManager?>(null) }

    DisposableEffect(Unit) {
        val manager = TTSManager(context)
        ttsManager = manager
        onDispose {
            manager.shutdown()
        }
    }

    val characterOptions = listOf(
        "Jethalal" to "TMKOC",
        "Motu" to "Motu Patlu",
        "Patlu" to "Motu Patlu",
        "Daya Bhabhi" to "TMKOC",
        "Inspector Daya" to "CID",
        "Studio Female" to "Standard"
    )

    val calendar = remember {
        Calendar.getInstance().apply {
            if (existingReminder != null) {
                timeInMillis = existingReminder.timeMillis
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
            }
        }
    }

    var selectedDateText by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(calendar.time))
    }
    var selectedTimeText by remember {
        mutableStateOf(SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(calendar.time))
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            selectedDateText = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            selectedTimeText = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(calendar.time)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existingReminder == null) "New Reminder" else "Edit Reminder",
                        fontWeight = FontWeight.Bold
                    )
                },
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
            // Reminder Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Reminder Title *") },
                placeholder = { Text("e.g. Priya ka birthday - gift order karna") },
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            // Date & Time Selectors Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(selectedDateText, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { timePickerDialog.show() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(selectedTimeText, fontSize = 13.sp)
                }
            }

            // Character Voice Choice (Jethalal, Motu, Patlu, Daya Bhabhi, Inspector Daya)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Character Voice:", fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(characterOptions) { (name, tag) ->
                        val isSelected = selectedVoicePreset == name
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedVoicePreset = name },
                            label = { Text("$name ($tag)", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.VolumeUp else Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangeAccent,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Test Character Voice Button
                OutlinedButton(
                    onClick = {
                        val textToSpeak = if (title.isNotBlank()) title else "Priya ka birthday - gift order karna"
                        ttsManager?.speak(textToSpeak, selectedVoicePreset)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = OrangeAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Suno '$selectedVoicePreset' ki voice me")
                }
            }

            // Repeat Options
            Text("Repeat Schedule:", fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RepeatType.entries.filter { it != RepeatType.CUSTOM }.forEach { type ->
                    FilterChip(
                        selected = repeatType == type.name,
                        onClick = { repeatType = type.name },
                        label = { Text(type.labelEnglish, fontSize = 12.sp) }
                    )
                }
            }

            // AI Voice Announcement Script Input (Optional Override)
            OutlinedTextField(
                value = customScript,
                onValueChange = { customScript = it },
                label = { Text("Custom Script (Optional)") },
                placeholder = { Text("e.g. Gift order karna mat bhoolna!") },
                leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            // Notes / Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Add additional details...") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Save Reminder Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val reminderToSave = ReminderEntity(
                            id = existingReminder?.id ?: 0,
                            title = title.trim(),
                            description = description.trim(),
                            timeMillis = calendar.timeInMillis,
                            repeatType = repeatType,
                            isVoiceEnabled = isVoiceEnabled,
                            voicePreset = selectedVoicePreset,
                            customVoiceScript = if (customScript.isNotBlank()) customScript.trim()
                            else title.trim(),
                            status = existingReminder?.status ?: "PENDING"
                        )
                        onSave(reminderToSave)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (existingReminder == null) "Save Reminder" else "Update Reminder",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
