package com.quicksettle.presentation.util

/**
 * Shared amount formatting with Indian comma grouping (lakhs/crores).
 * Used by both the Entry screen and Crew screen.
 */
object AmountFormatter {

    /**
     * Formats a Double amount with Indian grouping and optional 2dp fraction.
     * Handles negative values. Omits fraction when it's .00.
     *
     * Examples: 1234.50 → "1,234.50", 100000.0 → "1,00,000", -30.0 → "-30"
     */
    fun format(amount: Double): String {
        val negative = amount < 0
        val absPaisa = kotlin.math.abs((amount * 100).toLong())
        val intPart = absPaisa / 100
        val fracPart = absPaisa % 100
        val intStr = formatIndianGrouping(intPart)
        val formatted = if (fracPart == 0L) intStr else "$intStr.${fracPart.toString().padStart(2, '0')}"
        return if (negative) "-$formatted" else formatted
    }

    /**
     * Formats the integer part of a raw amount string with Indian grouping.
     * Preserves the decimal part as-is (for live input where user may type "100.").
     *
     * Examples: "1234567" → "12,34,567", "1234.5" → "1,234.5", "" → "0"
     */
    fun formatRawInput(rawAmount: String): String {
        if (rawAmount.isEmpty()) return "0"
        val parts = rawAmount.split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else null
        val intValue = intPart.toLongOrNull() ?: 0L
        val formatted = formatIndianGrouping(intValue)
        return if (decPart != null) "$formatted.$decPart" else formatted
    }

    /**
     * Indian comma grouping: last 3 digits, then groups of 2.
     * e.g. 1234567 → "12,34,567"
     */
    fun formatIndianGrouping(number: Long): String {
        if (number == 0L) return "0"
        val str = number.toString()
        if (str.length <= 3) return str
        val lastThree = str.takeLast(3)
        val remaining = str.dropLast(3)
        val groups = mutableListOf<String>()
        var i = remaining.length
        while (i > 0) {
            val start = maxOf(0, i - 2)
            groups.add(0, remaining.substring(start, i))
            i = start
        }
        return groups.joinToString(",") + "," + lastThree
    }
}
