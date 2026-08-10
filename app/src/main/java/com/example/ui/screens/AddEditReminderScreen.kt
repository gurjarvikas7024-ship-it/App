package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderEntity
import com.example.data.model.RepeatType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    userName: String,
    existingReminder: ReminderEntity? = null,
    onBack: () -> Unit,
    onSave: (ReminderEntity) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(existingReminder?.title ?: "") }
    var description by remember { mutableStateOf(existingReminder?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(existingReminder?.category ?: ReminderCategory.PERSONAL.name) }
    var selectedRepeat by remember { mutableStateOf(existingReminder?.repeatType ?: RepeatType.ONCE.name) }
    var voiceScript by remember { mutableStateOf(existingReminder?.customVoiceScript ?: "") }

    val calendar = remember {
        Calendar.getInstance().apply {
            if (existingReminder != null) {
                timeInMillis = existingReminder.timeMillis
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
            }
        }
    }

    var selectedTimeMillis by remember { mutableLongStateOf(calendar.timeInMillis) }

    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.ENGLISH) }

    var dateText by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var timeText by remember { mutableStateOf(timeFormat.format(calendar.time)) }

    fun updateVoiceScriptDefault() {
        if (voiceScript.isBlank()) {
            val namePrefix = if (userName.isBlank() || userName == "User") "" else "$userName जी, "
            voiceScript = "${namePrefix}उठ जाइए। आपका ${title.ifBlank { "रिमाइंडर" }} का समय हो गया है।"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingReminder == null) "नया रिमाइंडर" else "संपादित करें") },
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
            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    updateVoiceScriptDefault()
                },
                label = { Text("रिमाइंडर का नाम") },
                placeholder = { Text("जैसे: दवा लेनी है, मीटिंग") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Date and Time Pickers
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedCard(
                    onClick = {
                        val dpd = DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, day)
                                selectedTimeMillis = calendar.timeInMillis
                                dateText = dateFormat.format(calendar.time)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        dpd.show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("तारीख", style = MaterialTheme.typography.labelSmall)
                            Text(dateText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                OutlinedCard(
                    onClick = {
                        val tpd = TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                calendar.set(Calendar.HOUR_OF_DAY, hour)
                                calendar.set(Calendar.MINUTE, minute)
                                selectedTimeMillis = calendar.timeInMillis
                                timeText = timeFormat.format(calendar.time)
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                        )
                        tpd.show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("समय", style = MaterialTheme.typography.labelSmall)
                            Text(timeText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Category Selector
            Text("कैटेगरी:", fontWeight = FontWeight.Bold)
            var categoryExpanded by remember { mutableStateOf(false) }
            val currentCategoryEnum = try { ReminderCategory.valueOf(selectedCategory) } catch (e: Exception) { ReminderCategory.PERSONAL }

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = currentCategoryEnum.displayNameHindi,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    ReminderCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.displayNameHindi) },
                            onClick = {
                                selectedCategory = cat.name
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Repeat Options
            Text("पुनरावृत्ति:", fontWeight = FontWeight.Bold)
            var repeatExpanded by remember { mutableStateOf(false) }
            val currentRepeatEnum = try { RepeatType.valueOf(selectedRepeat) } catch (e: Exception) { RepeatType.ONCE }

            ExposedDropdownMenuBox(
                expanded = repeatExpanded,
                onExpandedChange = { repeatExpanded = !repeatExpanded }
            ) {
                OutlinedTextField(
                    value = currentRepeatEnum.labelHindi,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = repeatExpanded,
                    onDismissRequest = { repeatExpanded = false }
                ) {
                    RepeatType.entries.forEach { rep ->
                        DropdownMenuItem(
                            text = { Text(rep.labelHindi) },
                            onClick = {
                                selectedRepeat = rep.name
                                repeatExpanded = false
                            }
                        )
                    }
                }
            }

            // Custom Voice Script
            Text("AI आवाज़ संदेश:", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = voiceScript,
                onValueChange = { voiceScript = it },
                label = { Text("अलार्म बजने पर AI यह बोलेगा") },
                placeholder = { Text("समय हो गया है। अपना काम कर लें।") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            // Additional Notes
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("अतिरिक्त नोट्स (Optional Description)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Save Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val finalScript = voiceScript.ifBlank {
                            "$userName जी! आपका रिमाइंडर: $title"
                        }
                        val reminder = ReminderEntity(
                            id = existingReminder?.id ?: 0L,
                            title = title,
                            description = description,
                            timeMillis = selectedTimeMillis,
                            category = selectedCategory,
                            repeatType = selectedRepeat,
                            customVoiceScript = finalScript
                        )
                        onSave(reminder)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("रिमाइंडर सेव करें", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
