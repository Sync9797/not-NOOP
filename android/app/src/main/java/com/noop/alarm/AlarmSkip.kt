package com.noop.alarm

import java.util.Calendar
import java.util.Locale

/**
 * "Skip the next alarm" without switching the alarm off (Not-Noop; ryanbr/noop#1611 item 1).
 *
 * A skip is stored as the LOCAL DATE of the occurrence being skipped ("2026-11-25"), not a flag or a
 * timestamp, as the issue proposes:
 *  - it expires by itself: once that date has passed no occurrence can match it, and the alarm resumes,
 *    which is the "remember to switch it back on" step the request is about;
 *  - `enabled` is never touched, so the alarm reads "on, skipping Tue" rather than "off";
 *  - it survives the smart window moving the fire time, and DST, because it names a day, not an instant.
 *
 * Every resolver that answers "when does this alarm fire next" takes the same skip day, so the armed
 * alarm, the countdown and the card can never disagree about whether the skipped morning still rings.
 * A skip is cleared whenever that alarm's settings change, so it always names the occurrence that was
 * next when it was set: between the skip and that day nothing earlier can appear without an edit.
 */
object AlarmSkip {

    /** The local `yyyy-MM-dd` key of [cal]'s calendar day. Built from the calendar's own fields, so it is
     *  the same day the resolvers test, whatever the time of day. */
    fun dayKey(cal: Calendar): String = String.format(
        Locale.US,
        "%04d-%02d-%02d",
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
    )

    /** [dayKey] for an instant, in the device's zone. */
    fun dayKey(epochMs: Long): String = dayKey(Calendar.getInstance().apply { timeInMillis = epochMs })

    /**
     * Whether [skipDay] is currently in force: it must name exactly the occurrence that would fire next if
     * nothing were skipped ([nextUnskippedMs]). A stale key (a past date) or no next occurrence is not.
     */
    fun isActive(skipDay: String?, nextUnskippedMs: Long?): Boolean =
        skipDay != null && nextUnskippedMs != null && dayKey(nextUnskippedMs) == skipDay
}
