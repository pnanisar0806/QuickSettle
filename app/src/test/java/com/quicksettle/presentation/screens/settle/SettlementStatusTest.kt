package com.quicksettle.presentation.screens.settle

import com.google.common.truth.Truth.assertThat
import com.quicksettle.presentation.theme.Error
import com.quicksettle.presentation.theme.OnSurfaceVariant
import com.quicksettle.presentation.theme.TertiaryFixedDim
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SettlementStatusTest {

    @Nested
    inner class DisplayText {
        @Test
        fun `PENDING shows days`() {
            assertThat(SettlementStatus.PENDING.displayText(pendingDays = 3)).isEqualTo("PENDING 3D")
        }

        @Test
        fun `PENDING with zero days`() {
            assertThat(SettlementStatus.PENDING.displayText(pendingDays = 0)).isEqualTo("PENDING 0D")
        }

        @Test
        fun `URGENT text`() {
            assertThat(SettlementStatus.URGENT.displayText()).isEqualTo("URGENT")
        }

        @Test
        fun `DUE_TODAY text`() {
            assertThat(SettlementStatus.DUE_TODAY.displayText()).isEqualTo("DUE TODAY")
        }

        @Test
        fun `PAID text`() {
            assertThat(SettlementStatus.PAID.displayText()).isEqualTo("PAID")
        }
    }

    @Nested
    inner class BadgeColor {
        @Test
        fun `PENDING uses OnSurfaceVariant`() {
            assertThat(SettlementStatus.PENDING.badgeColor()).isEqualTo(OnSurfaceVariant)
        }

        @Test
        fun `URGENT uses Error`() {
            assertThat(SettlementStatus.URGENT.badgeColor()).isEqualTo(Error)
        }

        @Test
        fun `DUE_TODAY uses OnSurfaceVariant`() {
            assertThat(SettlementStatus.DUE_TODAY.badgeColor()).isEqualTo(OnSurfaceVariant)
        }

        @Test
        fun `PAID uses TertiaryFixedDim`() {
            assertThat(SettlementStatus.PAID.badgeColor()).isEqualTo(TertiaryFixedDim)
        }
    }
}
