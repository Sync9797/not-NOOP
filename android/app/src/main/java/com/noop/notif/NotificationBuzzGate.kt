package com.noop.notif

/**
 * One wrist buzz per notification (Not-Noop).
 *
 * Android calls onNotificationPosted for every POST and every UPDATE of a notification, and messaging
 * apps routinely post a message and then update the same notification once or twice within a second
 * (heads-up, grouping, read state). The listener used to buzz on each call, so one message sent two or
 * three RUN_HAPTICS_PATTERN commands a second apart. A strap log showed exactly that: pairs and triples of
 * "Buzz: patternId=2" at 12:33:49, 12:34:45-47, 12:35:35-37 and more. On a WHOOP 4.0 each one is a full
 * preset-2 pattern (two pairs of short buzzes), so they overlapped on the wrist into a jumbled, "triple"
 * feel no matter which pattern the user picked.
 *
 * The rules, in order:
 *  1. An update the app itself marked as quiet ([onlyAlertOnce], FLAG_ONLY_ALERT_ONCE) never buzzes
 *     again for a key that already buzzed, the same way the phone itself stays silent for it.
 *  2. The same key never buzzes twice within [SAME_KEY_WINDOW_MS]: an update is not a new message.
 *  3. Across ALL apps, no buzz within [BURST_WINDOW_MS] of the last one, so a burst (a message plus its
 *     group siblings, two apps at once) is one tap, and a new pattern never starts on top of one still
 *     playing on the strap.
 *
 * Pure apart from its own small memory; time is passed in, so the rules are unit-tested.
 */
internal class NotificationBuzzGate(
    private val sameKeyWindowMs: Long = SAME_KEY_WINDOW_MS,
    private val burstWindowMs: Long = BURST_WINDOW_MS,
) {
    enum class Decision { BUZZ, SKIP_QUIET_UPDATE, SKIP_SAME_NOTIFICATION, SKIP_BURST }

    private val lastByKey = LinkedHashMap<String, Long>()
    private var lastAnyMs: Long? = null

    @Synchronized
    fun decide(key: String, onlyAlertOnce: Boolean, nowMs: Long): Decision {
        val lastForKey = lastByKey[key]
        val decision = when {
            lastForKey != null && onlyAlertOnce -> Decision.SKIP_QUIET_UPDATE
            lastForKey != null && nowMs - lastForKey in 0 until sameKeyWindowMs -> Decision.SKIP_SAME_NOTIFICATION
            lastAnyMs?.let { nowMs - it in 0 until burstWindowMs } == true -> Decision.SKIP_BURST
            else -> Decision.BUZZ
        }
        if (decision == Decision.BUZZ) {
            lastByKey[key] = nowMs
            lastAnyMs = nowMs
            // Bounded memory: forget the oldest keys; an old key re-posting long after is a new alert anyway.
            while (lastByKey.size > MAX_KEYS) lastByKey.remove(lastByKey.keys.first())
        }
        return decision
    }

    /** The notification is gone: a later post under the same key is a genuinely new alert. */
    @Synchronized
    fun forget(key: String) {
        lastByKey.remove(key)
    }

    companion object {
        /** An update to the same notification within this window is not a new message. */
        const val SAME_KEY_WINDOW_MS = 30_000L

        /** Minimum gap between any two notification buzzes: long enough for one preset-2 pattern to finish. */
        const val BURST_WINDOW_MS = 4_000L

        private const val MAX_KEYS = 64
    }
}
