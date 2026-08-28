package com.example.service

import com.example.data.model.ReminderEntity
import com.example.data.model.RepeatType
import java.util.*
import java.util.regex.Pattern

data class ParsedVoiceData(
    val title: String,
    val timeMillis: Long,
    val cleanPrompt: String,
    val voiceScript: String,
    val repeatType: String = RepeatType.ONCE.name
)

object SmartVoiceParser {

    /**
     * Parses spoken natural language prompt in English, Hindi, or Hinglish into a ReminderEntity.
     */
    fun parse(rawPrompt: String, userName: String = "User"): ParsedVoiceData {
        val prompt = rawPrompt.trim()
        val lower = prompt.lowercase(Locale.ROOT)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var matchedTime = false
        var cleanedTitle = prompt
        var isDaily = lower.contains("daily") || lower.contains("har roz") || lower.contains("har din") || lower.contains("rozana") || lower.contains("every day")

        // 1. Check relative minutes ("in 10 minutes", "10 minute baad", "after 15 mins")
        val relMinPattern = Pattern.compile("(\\d+)\\s*(?:minutes?|mins?|minute|min)\\s*(?:baad|after|later)?", Pattern.CASE_INSENSITIVE)
        val relMinMatcher = relMinPattern.matcher(lower)
        if (relMinMatcher.find()) {
            val mins = relMinMatcher.group(1)?.toIntOrNull() ?: 10
            calendar.add(Calendar.MINUTE, mins)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            cleanedTitle = lower.replace(relMinMatcher.group(0) ?: "", "").replace("in ", "").trim()
            matchedTime = true
        }

        // 2. Check relative hours ("in 2 hours", "1 ghante baad", "after 1 hour")
        if (!matchedTime) {
            val relHourPattern = Pattern.compile("(\\d+)\\s*(?:hours?|hrs?|ghante|ghanta)\\s*(?:baad|after|later)?", Pattern.CASE_INSENSITIVE)
            val relHourMatcher = relHourPattern.matcher(lower)
            if (relHourMatcher.find()) {
                val hrs = relHourMatcher.group(1)?.toIntOrNull() ?: 1
                calendar.add(Calendar.HOUR_OF_DAY, hrs)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                cleanedTitle = lower.replace(relHourMatcher.group(0) ?: "", "").replace("in ", "").trim()
                matchedTime = true
            }
        }

        // 3. Check specific day markers: "tomorrow", "kal", "parso", "tonight", "aaj raat", "today", "aaj"
        var isTomorrow = lower.contains("tomorrow") || lower.contains("kal")
        val isDayAfterTomorrow = lower.contains("parso") || lower.contains("day after tomorrow")
        val isTonight = lower.contains("tonight") || lower.contains("aaj raat")

        if (isDayAfterTomorrow) {
            calendar.add(Calendar.DAY_OF_YEAR, 2)
            cleanedTitle = cleanedTitle.replace("day after tomorrow", "").replace("parso", "")
        } else if (isTomorrow) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            cleanedTitle = cleanedTitle.replace("tomorrow", "").replace("kal", "")
        }

        // 4. Time extraction (e.g. "at 9 AM", "9:30 pm", "subah 9 baje", "sham 6 baje", "dopahar 2 baje", "raat 10 baje", "7 baje")
        if (!matchedTime) {
            var hour = -1
            var minute = 0
            var isPm = false
            var isAm = false

            // Subah / Morning (AM)
            if (lower.contains("subah") || lower.contains("morning") || lower.contains("am")) {
                isAm = true
            }
            // Sham / Dopahar / Raat / Evening / Night / Afternoon / PM
            if (lower.contains("sham") || lower.contains("shaam") || lower.contains("dopahar") ||
                lower.contains("raat") || lower.contains("night") || lower.contains("evening") ||
                lower.contains("afternoon") || lower.contains("pm") || isTonight) {
                isPm = true
            }

            // Match "9:30", "09:30", "9"
            val timeRegex = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(?:baje|am|pm|o'clock)?", Pattern.CASE_INSENSITIVE)
            val timeMatcher = timeRegex.matcher(lower)

            while (timeMatcher.find()) {
                val h = timeMatcher.group(1)?.toIntOrNull()
                val m = timeMatcher.group(2)?.toIntOrNull() ?: 0
                if (h != null && h in 1..24) {
                    hour = h
                    minute = m
                    cleanedTitle = cleanedTitle.replace(timeMatcher.group(0) ?: "", "")
                    break
                }
            }

            if (hour != -1) {
                if (hour == 12) {
                    if (isAm) hour = 0
                } else if (isPm && hour < 12) {
                    hour += 12
                } else if (!isAm && !isPm) {
                    // Default heuristics if AM/PM not specified:
                    // If hour in 1..6 and no indicator, assume PM (e.g. 5 baje -> 5 PM)
                    if (hour in 1..6) {
                        hour += 12
                    }
                }

                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                matchedTime = true
            } else if (isTonight) {
                calendar.set(Calendar.HOUR_OF_DAY, 21) // 9:00 PM default tonight
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                matchedTime = true
            }
        }

        // Clean up common filler words in title
        val removeWords = listOf(
            "remind me to", "remind me", "set a reminder for", "set reminder to",
            "set reminder for", "set alarm for", "alarm for", "mujhe yaad dilana",
            "yaad dilana", "baje", "subah", "sham", "shaam", "dopahar", "raat",
            "at", "on", "for", "ki", "ko", "par", "please", "daily", "every day",
            "rozana", "har roz", "har din"
        )
        for (w in removeWords) {
            cleanedTitle = cleanedTitle.replace(Pattern.compile("\\b$w\\b", Pattern.CASE_INSENSITIVE).toRegex(), " ")
        }

        cleanedTitle = cleanedTitle.trim().replace("\\s+".toRegex(), " ")
        if (cleanedTitle.isBlank()) {
            cleanedTitle = prompt
        }

        // Capitalize first letter of title
        cleanedTitle = cleanedTitle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        // If calculated time is in the past, add 1 day
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val voiceScript = "Hello $userName, this is your reminder for $cleanedTitle"

        return ParsedVoiceData(
            title = cleanedTitle,
            timeMillis = calendar.timeInMillis,
            cleanPrompt = prompt,
            voiceScript = voiceScript,
            repeatType = if (isDaily) RepeatType.DAILY.name else RepeatType.ONCE.name
        )
    }
}

