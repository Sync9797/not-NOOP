package com.noop.ui

import com.noop.protocol.LiveSessionHaptics

/**
 * How multi-tap cues play on a WHOOP 4.0 (Not-Noop).
 *
 * The cue code was written for a strap with real short pulses: a Live Session "push" is two light taps
 * 650 ms apart, an "ease off" three heavy ones about a second apart, and Intervals ticks 3-2-1 once a
 * second into each phase change. On a WHOOP 4.0 every RUN_HAPTICS_PATTERN command plays a whole preset
 * (preset 2 is two pairs of short buzzes and lasts over a second), so those commands landed on top of one
 * another and blurred together (docs/PROTOCOL_ALARMS.md, "Why WHOOP 4 cues felt jumbled").
 *
 * On a 4.0, each cue is therefore ONE command, picked from the three presets mapped with the Test Centre
 * preset experiment (firmware 41.17.6.0): preset 0 is one long buzz, preset 2 two pairs of short buzzes.
 * A 5/MG keeps the original pulse walks, which its maverick buzz renders as distinct short pulses.
 */
object Whoop4Cues {
    /** WHOOP 4.0 preset ids (see HapticPresetLab). */
    const val PRESET_LONG = 0
    const val PRESET_SHORT_PAIRS = 2

    /** A single strap command: preset id and loop count. */
    data class Command(val presetId: Int, val loops: Int)

    /**
     * The one command a Live Session cue becomes on a WHOOP 4.0. "Give a bit more" stays the lighter,
     * shorter feel (one round of short pairs); "ease off" becomes the one long buzz, so the two cues
     * cannot be mistaken for each other.
     */
    fun liveSession(signal: LiveSessionHaptics.Signal): Command = when (signal) {
        LiveSessionHaptics.Signal.PUSH -> Command(PRESET_SHORT_PAIRS, 1)
        LiveSessionHaptics.Signal.EASE_OFF -> Command(PRESET_LONG, 1)
    }

    /**
     * Whether Intervals plays its once-a-second 3-2-1 tick. Not on a WHOOP 4.0: a one-second tick cannot
     * finish before the next, and the last one fires in the same second as the phase-change buzz. The
     * phase-change cues (into rest, into work, done) still play, one command each.
     */
    fun intervalsCountdownTick(remainingSec: Int, oneCommandPerCue: Boolean): Boolean =
        !oneCommandPerCue && remainingSec in 1..3
}
