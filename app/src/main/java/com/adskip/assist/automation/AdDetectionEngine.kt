package com.adskip.assist.automation

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque
import java.util.Locale

/** Conservative, explainable accessibility heuristics. No screenshots or OCR. */
class AdDetectionEngine {
    data class Candidate(val kind: AdStateMachine.Action, val node: AccessibilityNodeInfo, val score: Int)
    data class Detection(
        val adEvidence: Boolean,
        val completionHint: Boolean,
        val candidates: List<Candidate>,
    ) {
        fun target(kind: AdStateMachine.Action): AccessibilityNodeInfo? =
            candidates.firstOrNull { it.kind == kind }?.node
    }
    fun detect(root: AccessibilityNodeInfo, width: Int, height: Int, extended: Boolean = false): Detection {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        val nodes = ArrayList<Pair<AccessibilityNodeInfo, String>>()
        var count = 0
        while (queue.isNotEmpty() && count++ < 350) {
            val node = queue.removeFirst()
            if (!node.isVisibleToUser) continue
            val label = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
                .joinToString(" ").trim().lowercase(Locale.ROOT)
            nodes.add(node to label)
            for (i in 0 until node.childCount.coerceAtMost(30)) {
                node.getChild(i)?.let(queue::addLast)
            }
        }
        val context = AdUiRules.analyze(
            nodes.map { it.second }, nodes.map { it.first.viewIdResourceName.orEmpty() }, extended)
        if (!context.isAd) return Detection(false, false, emptyList())

        val found = mutableListOf<Candidate>()
        nodes.forEach { (node, label) ->
            val id = node.viewIdResourceName.orEmpty().lowercase(Locale.ROOT)
            val bounds = Rect().also(node::getBoundsInScreen)
            val kind = AdUiRules.classify(label, id,
                AdUiRules.Bounds(bounds.left, bounds.top, bounds.width(), bounds.height()),
                width, height) ?: return@forEach
            val clickable = findClickable(node, width, height) ?: return@forEach
            val score = when {
                label.contains("ad") || label.contains("game") -> 100
                kind == AdStateMachine.Action.CLOSE -> 80
                else -> 60
            }
            found.add(Candidate(kind, clickable, score))
        }
        // Return/Close first, then Skip. Never select a CTA/install target.
        val ordered = found.sortedWith(compareBy<Candidate> {
            when (it.kind) {
                AdStateMachine.Action.RETURN_TO_GAME -> 0
                AdStateMachine.Action.CLOSE -> 1
                AdStateMachine.Action.SKIP -> 2
            }
        }.thenByDescending { it.score })
        return Detection(true, context.completionHint, ordered)
    }

    private fun findClickable(node: AccessibilityNodeInfo, width: Int, height: Int): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        repeat(4) {
            val checking = current ?: return null
            val bounds = Rect().also(checking::getBoundsInScreen)
            if (checking.isEnabled && checking.isVisibleToUser && checking.isClickable &&
                bounds.width() in 1..(width * .45).toInt() &&
                bounds.height() in 1..(height * .35).toInt()) return checking
            current = checking.parent
        }
        return null
    }
}
