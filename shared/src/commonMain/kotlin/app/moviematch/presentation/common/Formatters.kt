package app.moviematch.presentation.common

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Locale-independent number formatting.
 *
 * `String.format`/`NumberFormat` do not exist in `commonMain`, and the copy in the reference app is
 * produced by JavaScript's `toFixed`, which is always dot-separated — so the formatting is done by
 * hand here and covered by unit tests.
 */

/** `8.8` -> `"8.8"`, `9` -> `"9.0"`, `7.45` -> `"7.5"`. Mirrors `value.toFixed(1)`. */
fun Double.formatDecimals1(): String = formatDecimals(this, decimals = 1)

/** `9.75` -> `"9.75"`, `8` -> `"8.00"`. Mirrors `value.toFixed(2)`. */
fun Double.formatDecimals2(): String = formatDecimals(this, decimals = 2)

/**
 * `9.75` -> `"9.75"`, `8.0` -> `"8"`.
 * Mirrors `value.toFixed(2).replace(/\.00$/, '')` from the consensus ranking.
 */
fun Double.formatDecimals2Trimmed(): String {
    val text = formatDecimals(this, decimals = 2)
    return if (text.endsWith(".00")) text.dropLast(3) else text
}

private fun formatDecimals(value: Double, decimals: Int): String {
    val factor = if (decimals == 1) 10 else 100
    val rounded = (value * factor).roundToInt()
    val sign = if (rounded < 0) "-" else ""
    val digits = abs(rounded)
    val whole = digits / factor
    val fraction = (digits % factor).toString().padStart(decimals, '0')
    return "$sign$whole.$fraction"
}
