package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderEntity
import com.example.data.model.RepeatType
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
    var repeatType by remember { mutableStateOf(existingReminder?.repeatType ?: RepeatType.ONCE.name) }

    val calendar = remember {
        Calendar.getInstance().apply {
            if (existingReminder != null) {
                timeInMillis = existingReminder.timeMillis
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
            }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
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
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
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
            calendar.set(Calendar.MILLISECOND, 0)
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Reminder Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Reminder Title *") },
                placeholder = { Text("e.g. Take morning medicine") },
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            // Repeat Options (Once, Daily, Weekly, Monthly)
            Text("Repeat Schedule:", fontWeight = FontWeight.Bold)
            val repeatDisplayMap = mapOf(
                RepeatType.ONCE.name to "Once",
                RepeatType.DAILY.name to "Daily",
                RepeatType.WEEKLY.name to "Weekly",
                RepeatType.MONTHLY.name to "Monthly"
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                repeatDisplayMap.forEach { (typeKey, labelStr) ->
                    val isSelected = repeatType == typeKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { repeatType = typeKey },
                        label = { 
                            Text(
                                labelStr, 
                                fontSize = 12.sp, 
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false
                            ) 
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeAccent,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Date & Time Selectors Row:
            // When Daily is selected, HIDE Date Picker completely and only show Time Picker
            val isDaily = repeatType == RepeatType.DAILY.name

            if (isDaily) {
                // Daily: Only Time Picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Alarm Time (Repeats Every Day):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    OutlinedButton(
                        onClick = { timePickerDialog.show() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(20.dp), tint = OrangeAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ring Daily at $selectedTimeText", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Once / Weekly / Monthly: Both Date & Time Pickers
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
            }

            // Notes / Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes & Details (Optional)") },
                placeholder = { Text("Add additional details...") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Reminder Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        // Timing calculation with 0 seconds / 0 millis
                        val targetCal = Calendar.getInstance().apply {
                            if (isDaily) {
                                // Set today's date with chosen hour and minute
                                set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                                // If time already passed today, set for tomorrow
                                if (timeInMillis <= System.currentTimeMillis()) {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }
                            } else {
                                timeInMillis = calendar.timeInMillis
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                        }

                        val reminderToSave = ReminderEntity(
                            id = existingReminder?.id ?: 0,
                            title = title.trim(),
                            description = description.trim(),
                            timeMillis = targetCal.timeInMillis,
                            repeatType = repeatType,
                            isVoiceEnabled = false,
                            voicePreset = "",
                            customVoiceScript = title.trim(),
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
