package com.memoryplus.app

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ParsedReminder(
    val title: String,
    val targetTimeMillis: Long,
    val formattedDateText: String
)

object HindiDateTimeParser {

    fun parseVoiceText(inputText: String): ParsedReminder {
        val text = inputText.lowercase().trim()
        val calendar = Calendar.getInstance()
        var cleanTitle = text

        // 1. Relative Dates (Kal, Parso, Aaj)
        when {
            text.contains("parso") || text.contains("parson") -> {
                calendar.add(Calendar.DAY_OF_YEAR, 2)
                cleanTitle = cleanTitle.replace(Regex("\\bparso(n)?\\b"), "")
            }
            text.contains("kal") -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                cleanTitle = cleanTitle.replace(Regex("\\bkal\\b"), "")
            }
            text.contains("aaj") -> {
                cleanTitle = cleanTitle.replace(Regex("\\baaj\\b"), "")
            }
        }

        // 2. Specific Date ("10 tarikh", "15 tareekh", "5 date")
        val datePattern = Regex("(\\d{1,2})\\s*(tarikh|tareekh|tarik|tareek|date)")
        val dateMatch = datePattern.find(text)
        if (dateMatch != null) {
            val dayOfMonth = dateMatch.groupValues[1].toIntOrNull()
            if (dayOfMonth != null && dayOfMonth in 1..31) {
                val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                if (dayOfMonth < currentDay && !text.contains("kal") && !text.contains("parso")) {
                    calendar.add(Calendar.MONTH, 1)
                }
                cleanTitle = cleanTitle.replace(datePattern, "")
            }
        }

        // 3. Time Modifiers (Subah, Dopahar, Sham, Raat)
        var isPM = false
        var isAM = false

        if (text.contains("subah") || text.contains("morning")) {
            isAM = true
            cleanTitle = cleanTitle.replace(Regex("\\b(subah|morning)\\b"), "")
        } else if (text.contains("dopahar") || text.contains("afternoon")) {
            isPM = true
            cleanTitle = cleanTitle.replace(Regex("\\b(dopahar|afternoon)\\b"), "")
        } else if (text.contains("shaam") || text.contains("sham") || text.contains("evening")) {
            isPM = true
            cleanTitle = cleanTitle.replace(Regex("\\b(shaam|sham|evening)\\b"), "")
        } else if (text.contains("raat") || text.contains("night")) {
            isPM = true
            cleanTitle = cleanTitle.replace(Regex("\\b(raat|night)\\b"), "")
        }

        // 4. Hour & Minute Extraction ("5 bje", "5:30 baje", "12 bje")
        val timePattern = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(baje|bje|am|pm)?")
        val timeMatches = timePattern.findAll(text)
        var hourFound: Int? = null
        var minuteFound = 0

        for (match in timeMatches) {
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..24) {
                hourFound = num
                minuteFound = match.groupValues[2].toIntOrNull() ?: 0
                cleanTitle = cleanTitle.replace(match.value, "")
                break
            }
        }

        if (hourFound != null) {
            var hour = hourFound
            if (isPM && hour < 12) {
                hour += 12
            } else if (isAM && hour == 12) {
                hour = 0
            }
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minuteFound)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        } else {
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 9)
                calendar.set(Calendar.MINUTE, 0)
            }
        }

        // 5. Past Time Auto-Correction
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 6. Clean Title
        val finalTitle = cleanTitle
            .replace(Regex("\\b(ko|ka|ke liye|hai|par|me|mein|set karo|remind me|reminder)\\b", RegexOption.IGNORE_CASE), "")
            .trim()
            .replace(Regex("\\s+"), " ")
            .ifEmpty { "Voice Reminder" }

        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return ParsedReminder(
            title = finalTitle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
            targetTimeMillis = calendar.timeInMillis,
            formattedDateText = sdf.format(calendar.time)
        )
    }
}
