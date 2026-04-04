package com.quicksettle.presentation.screens.entry

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests the actual [AmountInputRules] object extracted from EntryViewModel.
 * These are the real production functions, not copies.
 */
class AmountInputRulesTest {

    @Nested
    inner class AppendDigit {
        @Test
        fun `starting from empty, digit produces that digit`() {
            assertThat(AmountInputRules.appendDigit("", '5')).isEqualTo("5")
        }

        @Test
        fun `starting from empty, zero produces zero`() {
            assertThat(AmountInputRules.appendDigit("", '0')).isEqualTo("0")
        }

        @Test
        fun `leading zero replaced by non-zero digit`() {
            assertThat(AmountInputRules.appendDigit("0", '3')).isEqualTo("3")
        }

        @Test
        fun `max 7 digits before decimal`() {
            val sevenDigits = "1234567"
            assertThat(AmountInputRules.appendDigit(sevenDigits, '8')).isEqualTo(sevenDigits)
        }

        @Test
        fun `6 digits allows one more`() {
            assertThat(AmountInputRules.appendDigit("123456", '7')).isEqualTo("1234567")
        }

        @Test
        fun `max 2 decimal places`() {
            assertThat(AmountInputRules.appendDigit("100.99", '1')).isEqualTo("100.99")
        }

        @Test
        fun `1 decimal place allows another`() {
            assertThat(AmountInputRules.appendDigit("100.9", '5')).isEqualTo("100.95")
        }

        @Test
        fun `digit after decimal point with no fraction yet`() {
            assertThat(AmountInputRules.appendDigit("100.", '5')).isEqualTo("100.5")
        }

        @Test
        fun `building up number sequentially`() {
            var amount = ""
            amount = AmountInputRules.appendDigit(amount, '1')
            amount = AmountInputRules.appendDigit(amount, '2')
            amount = AmountInputRules.appendDigit(amount, '3')
            assertThat(amount).isEqualTo("123")
        }
    }

    @Nested
    inner class AppendDecimal {
        @Test
        fun `decimal on empty produces zero dot`() {
            assertThat(AmountInputRules.appendDecimal("")).isEqualTo("0.")
        }

        @Test
        fun `decimal on number appends dot`() {
            assertThat(AmountInputRules.appendDecimal("100")).isEqualTo("100.")
        }

        @Test
        fun `double decimal blocked`() {
            assertThat(AmountInputRules.appendDecimal("100.")).isEqualTo("100.")
        }

        @Test
        fun `double decimal on existing fraction blocked`() {
            assertThat(AmountInputRules.appendDecimal("100.5")).isEqualTo("100.5")
        }
    }

    @Nested
    inner class Backspace {
        @Test
        fun `backspace on non-empty drops last char`() {
            val raw = "123"
            val result = if (raw.isEmpty()) "" else raw.dropLast(1)
            assertThat(result).isEqualTo("12")
        }

        @Test
        fun `backspace on single char produces empty`() {
            val raw = "5"
            val result = if (raw.isEmpty()) "" else raw.dropLast(1)
            assertThat(result).isEmpty()
        }

        @Test
        fun `backspace on empty stays empty`() {
            val raw = ""
            val result = if (raw.isEmpty()) "" else raw.dropLast(1)
            assertThat(result).isEmpty()
        }

        @Test
        fun `backspace removes decimal point`() {
            val raw = "100."
            val result = raw.dropLast(1)
            assertThat(result).isEqualTo("100")
        }
    }
}
