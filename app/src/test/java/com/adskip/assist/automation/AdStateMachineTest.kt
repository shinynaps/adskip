package com.adskip.assist.automation

import org.junit.Assert.*
import org.junit.Test

class AdStateMachineTest {
    private val skip = AdStateMachine.Action.SKIP
    private fun frame(pkg: String = "game.one", evidence: Boolean = true,
                      completed: Boolean = false, actions: List<AdStateMachine.Action> = listOf(skip)) =
        AdStateMachine.Frame(pkg, evidence, completed, actions)

    @Test fun neverInteractsWithoutAdEvidence() {
        val machine = AdStateMachine()
        assertEquals(AdStateMachine.Decision.Ignore,
            machine.observe(frame(evidence = false), AdStateMachine.Mode.FAST_SKIP, 40_000, 100))
        assertNull(machine.session)
    }
    @Test fun rewardPriorityWaitsBeforeActing() {
        val machine = AdStateMachine()
        assertTrue(machine.observe(frame(), AdStateMachine.Mode.REWARD_PRIORITY, 40_000, 1_000)
            is AdStateMachine.Decision.Started)
        assertEquals(AdStateMachine.Decision.Wait(30_000),
            machine.observe(frame(), AdStateMachine.Mode.REWARD_PRIORITY, 40_000, 11_000))
        assertEquals(AdStateMachine.Decision.Click(skip),
            machine.observe(frame(), AdStateMachine.Mode.REWARD_PRIORITY, 40_000, 41_000))
    }
    @Test fun fastSkipActsImmediatelyAfterDetection() {
        val machine = AdStateMachine()
        machine.observe(frame(), AdStateMachine.Mode.FAST_SKIP, 40_000, 100)
        assertEquals(AdStateMachine.Decision.Click(skip),
            machine.observe(frame(), AdStateMachine.Mode.FAST_SKIP, 40_000, 101))
    }
    @Test fun completionHintAllowsEarlierActionButDoesNotProveReward() {
        val machine = AdStateMachine()
        machine.observe(frame(), AdStateMachine.Mode.REWARD_PRIORITY, 40_000, 0)
        assertEquals(AdStateMachine.Decision.Click(skip),
            machine.observe(frame(completed = true), AdStateMachine.Mode.REWARD_PRIORITY, 40_000, 500))
    }
    @Test fun currentFrameEvidenceRequiredEvenDuringActiveSession() {
        val machine = AdStateMachine()
        machine.observe(frame(), AdStateMachine.Mode.FAST_SKIP, 40_000, 0)
        assertEquals(AdStateMachine.Decision.Ignore,
            machine.observe(frame(evidence = false), AdStateMachine.Mode.FAST_SKIP, 40_000, 1_000))
        assertEquals(AdStateMachine.Decision.Ignore,
            machine.observe(frame(evidence = false), AdStateMachine.Mode.FAST_SKIP, 40_000, 6_100))
        assertNull(machine.session)
    }
    @Test fun switchingGamesStartsNewSession() {
        val machine = AdStateMachine()
        machine.observe(frame(), AdStateMachine.Mode.FAST_SKIP, 40_000, 0)
        assertEquals(AdStateMachine.Decision.Started("game.two"),
            machine.observe(frame(pkg = "game.two"), AdStateMachine.Mode.FAST_SKIP, 40_000, 3_000))
        assertEquals("game.two", machine.session?.packageName)
    }
    @Test fun actionRateLimitedAndEventuallyExpires() {
        val machine = AdStateMachine()
        machine.observe(frame(), AdStateMachine.Mode.FAST_SKIP, 40_000, 0)
        for (i in 1..5) {
            val now = i * 2_000L
            assertEquals(AdStateMachine.Decision.Click(skip),
                machine.observe(frame(), AdStateMachine.Mode.FAST_SKIP, 40_000, now))
            machine.actionAttempted(now)
            if (i == 1) assertTrue(machine.observe(frame(), AdStateMachine.Mode.FAST_SKIP, 40_000, now + 50)
                is AdStateMachine.Decision.Wait)
        }
        assertEquals(AdStateMachine.Decision.Expired,
            machine.observe(frame(), AdStateMachine.Mode.FAST_SKIP, 40_000, 12_000))
    }
    @Test fun ignoresMissingAccessibleActions() {
        val machine = AdStateMachine()
        machine.observe(frame(actions = emptyList()), AdStateMachine.Mode.FAST_SKIP, 40_000, 0)
        assertEquals(AdStateMachine.Decision.Wait(0),
            machine.observe(frame(actions = emptyList()), AdStateMachine.Mode.FAST_SKIP, 40_000, 1_000))
    }
    @Test fun noEvidenceOnUnrelatedPackageNeverClicks() {
        val machine = AdStateMachine()
        machine.observe(frame(), AdStateMachine.Mode.FAST_SKIP, 40_000, 0)
        assertEquals(AdStateMachine.Decision.Ignore,
            machine.observe(frame(pkg = "other.app", evidence = false), AdStateMachine.Mode.FAST_SKIP, 40_000, 200))
        assertNull(machine.session)
    }
}
