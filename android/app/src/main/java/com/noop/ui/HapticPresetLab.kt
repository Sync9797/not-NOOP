package com.noop.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noop.R

/**
 * Not-Noop haptic preset experiment, hosted in the Test Centre (full app). Every NOOP buzz on a WHOOP 4.0
 * uses preset 2, so every buzz feels the same; this lets a wearer try the other preset ids one at a time,
 * record what each felt like, and share the results in the strap log. See WhoopBleClient.tryHapticPreset.
 */
object HapticPresetLab {
    /** The ids offered. The command takes a byte; 0-15 covers every small id worth a first look. */
    val PRESET_IDS: List<Int> = (0..15).toList()

    /** What a wearer can report, as (log key, label resource). The log key stays English for triage. */
    val FEELS: List<Pair<String, Int>> = listOf(
        "nothing" to R.string.haptic_lab_feel_nothing,
        "one short" to R.string.haptic_lab_feel_one,
        "two" to R.string.haptic_lab_feel_two,
        "three" to R.string.haptic_lab_feel_three,
        "long" to R.string.haptic_lab_feel_long,
        "other pattern" to R.string.haptic_lab_feel_other,
    )

    private const val PREFS = "noop_prefs"
    private const val KEY_PREFIX = "noop.hapticPresetFeel."

    fun savedFeel(context: Context, id: Int): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PREFIX + id, null)

    fun saveFeel(context: Context, id: Int, feel: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PREFIX + id, feel).apply()
    }

    /** "3: two, 5: nothing" for every recorded id, in id order; empty when nothing is recorded. */
    fun summary(context: Context): String =
        PRESET_IDS.mapNotNull { id -> savedFeel(context, id)?.let { "$id: $it" } }.joinToString(", ")
}

/** The Test Centre card body; the screen wraps it in its own card so it matches its siblings. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HapticPresetLabContent(vm: AppViewModel) {
    val context = LocalContext.current
    var lastTried by remember { mutableStateOf<Int?>(null) }
    var summary by remember { mutableStateOf(HapticPresetLab.summary(context)) }
    val notConnected = stringResource(R.string.haptic_lab_needs_whoop4)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NoopButton(
                text = stringResource(R.string.haptic_lab_read_list),
                kind = NoopButtonKind.Secondary,
                onClick = {
                    if (!vm.ble.readHapticPatterns()) Toast.makeText(context, notConnected, Toast.LENGTH_LONG).show()
                },
            )
            NoopButton(
                text = stringResource(R.string.haptic_lab_stop),
                kind = NoopButtonKind.Secondary,
                onClick = { vm.ble.stopHaptics() },
            )
        }
        Text(stringResource(R.string.haptic_lab_try_heading), style = NoopType.subhead, color = Palette.textPrimary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HapticPresetLab.PRESET_IDS.forEach { id ->
                NoopButton(
                    text = id.toString(),
                    kind = if (id == lastTried) NoopButtonKind.Primary else NoopButtonKind.Secondary,
                    onClick = {
                        if (vm.ble.tryHapticPreset(id)) lastTried = id
                        else Toast.makeText(context, notConnected, Toast.LENGTH_LONG).show()
                    },
                )
            }
        }
        lastTried?.let { id ->
            Text(
                stringResource(R.string.haptic_lab_what_felt, id),
                style = NoopType.subhead,
                color = Palette.textPrimary,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HapticPresetLab.FEELS.forEach { (key, label) ->
                    NoopButton(
                        text = stringResource(label),
                        kind = if (HapticPresetLab.savedFeel(context, id) == key) NoopButtonKind.Primary
                        else NoopButtonKind.Secondary,
                        onClick = {
                            HapticPresetLab.saveFeel(context, id, key)
                            vm.ble.noteHapticPresetFeel(id, key)
                            summary = HapticPresetLab.summary(context)
                        },
                    )
                }
            }
        }
        if (summary.isNotEmpty()) {
            Text(
                stringResource(R.string.haptic_lab_results, summary),
                style = NoopType.footnote,
                color = Palette.textSecondary,
            )
        }
        Text(stringResource(R.string.haptic_lab_share_hint), style = NoopType.footnote, color = Palette.textTertiary)
    }
}

/** Icon for the Test Centre card. */
internal val hapticPresetLabIcon = Icons.Filled.Vibration
