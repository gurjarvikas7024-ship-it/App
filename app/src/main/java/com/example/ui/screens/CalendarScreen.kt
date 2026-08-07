package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    selectedDateMillis: Long,
    remindersForDate: List<ReminderEntity>,
    onSelectDate: (Long) -> Unit,
    onBack: () -> Unit,
    onToggleCompleted: (Long) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onEditReminder: (ReminderEntity) -> Unit,
    onSnoozeReminder: (Long) -> Unit
) {
    val currentMonthCal = remember { Calendar.getInstance().apply { timeInMillis = selectedDateMillis } }
    var displayedMonthCal by remember { mutableStateOf(Calendar.getInstance().apply { timeInMillis = selectedDateMillis }) }

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.ENGLISH) }
    val dayHeaderFormat = remember { listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") }

    val (selectedYear, selectedDayOfYear) = remember(selectedDateMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        Pair(cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR))
    }

    val (todayYear, todayDayOfYear) = remember {
        val cal = Calendar.getInstance()
        Pair(cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR))
    }

    // Calculate days for the grid
    val daysInMonth = remember(displayedMonthCal.timeInMillis) {
        val cal = displayedMonthCal.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val list = mutableListOf<Calendar?>()
        repeat(firstDayOfWeek) { list.add(null) }
        for (day in 1..maxDays) {
            val dayCal = cal.clone() as Calendar
            dayCal.set(Calendar.DAY_OF_MONTH, day)
            list.add(dayCal)
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("कैलेंडर व्यू (Calendar View)") },
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
        ) {
            // Month Header with Prev / Next
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val cal = displayedMonthCal.clone() as Calendar
                        cal.add(Calendar.MONTH, -1)
                        displayedMonthCal = cal
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month")
                    }

                    Text(
                        text = monthFormat.format(displayedMonthCal.time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = {
                        val cal = displayedMonthCal.clone() as Calendar
                        cal.add(Calendar.MONTH, 1)
                        displayedMonthCal = cal
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekday Headers
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                dayHeaderFormat.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Month Days Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                items(daysInMonth) { dayCal ->
                    if (dayCal == null) {
                        Spacer(modifier = Modifier.size(36.dp))
                    } else {
                        val isSelected = dayCal.get(Calendar.YEAR) == selectedYear && dayCal.get(Calendar.DAY_OF_YEAR) == selectedDayOfYear
                        val isToday = dayCal.get(Calendar.YEAR) == todayYear && dayCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(2.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    onSelectDate(dayCal.timeInMillis)
                                }
                        ) {
                            Text(
                                text = dayCal.get(Calendar.DAY_OF_MONTH).toString(),
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Reminders list for selected date
            Text(
                text = "चुनी गई तारीख के रिमाइंडर (${remindersForDate.size}):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (remindersForDate.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("इस तारीख को कोई रिमाइंडर नहीं है।", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(remindersForDate, key = { it.id }) { reminder ->
                        ReminderCardItem(
                            reminder = reminder,
                            onToggleCompleted = { onToggleCompleted(reminder.id) },
                            onDelete = { onDeleteReminder(reminder.id) },
                            onEdit = { onEditReminder(reminder) },
                            onSnooze = { onSnoozeReminder(reminder.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun isSameDay(millis1: Long, millis2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
