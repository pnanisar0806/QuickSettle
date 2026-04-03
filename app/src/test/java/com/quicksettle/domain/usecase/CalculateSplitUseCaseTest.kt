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
}
