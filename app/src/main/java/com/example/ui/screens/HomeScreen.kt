package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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
    on1TapMic: () -> Unit,
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
    val context = LocalContext.current
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

    Scaffold(
        containerColor = LightSurfaceBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CleanPureWhite,
                    titleContentColor = DeepSlateNavy
                ),
                title = {
                    Column {
                        Text(
                            text = if (uiState.userName.isNotBlank() && uiState.userName != "User") "Hello, ${uiState.userName}!" else "Memory Plus",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepSlateNavy
                        )
                        Text(
                            text = "Smart Voice & Alarm Reminders",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMutedText
                        )
                    }
                },
                actions = {
                    IconButton(onClick = on1TapMic) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "1-Tap Voice Reminder",
                            tint = OceanBlueAccent
                        )
                    }
                    IconButton(onClick = onOpenCalendar) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar View",
                            tint = DeepSlateNavy
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = DeepSlateNavy
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = on1TapMic,
                containerColor = OceanBlueAccent,
                contentColor = Color.White,
                shape = CircleShape,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "1-Tap Mic",
                        modifier = Modifier.size(26.dp)
                    )
                },
                text = {
                    Text("1-Tap Mic", fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
        ) {

            // Trial / Pro Status Banner
            item {
                if (uiState.isProUnlocked || uiState.isPremium) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "⭐ Memory Plus Pro Activated",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        text = "Unlimited Alarms • AI Voice • Battery Exact",
                                        fontSize = 12.sp,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        onClick = onOpenPaywall,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Free Trial: ${uiState.remainingFreeCount} of 2 reminders left",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = if (uiState.remainingFreeCount > 0) "Create up to 2 reminders free" else "Limit reached! Upgrade to unlock unlimited",
                                        fontSize = 11.sp,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                            Button(
                                onClick = onOpenPaywall,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Upgrade", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Primary "Set Reminder" Hero Card in Sky Blue Container
            item {
                Card(
                    onClick = onOpenAddReminder,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SkyBlueContainer),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SkyBorderColor)
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
                                    tint = OceanBlueAccent,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Set Reminder",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepSlateNavy
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Title • Date & Time • Daily / Weekly / Monthly • AI Voice Script",
                                fontSize = 12.sp,
                                color = SlateMutedText,
                                lineHeight = 16.sp
                            )
                        }

                        Button(
                            onClick = onOpenAddReminder,
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlueAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Quick Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search active reminders...", color = SlateMutedText) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateMutedText) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SlateMutedText)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CleanPureWhite,
                        unfocusedContainerColor = CleanPureWhite,
                        focusedBorderColor = OceanBlueAccent,
                        unfocusedBorderColor = SkyBorderColor,
                        focusedTextColor = DeepSlateNavy,
                        unfocusedTextColor = DeepSlateNavy
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
                            text = "Upcoming Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepSlateNavy
                        )
                        Text(
                            text = "${reminders.count { it.status == ReminderStatus.PENDING.name }} active",
                            fontSize = 13.sp,
                            color = OceanBlueAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Filter Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        HomeFilterTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectTab(tab) },
                                label = { 
                                    Text(
                                        tab.labelEnglish, 
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    ) 
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OceanBlueAccent,
                                    selectedLabelColor = Color.White,
                                    containerColor = CleanPureWhite,
                                    labelColor = SlateMutedText
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = SkyBorderColor,
                                    selectedBorderColor = OceanBlueAccent
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
                                tint = SlateMutedText.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No reminders found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepSlateNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the + button to add your first reminder",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateMutedText
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
        colors = CardDefaults.cardColors(containerColor = CleanPureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, SkyBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Accent Bar (Green for completed, Electric Ocean Blue for active)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (isCompleted) SuccessGreen else OceanBlueAccent)
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
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) SuccessGreenBg else SkyBlueContainer),
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
                            tint = if (isCompleted) SuccessGreen else OceanBlueAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepSlateNavy,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = SlateMutedText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formattedTime,
                                fontSize = 12.sp,
                                color = SlateMutedText
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // AI Voice Badge
                            Surface(
                                color = SkyBlueContainer,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SkyBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = OceanBlueAccent,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "AI Voice",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OceanBlueAccent
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Repeat Pill
                            Surface(
                                color = LightSurfaceBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = reminder.repeatType,
                                    fontSize = 10.sp,
                                    color = SlateMutedText,
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
                    val (badgeBg, badgeText, label) = when (reminder.status) {
                        ReminderStatus.COMPLETED.name -> Triple(SuccessGreenBg, SuccessGreen, "Completed")
                        ReminderStatus.MISSED.name -> Triple(UrgentRedBg, UrgentRed, "Missed")
                        else -> Triple(SkyBlueContainer, OceanBlueAccent, "Pending")
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
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SlateMutedText, modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = UrgentRed, modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SuccessGreenBg)
                                .clickable { onToggleCompleted() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Mark Done",
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
