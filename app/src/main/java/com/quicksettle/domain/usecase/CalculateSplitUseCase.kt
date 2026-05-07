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
 * [sharesSplit] divides proportionally by integer share counts using the largest-remainder
 * method, also guaranteeing exact paisa invariant.
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
     * Splits [totalAmount] proportionally according to integer share counts in [sharesByPerson].
     *
     * Example: ₹100 with shares {a:2, b:1, c:1} → {a:50.00, b:25.00, c:25.00}.
     *
     * Strategy:
     * - Convert total to paisa (× 100, round to Long).
     * - For each person, base paisa = floor(totalPaisa × shares / totalShares); remainder kept aside.
     * - Distribute leftover paisa one-by-one to people with the largest remainder
     *   (ties broken by stable insertion order). This is the largest-remainder method —
     *   the standard fair-rounding algorithm used by Splitwise and similar apps.
     *
     * Guarantees: sum of returned values equals [totalAmount] exactly (paisa-safe).
     *
     * @throws IllegalArgumentException if [totalAmount] < 0, any share < 0, or sum of shares == 0.
     */
    fun sharesSplit(totalAmount: Double, sharesByPerson: Map<String, Int>): Map<String, Double> {
        require(totalAmount >= 0.0) { "totalAmount must be non-negative" }
        require(sharesByPerson.values.all { it >= 0 }) { "shares must be non-negative" }
        val totalShares = sharesByPerson.values.sum()
        require(totalShares > 0) { "sum of shares must be > 0" }

        val totalPaisa = (totalAmount * 100).roundToLong()

        // Compute base paisa and remainder per person, preserving insertion order.
        data class Allocation(val key: String, var basePaisa: Long, val remainder: Long, val index: Int)
        val allocations = sharesByPerson.entries.mapIndexed { idx, (key, shares) ->
            val numerator = totalPaisa * shares.toLong()
            Allocation(
                key = key,
                basePaisa = numerator / totalShares,
                remainder = numerator % totalShares,
                index = idx,
            )
        }

        val leftoverPaisa = totalPaisa - allocations.sumOf { it.basePaisa }

        // Rank by remainder DESC, ties broken by original index ASC. Take the top N to get +1 paisa each.
        val ranked = allocations.sortedWith(
            compareByDescending<Allocation> { it.remainder }.thenBy { it.index }
        )
        for (i in 0 until leftoverPaisa.toInt()) {
            ranked[i].basePaisa += 1
        }

        // Restore original insertion order on the way out.
        return allocations.associate { it.key to it.basePaisa / 100.0 }
    }
}
