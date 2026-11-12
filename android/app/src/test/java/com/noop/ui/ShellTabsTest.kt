package com.noop.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bottom bar and the large-screen rail draw ONE tab list with ONE active rule (#2218). Pins that the
 * shared rule lights exactly what the bottom bar lit before it was shared.
 */
class ShellTabsTest {

    @Test fun theShellListIsTheBarTabsThenMore() {
        for (coach in listOf(true, false)) {
            assertEquals(
                (barLeadingTabs + barTrailingTabsFor(coach)).map { it.dest } + Destination.More,
                shellTabsFor(coach).map { it.dest },
            )
        }
    }

    @Test fun exactlyOneTabIsLitForEveryDestination() {
        for (coach in listOf(true, false)) {
            val tabs = shellTabsFor(coach)
            for (dest in Destination.entries) {
                val lit = tabs.filter { isShellTabActive(it, dest, tabs) }.map { it.dest }
                assertEquals("coach=$coach, $dest lit $lit", 1, lit.size)
            }
        }
    }

    @Test fun theSharedRuleMatchesTheOriginalBottomBarRule() {
        for (coach in listOf(true, false)) {
            val tabs = shellTabsFor(coach)
            val visibleTrailing = barTrailingTabsFor(coach)
            for (dest in Destination.entries) {
                for (tab in tabs) {
                    // The bottom bar's rule before the rail existed: a tab lights for its own destination;
                    // More lights for anything that is not one of the bar's own tabs.
                    val original = if (tab.dest == Destination.More) {
                        barLeadingTabs.none { it.dest == dest } && visibleTrailing.none { it.dest == dest }
                    } else {
                        dest == tab.dest
                    }
                    assertEquals("coach=$coach, tab=${tab.dest}, current=$dest", original, isShellTabActive(tab, dest, tabs))
                }
            }
        }
    }
}
