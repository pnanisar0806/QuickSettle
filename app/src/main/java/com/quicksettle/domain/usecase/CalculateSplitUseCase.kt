package com.quicksettle.domain.usecase

import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Pure business logic for bill splitting.
 *
 * [equalSplit] guarantees that the sum of all returned values equals [totalAmount] exactly
 * (zero leftover paisa). Extra paisa from integer division are distributed one-per-person
 * from the first participant onwards.
 *
 * [unequalSplit] returns the remaining amount after subtracting fixed per-person amounts.
 */
class CalculateSplitUseCase @Inject constructor() {

    /**
     * Splits [totalAmount] equally among [numberOfPeople] participants.
     *
     * Strategy:
     * - Convert to paisa (× 100, round to Long) to avoid floating-point drift.
     * - Assign floor(totalPaisa / n) to every person.
     * - Distribute the remaining (totalPaisa % n) paisa, one extra paisa each, to the
     *   first (remainder) participants.
     *
     * Example: ₹100 / 3 → [33.34, 33.33, 33.33]
     */
    fun equalSplit(totalAmount: Double, numberOfPeople: Int): List<Double> {
        require(numberOfPeople > 0) { "numberOfPeople must be > 0" }
        require(totalAmount >= 0.0) { "totalAmount must be non-negative" }

        val totalPaisa = (totalAmount * 100).roundToLong()
        val baseSharePaisa = totalPaisa / numberOfPeople
        val remainderPaisa = (totalPaisa % numberOfPeople).toInt()

        return List(numberOfPeople) { index ->
            val sharePaisa = if (index < remainderPaisa) baseSharePaisa + 1 else baseSharePaisa
            sharePaisa / 100.0
        }
    }

    /**
     * Returns the leftover amount after subtracting [fixedAmounts] from [totalAmount].
     *
     * @throws IllegalArgumentException if the sum of fixed amounts exceeds [totalAmount].
     */
    fun unequalSplit(totalAmount: Double, fixedAmounts: Map<String, Double>): Double {
        val totalFixed = fixedAmounts.values.fold(0.0) { acc, v -> acc + v }
        require(totalFixed <= totalAmount + 1e-9) {
            "Fixed amounts (%.2f) exceed total (%.2f)".format(totalFixed, totalAmount)
        }
        // Round the remaining amount to 2 decimal places to avoid floating-point noise.
        val remaining = (totalAmount * 100).roundToLong() - (totalFixed * 100).roundToLong()
        return remaining / 100.0
    }
}
