package com.quicksettle.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.roundToLong
import kotlin.random.Random

class CalculateSplitUseCaseTest {

    private lateinit var useCase: CalculateSplitUseCase

    @BeforeEach
    fun setUp() {
        useCase = CalculateSplitUseCase()
    }

    // ──────────────────────────────────────────────────────────────
    // equalSplit — exact value checks
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `equalSplit 2 people round amount`() {
        val result = useCase.equalSplit(totalAmount = 200.0, numberOfPeople = 2)
        assertThat(result).hasSize(2)
        assertThat(result[0]).isEqualTo(100.0)
        assertThat(result[1]).isEqualTo(100.0)
    }

    @Test
    fun `equalSplit 3 people canonical example 100 rupees`() {
        // ₹100 / 3 → [33.34, 33.33, 33.33]
        val result = useCase.equalSplit(totalAmount = 100.0, numberOfPeople = 3)
        assertThat(result).hasSize(3)
        assertThat(result[0]).isEqualTo(33.34)
        assertThat(result[1]).isEqualTo(33.33)
        assertThat(result[2]).isEqualTo(33.33)
    }

    @Test
    fun `equalSplit 4 people`() {
        val result = useCase.equalSplit(totalAmount = 100.0, numberOfPeople = 4)
        assertThat(result).hasSize(4)
        result.forEach { assertThat(it).isEqualTo(25.0) }
    }

    @Test
    fun `equalSplit 7 people with 100 rupees`() {
        // 100 / 7 = 14 remainder 2 → [14.29, 14.29, 14.28, 14.28, 14.28, 14.28, 14.28]
        // 1428 * 7 = 9996 paisa, 9 remainder → first 2 get 1429 paisa (14.29)
        val result = useCase.equalSplit(totalAmount = 100.0, numberOfPeople = 7)
        assertThat(result).hasSize(7)
        val sum = result.fold(0L) { acc, v -> acc + (v * 100).roundToLong() }
        assertThat(sum).isEqualTo(10000L)
    }

    @Test
    fun `equalSplit odd amount 10 by 3`() {
        val result = useCase.equalSplit(totalAmount = 10.0, numberOfPeople = 3)
        // 1000 paisa / 3 = 333 rem 1 → [3.34, 3.33, 3.33]
        assertThat(result[0]).isEqualTo(3.34)
        assertThat(result[1]).isEqualTo(3.33)
        assertThat(result[2]).isEqualTo(3.33)
    }

    @Test
    fun `equalSplit 1 rupee by 3`() {
        val result = useCase.equalSplit(totalAmount = 1.0, numberOfPeople = 3)
        // 100 paisa / 3 = 33 rem 1 → [0.34, 0.33, 0.33]
        assertThat(result[0]).isEqualTo(0.34)
        assertThat(result[1]).isEqualTo(0.33)
        assertThat(result[2]).isEqualTo(0.33)
    }

    @Test
    fun `equalSplit smallest unit 1 paisa by 3`() {
        val result = useCase.equalSplit(totalAmount = 0.01, numberOfPeople = 3)
        // 1 paisa / 3 = 0 rem 1 → [0.01, 0.00, 0.00]
        assertThat(result[0]).isEqualTo(0.01)
        assertThat(result[1]).isEqualTo(0.0)
        assertThat(result[2]).isEqualTo(0.0)
    }

    @Test
    fun `equalSplit single person gets full amount`() {
        val result = useCase.equalSplit(totalAmount = 750.50, numberOfPeople = 1)
        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(750.50)
    }

    @Test
    fun `equalSplit zero amount`() {
        val result = useCase.equalSplit(totalAmount = 0.0, numberOfPeople = 4)
        result.forEach { assertThat(it).isEqualTo(0.0) }
    }

    @Test
    fun `equalSplit throws when numberOfPeople is zero`() {
        assertThrows<IllegalArgumentException> {
            useCase.equalSplit(totalAmount = 100.0, numberOfPeople = 0)
        }
    }

    @Test
    fun `equalSplit throws when numberOfPeople is negative`() {
        assertThrows<IllegalArgumentException> {
            useCase.equalSplit(totalAmount = 100.0, numberOfPeople = -1)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // equalSplit — paisa invariant property test (200 random pairs)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `equalSplit paisa invariant holds for 200 random amount-people pairs`() {
        val rng = Random(seed = 42L)
        repeat(200) { iteration ->
            val amount = rng.nextDouble(from = 0.01, until = 100_000.0)
                .let { "%.2f".format(it).toDouble() }   // snap to 2 dp
            val people = rng.nextInt(from = 1, until = 51)

            val splits = useCase.equalSplit(totalAmount = amount, numberOfPeople = people)

            // Convert both sides to paisa (Long) to avoid double comparison drift
            val expectedPaisa = (amount * 100).roundToLong()
            val actualPaisa = splits.fold(0L) { acc, v -> acc + (v * 100).roundToLong() }

            assertThat(actualPaisa)
                .isEqualTo(expectedPaisa)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // unequalSplit
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `unequalSplit returns correct remainder`() {
        val remaining = useCase.unequalSplit(
            totalAmount = 500.0,
            fixedAmounts = mapOf("Alice" to 200.0, "Bob" to 150.0),
        )
        assertThat(remaining).isEqualTo(150.0)
    }

    @Test
    fun `unequalSplit when fixed amounts exactly equal total remaining is zero`() {
        val remaining = useCase.unequalSplit(
            totalAmount = 300.0,
            fixedAmounts = mapOf("Alice" to 150.0, "Bob" to 150.0),
        )
        assertThat(remaining).isEqualTo(0.0)
    }

    @Test
    fun `unequalSplit empty fixedAmounts returns full total`() {
        val remaining = useCase.unequalSplit(
            totalAmount = 999.99,
            fixedAmounts = emptyMap(),
        )
        assertThat(remaining).isEqualTo(999.99)
    }

    @Test
    fun `unequalSplit throws when fixed amounts exceed total`() {
        assertThrows<IllegalArgumentException> {
            useCase.unequalSplit(
                totalAmount = 100.0,
                fixedAmounts = mapOf("Alice" to 60.0, "Bob" to 60.0),
            )
        }
    }

    @Test
    fun `unequalSplit single fixed amount`() {
        val remaining = useCase.unequalSplit(
            totalAmount = 1000.0,
            fixedAmounts = mapOf("Charlie" to 350.75),
        )
        assertThat(remaining).isEqualTo(649.25)
    }

    // ──────────────────────────────────────────────────────────────
    // sharesSplit — exact value checks
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `sharesSplit equal shares exact division 4 people`() {
        // ₹100 with each having 1 share → all 25.00
        val result = useCase.sharesSplit(
            totalAmount = 100.0,
            sharesByPerson = mapOf("a" to 1, "b" to 1, "c" to 1, "d" to 1),
        )
        assertThat(result["a"]).isEqualTo(25.0)
        assertThat(result["b"]).isEqualTo(25.0)
        assertThat(result["c"]).isEqualTo(25.0)
        assertThat(result["d"]).isEqualTo(25.0)
    }

    @Test
    fun `sharesSplit equal shares paisa leftover 3 people`() {
        // ₹100 / 3 shares → totals 100.00; leftover paisa go by remainder rank
        val result = useCase.sharesSplit(
            totalAmount = 100.0,
            sharesByPerson = mapOf("a" to 1, "b" to 1, "c" to 1),
        )
        val sumPaisa = result.values.fold(0L) { acc, v -> acc + (v * 100).roundToLong() }
        assertThat(sumPaisa).isEqualTo(10_000L)
        // Each person gets either 33.33 or 33.34
        result.values.forEach { v ->
            assertThat(v).isAnyOf(33.33, 33.34)
        }
    }

    @Test
    fun `sharesSplit weighted shares exact 120 by 2-1-1`() {
        val result = useCase.sharesSplit(
            totalAmount = 120.0,
            sharesByPerson = mapOf("a" to 2, "b" to 1, "c" to 1),
        )
        assertThat(result["a"]).isEqualTo(60.0)
        assertThat(result["b"]).isEqualTo(30.0)
        assertThat(result["c"]).isEqualTo(30.0)
    }

    @Test
    fun `sharesSplit weighted shares with leftover 100 by 2-1-1`() {
        val result = useCase.sharesSplit(
            totalAmount = 100.0,
            sharesByPerson = mapOf("a" to 2, "b" to 1, "c" to 1),
        )
        assertThat(result["a"]).isEqualTo(50.0)
        assertThat(result["b"]).isEqualTo(25.0)
        assertThat(result["c"]).isEqualTo(25.0)
    }

    @Test
    fun `sharesSplit awkward weights 3-1`() {
        val result = useCase.sharesSplit(
            totalAmount = 100.0,
            sharesByPerson = mapOf("a" to 3, "b" to 1),
        )
        assertThat(result["a"]).isEqualTo(75.0)
        assertThat(result["b"]).isEqualTo(25.0)
    }

    @Test
    fun `sharesSplit awkward weights 2-3`() {
        val result = useCase.sharesSplit(
            totalAmount = 100.0,
            sharesByPerson = mapOf("a" to 2, "b" to 3),
        )
        assertThat(result["a"]).isEqualTo(40.0)
        assertThat(result["b"]).isEqualTo(60.0)
    }

    @Test
    fun `sharesSplit hard rounding 10 by 7 ones`() {
        val result = useCase.sharesSplit(
            totalAmount = 10.0,
            sharesByPerson = mapOf("a" to 1, "b" to 1, "c" to 1, "d" to 1, "e" to 1, "f" to 1, "g" to 1),
        )
        val sumPaisa = result.values.fold(0L) { acc, v -> acc + (v * 100).roundToLong() }
        assertThat(sumPaisa).isEqualTo(1_000L)
        // Every person gets either 1.42 or 1.43
        result.values.forEach { v -> assertThat(v).isAnyOf(1.42, 1.43) }
    }

    @Test
    fun `sharesSplit zero share participant gets zero`() {
        val result = useCase.sharesSplit(
            totalAmount = 100.0,
            sharesByPerson = mapOf("a" to 1, "b" to 0, "c" to 1),
        )
        assertThat(result["a"]).isEqualTo(50.0)
        assertThat(result["b"]).isEqualTo(0.0)
        assertThat(result["c"]).isEqualTo(50.0)
    }

    @Test
    fun `sharesSplit single participant takes all`() {
        val result = useCase.sharesSplit(
            totalAmount = 100.0,
            sharesByPerson = mapOf("a" to 5),
        )
        assertThat(result["a"]).isEqualTo(100.0)
    }

    @Test
    fun `sharesSplit zero amount`() {
        val result = useCase.sharesSplit(
            totalAmount = 0.0,
            sharesByPerson = mapOf("a" to 1, "b" to 2),
        )
        assertThat(result["a"]).isEqualTo(0.0)
        assertThat(result["b"]).isEqualTo(0.0)
    }

    @Test
    fun `sharesSplit throws when total shares is zero`() {
        assertThrows<IllegalArgumentException> {
            useCase.sharesSplit(
                totalAmount = 100.0,
                sharesByPerson = mapOf("a" to 0, "b" to 0),
            )
        }
    }

    @Test
    fun `sharesSplit throws when shares are negative`() {
        assertThrows<IllegalArgumentException> {
            useCase.sharesSplit(
                totalAmount = 100.0,
                sharesByPerson = mapOf("a" to -1, "b" to 2),
            )
        }
    }

    @Test
    fun `sharesSplit throws when total amount is negative`() {
        assertThrows<IllegalArgumentException> {
            useCase.sharesSplit(
                totalAmount = -1.0,
                sharesByPerson = mapOf("a" to 1),
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // sharesSplit — paisa invariant property test (200 random configs)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `sharesSplit paisa invariant holds for 200 random configurations`() {
        val rng = Random(seed = 7L)
        repeat(200) {
            val amount = rng.nextDouble(from = 0.01, until = 100_000.0)
                .let { "%.2f".format(it).toDouble() }   // snap to 2dp
            val n = rng.nextInt(from = 2, until = 9)
            val sharesMap = (0 until n).associate { idx ->
                "p$idx" to rng.nextInt(from = 1, until = 10)
            }
            val result = useCase.sharesSplit(totalAmount = amount, sharesByPerson = sharesMap)

            val expectedPaisa = (amount * 100).roundToLong()
            val actualPaisa = result.values.fold(0L) { acc, v -> acc + (v * 100).roundToLong() }
            assertThat(actualPaisa).isEqualTo(expectedPaisa)
        }
    }
}
