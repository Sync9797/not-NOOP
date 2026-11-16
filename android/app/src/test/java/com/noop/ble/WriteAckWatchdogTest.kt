package com.noop.ble

import com.noop.ble.WhoopBleClient.Companion.WriteAckAction
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The confirmed-write watchdog (ryanbr/noop#2805, #2908): a dropped completion callback used to wedge the
 * one-write-at-a-time queue for the rest of the link. Pure decision only; the GATT behaviour needs a strap
 * (see GattCrashSafetyTest's infra note).
 */
class WriteAckWatchdogTest {

    private fun action(
        writeInFlight: Boolean = true,
        hasGatt: Boolean = true,
        bondWrite: Boolean = false,
        hello: Boolean = false,
        didBond: Boolean = true,
        timeouts: Int = 1,
    ) = WhoopBleClient.writeAckAction(writeInFlight, hasGatt, bondWrite, hello, didBond, timeouts)

    @Test fun theFirstDroppedCompletionFreesTheQueue() {
        assertEquals(WriteAckAction.FREE_SLOT, action(timeouts = 1))
    }

    @Test fun aSecondOnTheSameBondedLinkRestartsIt() {
        assertEquals(WhoopBleClient.WRITE_ACK_TIMEOUTS_BEFORE_RECONNECT, 2)
        assertEquals(WriteAckAction.RECONNECT, action(timeouts = 2))
        assertEquals(WriteAckAction.RECONNECT, action(timeouts = 5))
    }

    @Test fun anUnbondedLinkIsNeverRestartedByThisWatchdog() {
        // Not-yet-bonded links feed the #982 / #971 bond counters; a teardown here must not disturb them.
        assertEquals(WriteAckAction.FREE_SLOT, action(didBond = false, timeouts = 9))
    }

    @Test fun itStandsAsideWhenNothingIsOwedOrAnotherWatchdogOwnsTheWrite() {
        assertEquals(WriteAckAction.IGNORE, action(writeInFlight = false))
        assertEquals(WriteAckAction.IGNORE, action(hasGatt = false))
        assertEquals(WriteAckAction.IGNORE, action(bondWrite = true, timeouts = 3))
        assertEquals(WriteAckAction.IGNORE, action(hello = true, timeouts = 3))
    }

    @Test fun theTimeoutIsGenerous() {
        // A confirmed write normally completes in well under a second; never second-guess a busy link.
        assertEquals(15_000L, WhoopBleClient.WRITE_ACK_TIMEOUT_MS)
    }
}
