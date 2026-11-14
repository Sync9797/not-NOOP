package com.noop.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The large-screen / foldable shell decisions, as pure functions. Phones must land on exactly the layout
 * they had before: the bottom bar and the 28dp screen inset.
 */
class AdaptiveLayoutTest {

    @Test fun widthClassesUseMaterialBreakpoints() {
        assertEquals(WindowWidthClass.COMPACT, WindowWidthClass.forWidth(360.dp))
        assertEquals(WindowWidthClass.COMPACT, WindowWidthClass.forWidth(599.dp))
        assertEquals(WindowWidthClass.MEDIUM, WindowWidthClass.forWidth(600.dp))
        assertEquals(WindowWidthClass.MEDIUM, WindowWidthClass.forWidth(839.dp))
        assertEquals(WindowWidthClass.EXPANDED, WindowWidthClass.forWidth(840.dp))
    }

    @Test fun phonesKeepTheBottomBarAndWideWindowsGetTheRail() {
        assertEquals(ShellNavigation.BOTTOM_BAR, shellNavigationFor(WindowWidthClass.COMPACT, null))
        assertEquals(ShellNavigation.RAIL, shellNavigationFor(WindowWidthClass.MEDIUM, null))
        assertEquals(ShellNavigation.RAIL, shellNavigationFor(WindowWidthClass.EXPANDED, null))
    }

    @Test fun aTabletopFoldWinsOverWidth() {
        val hinge = TabletopHinge(topPx = 1000, bottomPx = 1000)
        for (w in WindowWidthClass.entries) {
            assertEquals(ShellNavigation.TABLETOP, shellNavigationFor(w, hinge))
        }
    }

    @Test fun onlyAHalfOpenedHorizontalFoldIsTabletop() {
        fun hinge(halfOpened: Boolean, horizontal: Boolean) = tabletopHinge(
            halfOpened, horizontal, foldTopInWindowPx = 1100, foldBottomInWindowPx = 1100,
            shellTopInWindowPx = 0, shellHeightPx = 2200, minHalfPx = 400,
        )
        assertEquals(TabletopHinge(1100, 1100), hinge(halfOpened = true, horizontal = true))
        assertNull("flat is not tabletop", hinge(halfOpened = false, horizontal = true))
        assertNull("book posture is not tabletop", hinge(halfOpened = true, horizontal = false))
    }

    @Test fun theHingeIsShiftedIntoShellCoordinates() {
        val h = tabletopHinge(true, true, foldTopInWindowPx = 1100, foldBottomInWindowPx = 1130,
            shellTopInWindowPx = 100, shellHeightPx = 2100, minHalfPx = 400)
        assertEquals(TabletopHinge(topPx = 1000, bottomPx = 1030), h)
    }

    @Test fun aFoldThatLeavesASliverIsIgnored() {
        // Split-screen window sitting almost entirely below the hinge: the top half would be 100px.
        assertNull(tabletopHinge(true, true, 1100, 1100, shellTopInWindowPx = 1000, shellHeightPx = 1200, minHalfPx = 400))
        // ...or almost entirely above it.
        assertNull(tabletopHinge(true, true, 1100, 1100, shellTopInWindowPx = 0, shellHeightPx = 1200, minHalfPx = 400))
        // Not yet measured.
        assertNull(tabletopHinge(true, true, 1100, 1100, shellTopInWindowPx = 0, shellHeightPx = 0, minHalfPx = 400))
    }

    @Test fun aBookFoldWinsOverWidthButNotOverTabletop() {
        val book = BookHinge(startWidthPx = 900, hingeWidthPx = 0)
        for (w in WindowWidthClass.entries) {
            assertEquals(ShellNavigation.BOOK, shellNavigationFor(w, null, book))
        }
        assertEquals(ShellNavigation.TABLETOP, shellNavigationFor(WindowWidthClass.EXPANDED, TabletopHinge(1000, 1000), book))
    }

    @Test fun onlyAHalfOpenedVerticalFoldIsBook() {
        fun hinge(halfOpened: Boolean, vertical: Boolean) = bookHinge(
            halfOpened, vertical, foldLeftInWindowPx = 900, foldRightInWindowPx = 900,
            shellLeftInWindowPx = 0, shellWidthPx = 1800, minHalfPx = 400,
        )
        assertEquals(BookHinge(startWidthPx = 900, hingeWidthPx = 0), hinge(halfOpened = true, vertical = true))
        assertNull("flat is not book", hinge(halfOpened = false, vertical = true))
        assertNull("tabletop is not book", hinge(halfOpened = true, vertical = false))
    }

    @Test fun theBookHingeIsInShellCoordinatesAndMirrorsForRightToLeft() {
        // A 30px hinge from x=900 to x=930 in a shell starting at x=100, 1700px wide.
        assertEquals(BookHinge(startWidthPx = 800, hingeWidthPx = 30),
            bookHinge(true, true, 900, 930, shellLeftInWindowPx = 100, shellWidthPx = 1700, minHalfPx = 400))
        // Right-to-left: the start half is the right one, 1700 - 830 = 870px wide.
        assertEquals(BookHinge(startWidthPx = 870, hingeWidthPx = 30),
            bookHinge(true, true, 900, 930, shellLeftInWindowPx = 100, shellWidthPx = 1700, minHalfPx = 400, rightToLeft = true))
    }

    @Test fun aBookFoldThatLeavesASliverIsIgnored() {
        assertNull(bookHinge(true, true, 900, 900, shellLeftInWindowPx = 800, shellWidthPx = 1000, minHalfPx = 400))
        assertNull(bookHinge(true, true, 900, 900, shellLeftInWindowPx = 0, shellWidthPx = 1000, minHalfPx = 400))
        assertNull(bookHinge(true, true, 900, 900, shellLeftInWindowPx = 0, shellWidthPx = 0, minHalfPx = 400))
    }

    @Test fun phonesKeepThe28dpInset() {
        for (w in listOf(320.dp, 360.dp, 412.dp, 480.dp, 776.dp)) {
            assertEquals("width $w", 28.dp, readableSidePadding(w))
        }
    }

    @Test fun wideWindowsCentreAReadableColumn() {
        assertEquals(Metrics.readableContentMaxWidth, 720.dp)
        assertEquals(140.dp, readableSidePadding(1000.dp))   // (1000 - 720) / 2
        assertEquals(280.dp, readableSidePadding(1280.dp))   // content stays 720dp wide
    }
}
