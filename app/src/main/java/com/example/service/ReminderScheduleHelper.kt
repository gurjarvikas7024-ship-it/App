package com.example.service

import com.example.data.model.RepeatType
import java.util.Calendar

object ReminderScheduleHelper {

    /**
     * Checks if a repeatType string corresponds to a recurring reminder
     */
    fun isRecurring(repeatType: String?): Boolean {
        if (repeatType.isNullOrBlank()) return false
        return repeatType.equals(RepeatType.DAILY.name, ignoreCase = true) ||
               repeatType.equals(RepeatType.WEEKLY.name, ignoreCase = true) ||
               repeatType.equals(RepeatType.MONTHLY.name, ignoreCase = true) ||
               repeatType.equals(RepeatType.YEARLY.name, ignoreCase = true)
    }

    /**
     * Calculates the next trigger timestamp strictly in the future (> fromTimeMillis).
     *
     * - DAILY: Repeats every day at the exact same hour and minute.
     * - WEEKLY: Repeats every 7 days (same day of week, e.g. every Monday) at the exact same hour and minute.
     * - MONTHLY: Repeats every month on the exact same day of month (e.g. 7th Sep -> 7th Oct -> 7th Nov -> 7th Dec) at the exact same hour and minute.
     * - YEARLY: Repeats every year on the same date and time.
     */
    fun getNextTriggerTime(
        baseTimeMillis: Long,
        repeatType: String?,
        fromTimeMillis: Long = System.currentTimeMillis()
    ): Long {
        if (!isRecurring(repeatType)) {
            return baseTimeMillis
        }

        val baseCal = Calendar.getInstance().apply {
            timeInMillis = baseTimeMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val targetHour = baseCal.get(Calendar.HOUR_OF_DAY)
        val targetMinute = baseCal.get(Calendar.MINUTE)
        val targetDayOfMonth = baseCal.get(Calendar.DAY_OF_MONTH)

        val nextCal = Calendar.getInstance().apply {
            timeInMillis = baseTimeMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the base time is already in the future compared to fromTimeMillis, use it for the first occurrence!
        if (nextCal.timeInMillis > fromTimeMillis) {
            return nextCal.timeInMillis
        }

        when {
            repeatType.equals(RepeatType.DAILY.name, ignoreCase = true) -> {
                // If it is in the past, advance to next day at exact hour/minute
                nextCal.set(Calendar.HOUR_OF_DAY, targetHour)
                nextCal.set(Calendar.MINUTE, targetMinute)
                nextCal.set(Calendar.SECOND, 0)
                nextCal.set(Calendar.MILLISECOND, 0)
                while (nextCal.timeInMillis <= fromTimeMillis) {
                    nextCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            repeatType.equals(RepeatType.WEEKLY.name, ignoreCase = true) -> {
                // Repeat every 7 days (exact same day of week and time)
                nextCal.set(Calendar.HOUR_OF_DAY, targetHour)
                nextCal.set(Calendar.MINUTE, targetMinute)
                nextCal.set(Calendar.SECOND, 0)
                nextCal.set(Calendar.MILLISECOND, 0)
                while (nextCal.timeInMillis <= fromTimeMillis) {
                    nextCal.add(Calendar.DAY_OF_YEAR, 7)
                }
            }

            repeatType.equals(RepeatType.MONTHLY.name, ignoreCase = true) -> {
                // Repeat every month on the same day of the month at exact hour/minute
                // e.g. 7 September -> 7 October -> 7 November -> 7 December
                nextCal.set(Calendar.HOUR_OF_DAY, targetHour)
                nextCal.set(Calendar.MINUTE, targetMinute)
                nextCal.set(Calendar.SECOND, 0)
                nextCal.set(Calendar.MILLISECOND, 0)
                while (nextCal.timeInMillis <= fromTimeMillis) {
                    // Add 1 month
                    nextCal.add(Calendar.MONTH, 1)

                    // Maintain the target day of the month (or max days in month if 31st)
                    val maxDayInNewMonth = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val dayToSet = targetDayOfMonth.coerceAtMost(maxDayInNewMonth)
                    nextCal.set(Calendar.DAY_OF_MONTH, dayToSet)
                    nextCal.set(Calendar.HOUR_OF_DAY, targetHour)
                    nextCal.set(Calendar.MINUTE, targetMinute)
                    nextCal.set(Calendar.SECOND, 0)
                    nextCal.set(Calendar.MILLISECOND, 0)
                }
            }

            repeatType.equals(RepeatType.YEARLY.name, ignoreCase = true) -> {
                nextCal.set(Calendar.HOUR_OF_DAY, targetHour)
                nextCal.set(Calendar.MINUTE, targetMinute)
                nextCal.set(Calendar.SECOND, 0)
                nextCal.set(Calendar.MILLISECOND, 0)
                while (nextCal.timeInMillis <= fromTimeMillis) {
                    nextCal.add(Calendar.YEAR, 1)
                }
            }
        }

        return nextCal.timeInMillis
    }

    /**
     * Advance to the STRICT NEXT occurrence after an alarm has just fired.
     * Even if baseTimeMillis == fromTimeMillis, this forces advancing at least 1 cycle forward
     * (e.g. +1 day, +7 days, +1 month).
     */
    fun advanceToNextOccurrence(
        baseTimeMillis: Long,
        repeatType: String?,
        fromTimeMillis: Long = System.currentTimeMillis()
    ): Long {
        if (!isRecurring(repeatType)) {
            return baseTimeMillis
        }

        val baseCal = Calendar.getInstance().apply {
            timeInMillis = baseTimeMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val targetHour = baseCal.get(Calendar.HOUR_OF_DAY)
        val targetMinute = baseCal.get(Calendar.MINUTE)
        val targetDayOfMonth = baseCal.get(Calendar.DAY_OF_MONTH)

        val nextCal = Calendar.getInstance().apply {
            timeInMillis = baseTimeMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when {
            repeatType.equals(RepeatType.DAILY.name, ignoreCase = true) -> {
                nextCal.set(Calendar.HOUR_OF_DAY, targetHour)
                nextCal.set(Calendar.MINUTE, targetMinute)
                nextCal.set(Calendar.SECOND, 0)
                nextCal.set(Calendar.MILLISECOND, 0)
                // Always add at least 1 day
                nextCal.add(Calendar.DAY_OF_YEAR, 1)
                while (nextCal.timeInMillis <= fromTimeMillis) {
                    nextCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            repeatType.equals(RepeatType.WEEKLY.name, ignoreCase = true) -> {
                nextCal.set(Calendar.HOUR_OF_DAY, targetHour)
                nextCal.set(Calendar.MINUTE, targetMinute)
                nextCal.set(Calendar.SECOND, 0)
                nextCal.set(Calendar.MILLISECOND, 0)
                // Always add at least 7 days
                nextCal.add(Calendar.DAY_OF_YEAR, 7)
                while (nextCal.timeInMillis <= fromTimeMillis) {
                    nextCal.add(Calendar.DAY_OF_YEAR, 7)
                }
            }

            repeatType.equals(RepeatType.MONTHLY.name, ignoreCase = true) -> {
                nextCal.set(Calendar.HOUR_OF_DAY, targetHour)
                nextCal.set(Calendar.MINUTE, targetMinute)
                nextCal.set(Calendar.SECOND, 0)
                nextCal.set(Calendar.MILLISECOND, 0)
                // Always add at least 1 month
                nextCal.add(Calendar.MONTH, 1)
                val maxDay = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                nextCal.set(Calendar.DAY_OF_MONTH, targetDayOfMonth.coerceAtMost(maxDay))
                nextCal.set(Calendar.HOUR_OF_DAY, targetHour)
                nextCal.set(Calendar.MINUTE, targetMinute)
                nextCal.set(Calendar.SECOND, 0)
                nextCal.set(Calendar.MILLISECOND, 0)

                while (nextCal.timeInMillis <= fromTimeMillis) {
                    nextCal.add(Calendar.MONTH, 1)
                    val mDay = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    nextCal.set(Calendar.DAY_OF_MONTH, targetDayOfMonth.coerceAtMost(mDay))
                    nextCal.set(Calendar.HOUR_OF_DAY, targetHour)
                    nextCal.set(Calendar.MINUTE, targetMinute)
                    nextCal.set(Calendar.SECOND, 0)
                    nextCal.set(Calendar.MILLISECOND, 0)
                }
            }

            repeatType.equals(RepeatType.YEARLY.name, ignoreCase = true) -> {
                nextCal.set(Calendar.HOUR_OF_DAY, targetHour)
                nextCal.set(Calendar.MINUTE, targetMinute)
                nextCal.set(Calendar.SECOND, 0)
                nextCal.set(Calendar.MILLISECOND, 0)
                nextCal.add(Calendar.YEAR, 1)
                while (nextCal.timeInMillis <= fromTimeMillis) {
                    nextCal.add(Calendar.YEAR, 1)
                }
            }
        }

        return nextCal.timeInMillis
    }
}
