package com.noop.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

// MARK: - Large screens and foldables (Android-only, Not-Noop)
//
// The phone layout is unchanged. Everything here only takes effect on a wide window (a tablet, an
// unfolded foldable, a phone in landscape) or a half-folded one. UI parity with the Swift apps is
// feature-level, and this is Android window management with no Apple twin, so nothing here touches
// stored data or analytics.

/** Material's window width classes: compact < 600dp <= medium < 840dp <= expanded. */
enum class WindowWidthClass {
    COMPACT, MEDIUM, EXPANDED;

    companion object {
        fun forWidth(width: Dp): WindowWidthClass = when {
            width < 600.dp -> COMPACT
            width < 840.dp -> MEDIUM
            else -> EXPANDED
        }
    }
}

/** How the app shell presents its top-level navigation. */
enum class ShellNavigation {
    /** The floating glass bar along the bottom: phones, and anything compact. */
    BOTTOM_BAR,

    /** A glass rail down the end (right) edge: wide windows, where a bottom bar wastes height. */
    RAIL,

    /** Half-folded with a horizontal hinge: screens above the hinge, the bar below it. */
    TABLETOP,
}

/** A half-opened horizontal fold, as pixel offsets from the top of the app shell. */
data class TabletopHinge(val topPx: Int, val bottomPx: Int)

/** Tabletop wins over width (a half-folded device is held like a laptop); then wide windows get the rail. */
internal fun shellNavigationFor(widthClass: WindowWidthClass, tabletop: TabletopHinge?): ShellNavigation = when {
    tabletop != null -> ShellNavigation.TABLETOP
    widthClass != WindowWidthClass.COMPACT -> ShellNavigation.RAIL
    else -> ShellNavigation.BOTTOM_BAR
}

/**
 * Pure: the tabletop hinge in shell coordinates, or null when the fold is not a tabletop one.
 *
 * Tabletop means HALF_OPENED with a HORIZONTAL fold. The fold's bounds are in window coordinates, so
 * they are shifted by the shell's own top. A fold outside the shell, or one that would leave either
 * half shorter than [minHalfPx], is ignored so the layout never squeezes a screen into a sliver (a
 * window in split-screen can sit almost entirely on one side of the hinge).
 */
internal fun tabletopHinge(
    halfOpened: Boolean,
    horizontal: Boolean,
    foldTopInWindowPx: Int,
    foldBottomInWindowPx: Int,
    shellTopInWindowPx: Int,
    shellHeightPx: Int,
    minHalfPx: Int,
): TabletopHinge? {
    if (!halfOpened || !horizontal || shellHeightPx <= 0) return null
    val top = foldTopInWindowPx - shellTopInWindowPx
    val bottom = foldBottomInWindowPx - shellTopInWindowPx
    if (top < minHalfPx || shellHeightPx - bottom < minHalfPx || bottom < top) return null
    return TabletopHinge(topPx = top, bottomPx = bottom)
}

/**
 * The current tabletop hinge, or null. Reads androidx.window's fold reports for the hosting activity;
 * on a device without a fold the flow simply never reports one.
 */
@Composable
internal fun rememberTabletopHinge(shellTopInWindowPx: Int, shellHeightPx: Int, minHalfPx: Int): TabletopHinge? {
    val activity = LocalContext.current.findHostActivity()
    val layoutInfoFlow = remember(activity) {
        activity?.let { WindowInfoTracker.getOrCreate(it).windowLayoutInfo(it) }
    }
    val layoutInfo = layoutInfoFlow?.collectAsState(initial = null)?.value
    val fold = layoutInfo?.displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull()
        ?: return null
    return tabletopHinge(
        halfOpened = fold.state == FoldingFeature.State.HALF_OPENED,
        horizontal = fold.orientation == FoldingFeature.Orientation.HORIZONTAL,
        foldTopInWindowPx = fold.bounds.top,
        foldBottomInWindowPx = fold.bounds.bottom,
        shellTopInWindowPx = shellTopInWindowPx,
        shellHeightPx = shellHeightPx,
        minHalfPx = minHalfPx,
    )
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

/**
 * Pure: the side margin that keeps content at most [maxContent] wide, centred, and never less than
 * [minimum]. On a phone this is just [minimum] (the standard 28dp screen padding), so the phone layout
 * is identical; on a tablet the margins grow instead of every card stretching edge to edge.
 */
internal fun readableSidePadding(
    available: Dp,
    minimum: Dp = 28.dp,
    maxContent: Dp = Metrics.readableContentMaxWidth,
): Dp = maxOf(minimum, (available - maxContent) / 2)

/**
 * Horizontal padding of [readableSidePadding], measured from the width this element is actually given
 * (so a rail beside it is accounted for). A layout modifier rather than BoxWithConstraints so an eager
 * screen column gains no subcomposition. Apply it INSIDE a scroll modifier: the scroll container stays
 * full width, so the margins still scroll the screen.
 */
internal fun Modifier.readableHorizontalPadding(minimum: Dp = 28.dp): Modifier = layout { measurable, constraints ->
    val side = if (constraints.hasBoundedWidth) {
        readableSidePadding(constraints.maxWidth.toDp(), minimum).roundToPx()
    } else {
        minimum.roundToPx()
    }
    val placeable = measurable.measure(constraints.offset(horizontal = -2 * side))
    val width = constraints.constrainWidth(placeable.width + 2 * side)
    val height = constraints.constrainHeight(placeable.height)
    layout(width, height) { placeable.placeRelative(side, 0) }
}
