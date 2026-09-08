package com.example

import com.example.data.model.RepeatType
import com.example.service.ReminderScheduleHelper
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun testDailyRecurrenceCalculation() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 8, 6, 40, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerTime = baseCal.timeInMillis

        // When alarm fires on 8 Sep at 06:40:01 AM
        val nextOccurrence = ReminderScheduleHelper.advanceToNextOccurrence(
            baseTimeMillis = triggerTime,
            repeatType = RepeatType.DAILY.name,
            fromTimeMillis = triggerTime + 1000L
        )

        val nextCal = Calendar.getInstance().apply { timeInMillis = nextOccurrence }
        assertEquals(2026, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, nextCal.get(Calendar.MONTH))
        assertEquals(9, nextCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(6, nextCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(40, nextCal.get(Calendar.MINUTE))
        assertEquals(0, nextCal.get(Calendar.SECOND))

        // When it fires on 9 Sep at 06:40:01 AM -> should advance to 10 Sep
        val dayAfterOccurrence = ReminderScheduleHelper.advanceToNextOccurrence(
            baseTimeMillis = nextOccurrence,
            repeatType = RepeatType.DAILY.name,
            fromTimeMillis = nextOccurrence + 1000L
        )
        val dayAfterCal = Calendar.getInstance().apply { timeInMillis = dayAfterOccurrence }
        assertEquals(10, dayAfterCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(6, dayAfterCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(40, dayAfterCal.get(Calendar.MINUTE))
    }

    @Test
    fun testWeeklyRecurrenceCalculation() {
        // Monday 8 September 2026 at 10:00 AM
        val baseCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 8)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetDayOfWeek = baseCal.get(Calendar.DAY_OF_WEEK)

        val nextOccurrence = ReminderScheduleHelper.advanceToNextOccurrence(
            baseTimeMillis = baseCal.timeInMillis,
            repeatType = RepeatType.WEEKLY.name,
            fromTimeMillis = baseCal.timeInMillis + 5000L
        )

        val nextCal = Calendar.getInstance().apply { timeInMillis = nextOccurrence }
        assertEquals(targetDayOfWeek, nextCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(10, nextCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, nextCal.get(Calendar.MINUTE))
        assertTrue(nextOccurrence > baseCal.timeInMillis)
        assertEquals(7 * 24 * 3600 * 1000L, nextOccurrence - baseCal.timeInMillis)
    }

    @Test
    fun testMonthlyRecurrenceCalculation() {
        // 8 September at 06:40 AM -> next must be 8 October at 06:40 AM -> next must be 8 November at 06:40 AM
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 8, 6, 40, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val baseTime = baseCal.timeInMillis

        // After 8 Sep fires:
        val octTime = ReminderScheduleHelper.advanceToNextOccurrence(
            baseTimeMillis = baseTime,
            repeatType = RepeatType.MONTHLY.name,
            fromTimeMillis = baseTime + 1000L
        )
        val octCal = Calendar.getInstance().apply { timeInMillis = octTime }
        assertEquals(8, octCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.OCTOBER, octCal.get(Calendar.MONTH))
        assertEquals(6, octCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(40, octCal.get(Calendar.MINUTE))

        // After 8 Oct fires:
        val novTime = ReminderScheduleHelper.advanceToNextOccurrence(
            baseTimeMillis = octTime,
            repeatType = RepeatType.MONTHLY.name,
            fromTimeMillis = octTime + 1000L
        )
        val novCal = Calendar.getInstance().apply { timeInMillis = novTime }
        assertEquals(8, novCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.NOVEMBER, novCal.get(Calendar.MONTH))
        assertEquals(6, novCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(40, novCal.get(Calendar.MINUTE))
    }

    @Test
    fun testGetNextTriggerTimePreservesFutureOccurrence() {
        val futureCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val futureTime = futureCal.timeInMillis

        val result = ReminderScheduleHelper.getNextTriggerTime(
            baseTimeMillis = futureTime,
            repeatType = RepeatType.DAILY.name,
            fromTimeMillis = System.currentTimeMillis()
        )
        // Must preserve future time without skipping days
        assertEquals(futureTime, result)
    }
}
