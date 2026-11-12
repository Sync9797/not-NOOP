package com.noop.ui

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MaterialAccentSchemeTest {
    private val originalTokens = Palette.active
    private val originalAccent = AccentPrefs.color
    private val originalHex = AccentPrefs.customHex
    private val originalDynamic = MaterialYouAccent.primary

    @After
    fun restorePreferences() {
        val context = RuntimeEnvironment.getApplication()
        AccentPrefs.setColor(context, originalAccent)
        AccentPrefs.setCustomHex(context, originalHex)
        Palette.active = originalTokens
        MaterialYouAccent.primary = originalDynamic
    }

    @Test
    fun `Material controls follow every selected accent in both appearances`() {
        val context = RuntimeEnvironment.getApplication()
        AccentPrefs.setCustomHex(context, "#A844CC")
        for (dark in listOf(false, true)) {
            val tokens = if (dark) DarkTokens else LightTokens
            Palette.active = tokens
            for (accent in AccentColor.entries) {
                AccentPrefs.setColor(context, accent)
                val scheme = noopColorScheme(tokens, dark)
                assertEquals("primary: $accent, dark=$dark", Palette.accent, scheme.primary)
                assertEquals(Palette.accentMuted, scheme.primaryContainer)
                assertEquals(if (dark) Palette.accentHover else Palette.accent, scheme.onPrimaryContainer)
                // Picking a chrome accent must not recolor metric or warning semantics.
                assertEquals(tokens.metricPurple, scheme.secondary)
                assertEquals(tokens.statusCritical, scheme.error)
            }
        }
    }

    @Test
    fun `Material You accent follows the wallpaper primary when dynamic colour resolved`() {
        val context = RuntimeEnvironment.getApplication()
        AccentPrefs.setColor(context, AccentColor.MATERIAL_YOU)
        val wallpaper = androidx.compose.ui.graphics.Color(0xFF6750A4)
        MaterialYouAccent.primary = wallpaper
        for (dark in listOf(false, true)) {
            val tokens = if (dark) DarkTokens else LightTokens
            Palette.active = tokens
            assertEquals(wallpaper, Palette.accent)
            assertEquals(wallpaper.copy(alpha = 0.18f), Palette.accentMuted)
            assertEquals(AccentColor.lighten(wallpaper), Palette.accentHover)
            assertEquals(wallpaper, Palette.focusRing)
            val scheme = noopColorScheme(tokens, dark)
            assertEquals(wallpaper, scheme.primary)
            // Chrome only: data and warning colours are untouched.
            assertEquals(tokens.metricPurple, scheme.secondary)
            assertEquals(tokens.statusCritical, scheme.error)
        }
    }

    @Test
    fun `Material You accent falls back to mint without dynamic colour`() {
        val context = RuntimeEnvironment.getApplication()
        AccentPrefs.setColor(context, AccentColor.MATERIAL_YOU)
        MaterialYouAccent.primary = null
        for (tokens in listOf(LightTokens, DarkTokens)) {
            Palette.active = tokens
            assertEquals(tokens.accent, Palette.accent)
            assertEquals(tokens.accentHover, Palette.accentHover)
            assertEquals(tokens.accentMuted, Palette.accentMuted)
            assertEquals(tokens.focusRing, Palette.focusRing)
        }
    }

    @Test
    fun `Material You choice survives a prefs round trip`() {
        assertEquals(AccentColor.MATERIAL_YOU, AccentColor.fromStorage("materialYou"))
    }
}
