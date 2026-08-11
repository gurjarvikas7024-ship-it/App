package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderEntity
import com.example.data.model.ReminderStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.HomeFilterTab
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: UiState,
    reminders: List<ReminderEntity>,
    selectedTab: HomeFilterTab,
    searchQuery: String,
    onSelectTab: (HomeFilterTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenVoiceDialog: () -> Unit,
    onOpenAddReminder: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPaywall: () -> Unit,
    onToggleCompleted: (Long) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onEditReminder: (ReminderEntity) -> Unit,
    onSnoozeReminder: (Long) -> Unit
) {
    // Calculations for Header Dashboard matching the reference photo
    val now = System.currentTimeMillis()
    val timeSdf = remember { SimpleDateFormat("hh:mm a", Locale.ENGLISH) }

    val todayReminders = remember(reminders) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentDay = calendar.get(Calendar.DAY_OF_YEAR)

        reminders.filter { r ->
            val rCal = Calendar.getInstance().apply { timeInMillis = r.timeMillis }
            rCal.get(Calendar.YEAR) == currentYear && rCal.get(Calendar.DAY_OF_YEAR) == currentDay
        }
    }

    val totalTodayCount = todayReminders.size
    val completedTodayCount = todayReminders.count { it.status == ReminderStatus.COMPLETED.name }
    val progressPercent = if (totalTodayCount > 0) ((completedTodayCount.toFloat() / totalTodayCount) * 100).toInt() else 0

    val nextReminder = remember(reminders) {
        reminders.filter { it.status == ReminderStatus.PENDING.name && it.timeMillis >= now }
            .minByOrNull { it.timeMillis }
            ?: reminders.firstOrNull { it.status == ReminderStatus.PENDING.name }
    }

    val nextReminderTimeFormatted = remember(nextReminder) {
        if (nextReminder != null) timeSdf.format(Date(nextReminder.timeMillis)) else "--:--"
    }

    Scaffold(
        containerColor = AppBackgroundDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackgroundDark,
                    titleContentColor = Color.White
                ),
                title = {
                    Column {
                        Text(
                            text = if (uiState.userName.isNotBlank() && uiState.userName != "User") "Hello, ${uiState.userName}!" else "Yaad AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Smart Offline AI Reminders",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenVoiceDialog) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Voice Assistant",
                            tint = OrangeAccent
                        )
                    }
                    IconButton(onClick = onOpenCalendar) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar View",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddReminder,
                containerColor = OrangeAccent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Add Reminder",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Primary "Set Reminder" Hero Card
            item {
                Card(
                    onClick = onOpenAddReminder,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = OrangeBannerBg),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, OrangeAccent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = OrangeAccent,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Set Reminder",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Title • Date & Time • Daily / Weekly / Monthly • AI Voice Script",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }

                        Button(
                            onClick = onOpenAddReminder,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Click Here", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Quick Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search active reminders...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppCardDark,
                        unfocusedContainerColor = AppCardDark,
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = AppCardBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Section Header & Active Filter Tabs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upcoming Reminders (Active)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${reminders.count { it.status == ReminderStatus.PENDING.name }} active",
                            fontSize = 13.sp,
                            color = OrangeAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Filter Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HomeFilterTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectTab(tab) },
                                label = { Text(tab.labelEnglish, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OrangeAccent,
                                    selectedLabelColor = Color.White,
                                    containerColor = AppCardDark,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = AppCardBorderDark,
                                    selectedBorderColor = OrangeAccent
                                )
                            )
                        }
                    }
                }
            }

            // Reminders List Items
            if (reminders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = TextSecondary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No reminders found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the + button to add your first reminder",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(reminders, key = { it.id }) { reminder ->
                    PhotoStyleReminderCard(
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

@Composable
fun PhotoStyleReminderCard(
    reminder: ReminderEntity,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onSnooze: () -> Unit
) {
    val timeSdf = remember { SimpleDateFormat("hh:mm a", Locale.ENGLISH) }
    val formattedTime = remember(reminder.timeMillis) { timeSdf.format(Date(reminder.timeMillis)) }
    val isCompleted = reminder.status == ReminderStatus.COMPLETED.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Accent Bar (Red/Coral)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (isCompleted) GreenAccent else RedAccentBar)
            )

            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon & Details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Category/Event Round Icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) GreenBadgeBg else RedIconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                reminder.title.contains("birthday", ignoreCase = true) -> Icons.Default.Cake
                                reminder.title.contains("medicine", ignoreCase = true) || reminder.title.contains("tab", ignoreCase = true) -> Icons.Default.Medication
                                reminder.title.contains("cook", ignoreCase = true) || reminder.title.contains("dinner", ignoreCase = true) -> Icons.Default.Restaurant
                                else -> Icons.Default.NotificationsActive
                            },
                            contentDescription = null,
                            tint = if (isCompleted) GreenAccent else Color(0xFFF43F5E),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formattedTime,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // AI Voice Badge
                            Surface(
                                color = OrangeBannerBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OrangeBannerBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = OrangeAccent,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "AI Voice",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangeAccent
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Repeat Pill
                            Surface(
                                color = Color(0xFF27272A),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = reminder.repeatType,
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status & Actions
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Status Badge (Pending / Completed / Missed)
                    val (badgeBg, badgeText, label) = when (reminder.status) {
                        ReminderStatus.COMPLETED.name -> Triple(GreenBadgeBg, GreenAccent, "Completed")
                        ReminderStatus.MISSED.name -> Triple(RedIconBg, RedAccentBar, "Missed")
                        else -> Triple(YellowPendingBg, YellowPendingText, "Pending")
                    }

                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedAccentBar, modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Big Round Green Check Button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GreenBadgeBg)
                                .clickable { onToggleCompleted() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Mark Done",
                                tint = GreenAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
