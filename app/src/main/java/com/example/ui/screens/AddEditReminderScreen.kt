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
    var selectedVoicePreset by remember { mutableStateOf(existingReminder?.voicePreset ?: "Studio Female") }

    var ttsManager by remember { mutableStateOf<TTSManager?>(null) }

    DisposableEffect(Unit) {
        val manager = TTSManager(context)
        ttsManager = manager
        onDispose {
            manager.shutdown()
        }
    }

    val voiceProfiles = listOf(
        "Studio Female",
        "Executive Male",
        "Soft Narrator",
        "Bold Leader"
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
                placeholder = { Text("e.g. Order birthday gift for Priya") },
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

            // Repeat Options (Once, Daily, Weekly, Monthly)
            Text("Repeat Schedule:", fontWeight = FontWeight.Bold)
            val repeatDisplayMap = mapOf(
                RepeatType.ONCE.name to "Once",
                RepeatType.DAILY.name to "Daily",
                RepeatType.WEEKLY.name to "Weekly",
                RepeatType.MONTHLY.name to "Monthly"
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeatDisplayMap.forEach { (typeKey, labelStr) ->
                    val isSelected = repeatType == typeKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { repeatType = typeKey },
                        label = { Text(labelStr, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeAccent,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // AI Voice Announcement Script & Profile Input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customScript,
                    onValueChange = { customScript = it },
                    label = { Text("AI Voice Script (What Voice Speaks)") },
                    placeholder = { Text("e.g. I need to wake up at 5:00 AM") },
                    leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Text("Voice Profile:", style = MaterialTheme.typography.labelMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(voiceProfiles) { profile ->
                        val isSelected = selectedVoicePreset == profile
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedVoicePreset = profile },
                            label = { Text(profile, fontSize = 12.sp) }
                        )
                    }
                }

                // Test AI Voice Speech Button
                OutlinedButton(
                    onClick = {
                        val scriptToSpeak = if (customScript.isNotBlank()) customScript else if (title.isNotBlank()) title else "I need to wake up at 5:00 AM"
                        ttsManager?.speak(scriptToSpeak, selectedVoicePreset)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = OrangeAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test AI Voice Script ($selectedVoicePreset)")
                }
            }

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
                    if (existingReminder == null) "Set Reminder" else "Update Reminder",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
