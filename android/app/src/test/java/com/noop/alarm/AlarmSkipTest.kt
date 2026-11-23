package com.noop.alarm

import com.noop.ui.nextSmartAlarmEpochSec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * "Skip the next alarm" (Not-Noop, ryanbr/noop#1611 item 1). A skip is one stored local date; every resolver
 * an alarm is armed from passes over that date and nothing else, and the alarm itself stays on.
 *
 * Covers both alarms' resolvers: the phone alarm's [SmartAlarmScheduler.nextDeadline] (keyed on the
 * deadline, the morning the user is woken) and the strap alarm's [nextSmartAlarmEpochSec], including the
 * "Buzz WHOOP 4" companion's offset keying for a phone window that crosses midnight.
 *
 * Fixtures use fixed dates so a weekday case stays that weekday whatever day the suite runs on.
 */
class AlarmSkipTest {

    /** 2026-08-24 is a Monday. */
    private fun mondayAt(hour: Int, minute: Int = 0): Calendar = Calendar.getInstance().apply {
        set(2026, Calendar.AUGUST, 24, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun phoneDeadline(
        now: Calendar,
        weekdays: Set<Int> = emptySet(),
        target: Int = 6 * 60 + 30,
        window: Int = 30,
        skipDay: String? = null,
    ): Calendar? = SmartAlarmScheduler.nextDeadline(now, weekdays, window, skipDay = skipDay) { target }

    private fun key(c: Calendar) = AlarmSkip.dayKey(c)

    @Test fun fixtureIsAMonday() {
        assertEquals(Calendar.MONDAY, mondayAt(5).get(Calendar.DAY_OF_WEEK))
    }

    // MARK: - The key

    @Test fun dayKeyIsZeroPaddedIsoDate() {
        assertEquals("2026-08-24", AlarmSkip.dayKey(mondayAt(5)))
        val jan = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 5, 23, 59, 0) }
        assertEquals("2026-01-05", AlarmSkip.dayKey(jan))
        // The epoch overload names the same local day.
        assertEquals("2026-01-05", AlarmSkip.dayKey(jan.timeInMillis))
    }

    @Test fun aSkipIsActiveOnlyWhileItNamesTheNextOccurrence() {
        val next = mondayAt(7).timeInMillis
        assertTrue(AlarmSkip.isActive("2026-08-24", next))
        assertFalse(AlarmSkip.isActive(null, next))
        assertFalse(AlarmSkip.isActive("2026-08-24", null))
        // A key left from a morning that has passed reads as no skip at all.
        assertFalse(AlarmSkip.isActive("2026-08-23", next))
    }

    // MARK: - Phone alarm

    @Test fun noSkipLeavesThePhoneAlarmUnchanged() {
        val now = mondayAt(5)
        assertEquals(
            SmartAlarmScheduler.nextDeadline(now, emptySet(), 30) { 6 * 60 + 30 }!!.timeInMillis,
            phoneDeadline(now, skipDay = null)!!.timeInMillis,
        )
    }

    @Test fun aDailyPhoneAlarmSkipsToTomorrowAtTheSameTime() {
        val d = phoneDeadline(mondayAt(5), skipDay = "2026-08-24")!!
        assertEquals("2026-08-25", key(d))
        assertEquals(7, d.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, d.get(Calendar.MINUTE))
    }

    @Test fun aWeekdayPhoneAlarmSkipsToItsNextChosenDay() {
        val d = phoneDeadline(mondayAt(5), weekdays = setOf(Calendar.MONDAY, Calendar.FRIDAY), skipDay = "2026-08-24")!!
        assertEquals("2026-08-28", key(d))
    }

    /** Once a week, already past today: the next is 7 days out and skipping it needs the 14-day search. */
    @Test fun aOnceAWeekPhoneAlarmSkipsAFullWeekNotToNothing() {
        val now = mondayAt(8)
        val unskipped = phoneDeadline(now, weekdays = setOf(Calendar.MONDAY))!!
        assertEquals("2026-08-31", key(unskipped))
        val skipped = phoneDeadline(now, weekdays = setOf(Calendar.MONDAY), skipDay = "2026-08-31")
        assertNotNull(skipped)
        assertEquals("2026-09-07", key(skipped!!))
    }

    @Test fun aStaleSkipChangesNothing() {
        val now = mondayAt(5)
        assertEquals(
            phoneDeadline(now)!!.timeInMillis,
            phoneDeadline(now, skipDay = "2026-08-23")!!.timeInMillis,
        )
    }

    @Test fun theWindowFigureFollowsTheSkip() {
        // Tuesday has its own earlier time; skipping Monday must show Tuesday's start, not Monday's.
        val start = SmartAlarmScheduler.nextWindowStartMinutes(
            now = mondayAt(5),
            weekdays = emptySet(),
            windowMinutes = 30,
            defaultTarget = 6 * 60 + 30,
            skipDay = "2026-08-24",
        ) { if (it == Calendar.TUESDAY) 5 * 60 else 6 * 60 + 30 }
        assertEquals(5 * 60, start)
    }

    // MARK: - Strap alarm

    @Test fun noSkipLeavesTheStrapAlarmUnchanged() {
        val now = mondayAt(5).timeInMillis
        assertEquals(
            nextSmartAlarmEpochSec(6 * 60 + 30, emptySet(), nowMs = now),
            nextSmartAlarmEpochSec(6 * 60 + 30, emptySet(), nowMs = now, skipDay = null),
        )
    }

    @Test fun aDailyStrapAlarmSkipsToTomorrow() {
        val sec = nextSmartAlarmEpochSec(6 * 60 + 30, emptySet(), nowMs = mondayAt(5).timeInMillis, skipDay = "2026-08-24")!!
        val c = Calendar.getInstance().apply { timeInMillis = sec * 1000 }
        assertEquals("2026-08-25", key(c))
        assertEquals(6, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, c.get(Calendar.MINUTE))
    }

    @Test fun aOnceAWeekStrapAlarmSkipsAFullWeekNotToNothing() {
        val now = mondayAt(8).timeInMillis
        val weekly = setOf(Calendar.MONDAY)
        val unskipped = nextSmartAlarmEpochSec(6 * 60 + 30, weekly, nowMs = now)!!
        assertEquals("2026-08-31", AlarmSkip.dayKey(unskipped * 1000))
        val skipped = nextSmartAlarmEpochSec(6 * 60 + 30, weekly, nowMs = now, skipDay = "2026-08-31")
        assertNotNull(skipped)
        assertEquals("2026-09-07", AlarmSkip.dayKey(skipped!! * 1000))
    }

    /**
     * The companion buzzes at the phone alarm's earliest time (23:50) while the phone alarm keys its days
     * and its skip on the deadline (00:20 the next date). Keyed with the window offset, the companion skips
     * the same night the phone alarm does, and both resume on the same night.
     */
    @Test fun theCompanionSkipsTheSameNightAsAPhoneWindowThatCrossesMidnight() {
        val now = mondayAt(20)
        val target = 23 * 60 + 50
        val window = 30
        val phone = phoneDeadline(now, target = target, window = window)!!
        assertEquals("2026-08-25", key(phone))
        val skip = key(phone)

        val phoneAfter = phoneDeadline(now, target = target, window = window, skipDay = skip)!!
        val companionAfter = nextSmartAlarmEpochSec(
            target, emptySet(), nowMs = now.timeInMillis, skipDay = skip, skipDayOffsetMinutes = window,
        )!!
        // Without the offset the companion would key Monday night on Monday and still buzz.
        val companionUnkeyed = nextSmartAlarmEpochSec(target, emptySet(), nowMs = now.timeInMillis, skipDay = skip)!!
        assertEquals("2026-08-24", AlarmSkip.dayKey(companionUnkeyed * 1000))

        assertEquals("2026-08-25", AlarmSkip.dayKey(companionAfter * 1000))
        assertEquals(phoneAfter.timeInMillis, companionAfter * 1000 + window * 60_000L)
    }
}
