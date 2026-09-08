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
     * - If baseTimeMillis is already strictly in the future (> fromTimeMillis), retain it.
     * - If baseTimeMillis is in the past or now (<= fromTimeMillis), advance to the first upcoming occurrence.
     */
    fun getNextTriggerTime(
        baseTimeMillis: Long,
        repeatType: String?,
        fromTimeMillis: Long = System.currentTimeMillis()
    ): Long {
        if (!isRecurring(repeatType)) {
            return baseTimeMillis
        }

        // If the scheduled time is already strictly in the future, retain it (do not skip cycles)
        if (baseTimeMillis > fromTimeMillis) {
            return baseTimeMillis
        }

        return advanceToNextOccurrence(baseTimeMillis, repeatType, fromTimeMillis)
    }

    /**
     * Advances to the STRICT NEXT occurrence strictly after fromTimeMillis.
     * Guaranteed to return a timestamp strictly in the future (> fromTimeMillis).
     *
     * - DAILY: Repeats every single day at the fixed hour and minute.
     * - WEEKLY: Repeats every week on the fixed day of the week (e.g. every Monday) at the fixed hour and minute.
     * - MONTHLY: Repeats every month on the fixed day of the month (e.g. 8 Sep -> 8 Oct -> 8 Nov -> 8 Dec) at the fixed hour and minute.
     * - YEARLY: Repeats every year on the fixed date at the fixed hour and minute.
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
        val targetDayOfWeek = baseCal.get(Calendar.DAY_OF_WEEK)
        val targetDayOfMonth = baseCal.get(Calendar.DAY_OF_MONTH)
        val targetMonth = baseCal.get(Calendar.MONTH)

        when {
            repeatType.equals(RepeatType.DAILY.name, ignoreCase = true) -> {
                // Start candidate on the same day as fromTimeMillis
                val cal = Calendar.getInstance().apply {
                    timeInMillis = fromTimeMillis
                    set(Calendar.HOUR_OF_DAY, targetHour)
                    set(Calendar.MINUTE, targetMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // If today's target time is already in the past or right now, advance by 1 day to tomorrow
                if (cal.timeInMillis <= fromTimeMillis) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                return cal.timeInMillis
            }

            repeatType.equals(RepeatType.WEEKLY.name, ignoreCase = true) -> {
                // Must repeat every week on the exact same day of the week (e.g. Monday) at targetHour:targetMinute
                val cal = Calendar.getInstance().apply {
                    timeInMillis = fromTimeMillis
                    set(Calendar.DAY_OF_WEEK, targetDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, targetHour)
                    set(Calendar.MINUTE, targetMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                while (cal.timeInMillis <= fromTimeMillis) {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                    cal.set(Calendar.DAY_OF_WEEK, targetDayOfWeek)
                    cal.set(Calendar.HOUR_OF_DAY, targetHour)
                    cal.set(Calendar.MINUTE, targetMinute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }

            repeatType.equals(RepeatType.MONTHLY.name, ignoreCase = true) -> {
                // Must repeat every month on the exact same day of the month (e.g. 8th Sep -> 8th Oct -> 8th Nov)
                val cal = Calendar.getInstance().apply {
                    timeInMillis = fromTimeMillis
                    val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, targetDayOfMonth.coerceAtMost(maxDay))
                    set(Calendar.HOUR_OF_DAY, targetHour)
                    set(Calendar.MINUTE, targetMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                while (cal.timeInMillis <= fromTimeMillis) {
                    cal.add(Calendar.MONTH, 1)
                    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, targetDayOfMonth.coerceAtMost(maxDay))
                    cal.set(Calendar.HOUR_OF_DAY, targetHour)
                    cal.set(Calendar.MINUTE, targetMinute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }

            repeatType.equals(RepeatType.YEARLY.name, ignoreCase = true) -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = fromTimeMillis
                    set(Calendar.MONTH, targetMonth)
                    val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, targetDayOfMonth.coerceAtMost(maxDay))
                    set(Calendar.HOUR_OF_DAY, targetHour)
                    set(Calendar.MINUTE, targetMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                while (cal.timeInMillis <= fromTimeMillis) {
                    cal.add(Calendar.YEAR, 1)
                    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, targetDayOfMonth.coerceAtMost(maxDay))
                    cal.set(Calendar.HOUR_OF_DAY, targetHour)
                    cal.set(Calendar.MINUTE, targetMinute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }

            else -> return baseTimeMillis
        }
    }
}
