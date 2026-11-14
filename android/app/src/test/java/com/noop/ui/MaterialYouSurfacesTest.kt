package com.noop.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The optional full Material You theme changes chrome surfaces only, never text or data colours. */
class MaterialYouSurfacesTest {

    private val light = lightColorScheme(
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF6F2FA),
        surfaceContainer = Color(0xFFF0ECF4), surfaceContainerHigh = Color(0xFFEAE6EE),
        outline = Color(0xFF79747E), outlineVariant = Color(0xFFCAC4D0),
    )
    private val dark = darkColorScheme(
        surfaceContainerLowest = Color(0xFF0F0D13), surfaceContainerLow = Color(0xFF1D1B20),
        surfaceContainer = Color(0xFF211F26), surfaceContainerHigh = Color(0xFF2B2930),
        outline = Color(0xFF938F99), outlineVariant = Color(0xFF49454F),
    )

    @Test fun surfacesComeFromTheScheme() {
        val l = materialYouSurfaces(LightTokens, light)
        assertEquals(light.surfaceContainer, l.surfaceBase)
        assertEquals(light.surfaceContainerLowest, l.surfaceRaised)
        assertEquals(light.outlineVariant, l.hairline)
        val d = materialYouSurfaces(DarkTokens, dark)
        assertEquals(dark.surfaceContainerLowest, d.surfaceBase)
        assertEquals(dark.surfaceContainerHigh, d.surfaceRaised)
    }

    @Test fun textAndDataColoursAreUntouched() {
        for ((base, scheme) in listOf(LightTokens to light, DarkTokens to dark)) {
            val derived = materialYouSurfaces(base, scheme)
            // Everything but the surface roles must equal the base set exactly.
            val surfaceRolesReset = derived.copy(
                surfaceBase = base.surfaceBase, surfaceRaised = base.surfaceRaised,
                surfaceOverlay = base.surfaceOverlay, surfaceInset = base.surfaceInset,
                hairline = base.hairline, hairlineStrong = base.hairlineStrong,
                cardFillTop = base.cardFillTop, cardFillBottom = base.cardFillBottom,
                heroFill = base.heroFill,
            )
            assertEquals(base, surfaceRolesReset)
            assertNotEquals(base, derived)
        }
    }

    @Test fun lightAndDarkStayWhatTheyWere() {
        assertTrue(materialYouSurfaces(LightTokens, light).light)
        assertFalse(materialYouSurfaces(DarkTokens, dark).light)
    }

    @Test fun cardsStayLighterThanThePageInLightAndRaisedInDark() {
        val l = materialYouSurfaces(LightTokens, light)
        assertTrue(luma(l.surfaceRaised) > luma(l.surfaceBase))
        val d = materialYouSurfaces(DarkTokens, dark)
        assertTrue(luma(d.surfaceRaised) > luma(d.surfaceBase))
    }

    @Test fun sameSchemeGivesAnEqualSet() {
        // NoopTheme relies on value equality to keep Palette.active's identity across theme passes.
        assertEquals(materialYouSurfaces(DarkTokens, dark), materialYouSurfaces(DarkTokens, dark))
    }

    private fun luma(c: Color) = 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue
}
