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

        var hasSpecificDate = false

        // 1. Relative Dates (Kal, Parso, Aaj) with strict word boundaries
        if (Regex("\\bparso(n)?\\b").containsMatchIn(text)) {
            calendar.add(Calendar.DAY_OF_YEAR, 2)
            cleanTitle = cleanTitle.replace(Regex("\\bparso(n)?\\b"), "")
            hasSpecificDate = true
        } else if (Regex("\\bkal\\b").containsMatchIn(text)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            cleanTitle = cleanTitle.replace(Regex("\\bkal\\b"), "")
            hasSpecificDate = true
        } else if (Regex("\\baaj\\b").containsMatchIn(text)) {
            cleanTitle = cleanTitle.replace(Regex("\\baaj\\b"), "")
            hasSpecificDate = true
        }

        // 2. Specific Month & Date ("13 october", "14 oct", "october 13", "13 tareekh")
        val monthNames = "january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|oct|nov|dec|aktubar|aktobar|navambar|disambar"
        val monthMap = mapOf(
            "january" to 0, "jan" to 0,
            "february" to 1, "feb" to 1,
            "march" to 2, "mar" to 2,
            "april" to 3, "apr" to 3,
            "may" to 4,
            "june" to 5, "jun" to 5,
            "july" to 6, "jul" to 6,
            "august" to 7, "aug" to 7,
            "september" to 8, "sep" to 8,
            "october" to 9, "oct" to 9, "aktubar" to 9, "aktobar" to 9,
            "november" to 10, "nov" to 10, "navambar" to 10,
            "december" to 11, "dec" to 11, "disambar" to 11
        )

        // Pattern: "13 october" or "13th october"
        val dayMonthPattern = Regex("(\\d{1,2})(?:st|nd|rd|th)?\\s*($monthNames)")
        val dayMonthMatch = dayMonthPattern.find(text)

        // Pattern: "october 13" or "october 13th"
        val monthDayPattern = Regex("($monthNames)\\s*(\\d{1,2})(?:st|nd|rd|th)?")
        val monthDayMatch = monthDayPattern.find(text)

        if (dayMonthMatch != null) {
            val day = dayMonthMatch.groupValues[1].toIntOrNull()
            val monthStr = dayMonthMatch.groupValues[2]
            val month = monthMap[monthStr]
            if (day != null && day in 1..31 && month != null) {
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                cleanTitle = cleanTitle.replace(dayMonthMatch.value, "")
                hasSpecificDate = true
            }
        } else if (monthDayMatch != null) {
            val monthStr = monthDayMatch.groupValues[1]
            val day = monthDayMatch.groupValues[2].toIntOrNull()
            val month = monthMap[monthStr]
            if (day != null && day in 1..31 && month != null) {
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                cleanTitle = cleanTitle.replace(monthDayMatch.value, "")
                hasSpecificDate = true
            }
        } else {
            // Pattern: "10 tarikh", "15 tareekh", "5 date"
            val datePattern = Regex("(\\d{1,2})\\s*(tarikh|tareekh|tarik|tareek|date)")
            val dateMatch = datePattern.find(text)
            if (dateMatch != null) {
                val dayOfMonth = dateMatch.groupValues[1].toIntOrNull()
                if (dayOfMonth != null && dayOfMonth in 1..31) {
                    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    if (dayOfMonth < currentDay && !hasSpecificDate) {
                        calendar.add(Calendar.MONTH, 1)
                    }
                    cleanTitle = cleanTitle.replace(datePattern, "")
                    hasSpecificDate = true
                }
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

        // 4. Hour & Minute Extraction ("5 bje", "5:30 baje", "12 bje", "6 bje")
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
                if (!hasSpecificDate) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                calendar.set(Calendar.HOUR_OF_DAY, 9)
                calendar.set(Calendar.MINUTE, 0)
            }
        }

        // 5. Past Time Auto-Correction only if no specific date/month was explicitly provided
        if (!hasSpecificDate && calendar.timeInMillis <= System.currentTimeMillis()) {
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
