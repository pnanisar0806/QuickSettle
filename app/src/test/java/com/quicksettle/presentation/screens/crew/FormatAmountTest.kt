package com.quicksettle.presentation.screens.crew

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FormatAmountTest {

    @Nested
    inner class WholeAmounts {
        @Test
        fun `zero`() {
            assertThat(formatAmount(0.0)).isEqualTo("0")
        }

        @Test
        fun `single digit`() {
            assertThat(formatAmount(5.0)).isEqualTo("5")
        }

        @Test
        fun `three digits no comma`() {
            assertThat(formatAmount(999.0)).isEqualTo("999")
        }

        @Test
        fun `four digits Indian comma`() {
            assertThat(formatAmount(1234.0)).isEqualTo("1,234")
        }

        @Test
        fun `five digits Indian comma`() {
            assertThat(formatAmount(12345.0)).isEqualTo("12,345")
        }

        @Test
        fun `six digits Indian grouping`() {
            assertThat(formatAmount(123456.0)).isEqualTo("1,23,456")
        }

        @Test
        fun `seven digits full Indian grouping`() {
            assertThat(formatAmount(1234567.0)).isEqualTo("12,34,567")
        }

        @Test
        fun `one lakh`() {
            assertThat(formatAmount(100000.0)).isEqualTo("1,00,000")
        }

        @Test
        fun `ten lakh`() {
            assertThat(formatAmount(1000000.0)).isEqualTo("10,00,000")
        }
    }

    @Nested
    inner class FractionalAmounts {
        @Test
        fun `amount with 50 paise`() {
            assertThat(formatAmount(100.50)).isEqualTo("100.50")
        }

        @Test
        fun `amount with single paisa`() {
            assertThat(formatAmount(100.01)).isEqualTo("100.01")
        }

        @Test
        fun `amount with 99 paise`() {
            assertThat(formatAmount(100.99)).isEqualTo("100.99")
        }

        @Test
        fun `large amount with paise`() {
            assertThat(formatAmount(12345.67)).isEqualTo("12,345.67")
        }

        @Test
        fun `lakh amount with paise`() {
            assertThat(formatAmount(123456.78)).isEqualTo("1,23,456.78")
        }

        @Test
        fun `small fractional`() {
            assertThat(formatAmount(0.50)).isEqualTo("0.50")
        }

        @Test
        fun `one paisa`() {
            assertThat(formatAmount(0.01)).isEqualTo("0.01")
        }
    }

    @Nested
    inner class EdgeCases {
        @Test
        fun `typical split amount 33_33`() {
            assertThat(formatAmount(33.33)).isEqualTo("33.33")
        }

        @Test
        fun `typical split amount 33_34`() {
            assertThat(formatAmount(33.34)).isEqualTo("33.34")
        }

        @Test
        fun `amount with trailing zero in decimal`() {
            assertThat(formatAmount(250.10)).isEqualTo("250.10")
        }
    }

    @Nested
    inner class NegativeAmounts {
        @Test
        fun `negative whole amount`() {
            assertThat(formatAmount(-30.0)).isEqualTo("-30")
        }

        @Test
        fun `negative fractional amount`() {
            assertThat(formatAmount(-0.50)).isEqualTo("-0.50")
        }

        @Test
        fun `negative one paisa`() {
            assertThat(formatAmount(-0.01)).isEqualTo("-0.01")
        }

        @Test
        fun `negative large amount with Indian grouping`() {
            assertThat(formatAmount(-12345.67)).isEqualTo("-12,345.67")
        }

        @Test
        fun `negative zero is zero`() {
            assertThat(formatAmount(-0.0)).isEqualTo("0")
        }
    }
}
