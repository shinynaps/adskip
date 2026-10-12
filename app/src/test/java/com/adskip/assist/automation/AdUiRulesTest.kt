package com.adskip.assist.automation

import org.junit.Assert.*
import org.junit.Test

class AdUiRulesTest {
    private val topRight = AdUiRules.Bounds(940, 24, 60, 55)
    private val midScreen = AdUiRules.Bounds(460, 800, 80, 65)
    @Test fun explicitAdContextRecognized() {
        assertTrue(AdUiRules.analyze(listOf("Skip ad in 4"), emptyList()).isAd)
        assertTrue(AdUiRules.analyze(listOf("AdChoices"), emptyList()).isAd)
    }
    @Test fun gameRewardMenuNotAutomaticallyDetectedAsAd() {
        assertFalse(AdUiRules.analyze(listOf("Claim your reward", "Watch ad"), emptyList()).isAd)
        assertFalse(AdUiRules.analyze(listOf("Continue", "Close"), emptyList()).isAd)
    }
    @Test fun sdkEvidencePlusAdBadgeRecognized() {
        assertTrue(AdUiRules.analyze(listOf("Ad"), listOf("admob_ad_container")).isAd)
        assertFalse(AdUiRules.analyze(listOf("Ad"), emptyList()).isAd)
    }
    @Test fun extendedDetectionRequiresAdBadgeAndSkipControl() {
        assertFalse(AdUiRules.analyze(listOf("Ad", "Skip"), emptyList()).isAd)
        assertTrue(AdUiRules.analyze(listOf("Ad", "Skip"), emptyList(), extended = true).isAd)
        assertFalse(AdUiRules.analyze(listOf("Skip"), emptyList(), extended = true).isAd)
    }
    @Test fun rewardHintIsNotAdEvidenceByItself() {
        val context = AdUiRules.analyze(listOf("Reward granted"), emptyList())
        assertTrue(context.completionHint)
        assertFalse(context.isAd)
    }
    @Test fun distinguishesValidSkipFromRiskyCta() {
        assertEquals(AdStateMachine.Action.SKIP,
            AdUiRules.classify("Skip ad", "", topRight, 1080, 1920))
        assertNull(AdUiRules.classify("Install now", "ad_close_button", topRight, 1080, 1920))
        assertNull(AdUiRules.classify("Play now", "", topRight, 1080, 1920))
    }
    @Test fun bareXMustBeTopRight() {
        assertEquals(AdStateMachine.Action.CLOSE,
            AdUiRules.classify("×", "", topRight, 1080, 1920))
        assertNull(AdUiRules.classify("×", "", midScreen, 1080, 1920))
    }
    @Test fun countdownLabelsAreNeverTapTargets() {
        assertNull(AdUiRules.classify("Skip in 4", "", topRight, 1080, 1920))
        assertNull(AdUiRules.classify("Skip ad in 2", "", topRight, 1080, 1920))
    }
    @Test fun noGenericContinueOrTapWholeScreen() {
        assertNull(AdUiRules.classify("Continue", "", topRight, 1080, 1920))
        assertNull(AdUiRules.classify("Close", "", AdUiRules.Bounds(0, 0, 1080, 1920), 1080, 1920))
        assertEquals(AdStateMachine.Action.RETURN_TO_GAME,
            AdUiRules.classify("Return to game", "", topRight, 1080, 1920))
    }
}
