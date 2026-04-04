package com.quicksettle.presentation.screens.entry

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EntryUiStateTest {

    // ── canProceed ───────────────────────────────────────────────────

    @Nested
    inner class CanProceed {
        @Test
        fun `cannot proceed with empty amount`() {
            val state = EntryUiState(rawAmount = "")
            assertThat(state.canProceed).isFalse()
        }

        @Test
        fun `cannot proceed with zero amount`() {
            val state = EntryUiState(rawAmount = "0")
            assertThat(state.canProceed).isFalse()
        }

        @Test
        fun `cannot proceed with zero point zero`() {
            val state = EntryUiState(rawAmount = "0.00")
            assertThat(state.canProceed).isFalse()
        }

        @Test
        fun `can proceed with valid amount`() {
            val state = EntryUiState(rawAmount = "150")
            assertThat(state.canProceed).isTrue()
        }

        @Test
        fun `can proceed with decimal amount`() {
            val state = EntryUiState(rawAmount = "99.50")
            assertThat(state.canProceed).isTrue()
        }

        @Test
        fun `can proceed with small amount`() {
            val state = EntryUiState(rawAmount = "0.01")
            assertThat(state.canProceed).isTrue()
        }
    }

    // ── formattedAmount ──────────────────────────────────────────────

    @Nested
    inner class FormattedAmount {
        @Test
        fun `empty input formats as zero`() {
            val state = EntryUiState(rawAmount = "")
            assertThat(state.formattedAmount).isEqualTo("0")
        }

        @Test
        fun `single digit`() {
            val state = EntryUiState(rawAmount = "5")
            assertThat(state.formattedAmount).isEqualTo("5")
        }

        @Test
        fun `three digits no comma`() {
            val state = EntryUiState(rawAmount = "999")
            assertThat(state.formattedAmount).isEqualTo("999")
        }

        @Test
        fun `four digits with Indian comma`() {
            val state = EntryUiState(rawAmount = "1234")
            assertThat(state.formattedAmount).isEqualTo("1,234")
        }

        @Test
        fun `five digits with Indian comma`() {
            val state = EntryUiState(rawAmount = "12345")
            assertThat(state.formattedAmount).isEqualTo("12,345")
        }

        @Test
        fun `six digits with Indian grouping`() {
            val state = EntryUiState(rawAmount = "123456")
            assertThat(state.formattedAmount).isEqualTo("1,23,456")
        }

        @Test
        fun `seven digits full Indian grouping`() {
            val state = EntryUiState(rawAmount = "1234567")
            assertThat(state.formattedAmount).isEqualTo("12,34,567")
        }

        @Test
        fun `decimal part preserved`() {
            val state = EntryUiState(rawAmount = "1234.56")
            assertThat(state.formattedAmount).isEqualTo("1,234.56")
        }

        @Test
        fun `trailing dot preserved`() {
            val state = EntryUiState(rawAmount = "100.")
            assertThat(state.formattedAmount).isEqualTo("100.")
        }

        @Test
        fun `decimal with single digit preserved`() {
            val state = EntryUiState(rawAmount = "100.5")
            assertThat(state.formattedAmount).isEqualTo("100.5")
        }

        @Test
        fun `zero formats as zero`() {
            val state = EntryUiState(rawAmount = "0")
            assertThat(state.formattedAmount).isEqualTo("0")
        }

        @Test
        fun `zero with decimal`() {
            val state = EntryUiState(rawAmount = "0.5")
            assertThat(state.formattedAmount).isEqualTo("0.5")
        }
    }
}
