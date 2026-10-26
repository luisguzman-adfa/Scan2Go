package org.appdevforall.scan2go.util

import kotlin.math.max
import kotlin.math.min

/**
 * Snackbar on-screen time sized to reading time (about 200 wpm), clamped to 3..10 s,
 * so long messages are not cut off. Pass the resolved String (not a resId) so we can
 * count words. Mirrors the K2Go util.SnackbarDuration convention.
 */
object SnackbarDuration {
    private const val MIN_MS = 3000
    private const val MAX_MS = 10000
    private const val WORDS_PER_MINUTE = 200.0

    fun millisForText(message: String): Int {
        val words = message.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
        val readMs = (words / WORDS_PER_MINUTE * 60_000).toInt()
        return min(MAX_MS, max(MIN_MS, readMs))
    }
}
