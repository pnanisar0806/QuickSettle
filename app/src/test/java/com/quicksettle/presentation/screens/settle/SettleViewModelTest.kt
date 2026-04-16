package com.quicksettle.presentation.screens.settle

import com.google.common.truth.Truth.assertThat
import com.quicksettle.domain.usecase.GenerateUpiLinkUseCase
import com.quicksettle.testutil.FakeUserProfile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SettleViewModelTest {

    private val useCase = GenerateUpiLinkUseCase()

    private val testSplits = listOf(
        SplitEntry(name = "Alice", amount = 250.0, upiId = "alice@upi"),
        SplitEntry(name = "Bob", amount = 150.0, upiId = "bob@upi"),
        SplitEntry(name = "Charlie", amount = 100.0, upiId = "charlie@upi"),
    )

    private val testSplitsNoUpi = listOf(
        SplitEntry(name = "Alice", amount = 250.0, upiId = null),
        SplitEntry(name = "Bob", amount = 150.0, upiId = null),
    )

    private lateinit var fakeProfile: FakeUserProfile

    private fun makeViewModel(
        displayName: String? = "TestUser",
        upiId: String? = "testuser@upi",
    ): SettleViewModel {
        fakeProfile = FakeUserProfile(displayName = displayName, upiId = upiId)
        return SettleViewModel(
            generateUpiLinkUseCase = useCase,
            userProfile = fakeProfile,
        )
    }

    // ── Initialization ────────────────────────────────────────────────────────

    @Nested
    inner class Initialization {

        @Test
        fun `init loads user profile from store`() {
            val vm = makeViewModel(displayName = "Raj", upiId = "raj@upi")
            assertThat(vm.uiState.value.userVpa).isEqualTo("raj@upi")
            assertThat(vm.uiState.value.userDisplayName).isEqualTo("Raj")
        }

        @Test
        fun `init handles missing profile gracefully`() {
            val vm = makeViewModel(displayName = null, upiId = null)
            assertThat(vm.uiState.value.userVpa).isEqualTo("")
            assertThat(vm.uiState.value.userDisplayName).isEqualTo("")
        }
    }

    // ── SetSettlementData ─────────────────────────────────────────────────────

    @Nested
    inner class SetSettlementData {

        private lateinit var vm: SettleViewModel

        @BeforeEach
        fun setUp() {
            vm = makeViewModel()
            vm.setSettlementData(description = "Dinner", splits = testSplits)
        }

        @Test
        fun `populates settlements from splits`() {
            assertThat(vm.uiState.value.settlements).hasSize(3)
        }

        @Test
        fun `totalToCollect sums all non-PAID`() {
            assertThat(vm.uiState.value.totalToCollect).isEqualTo(500.0)
        }

        @Test
        fun `activeDebtsCount counts non-PAID`() {
            assertThat(vm.uiState.value.activeDebtsCount).isEqualTo(3)
        }

        @Test
        fun `settlements have Shared prefix in description`() {
            val descriptions = vm.uiState.value.settlements.map { it.description }
            assertThat(descriptions).containsExactly(
                "Shared: Dinner",
                "Shared: Dinner",
                "Shared: Dinner",
            )
        }

        @Test
        fun `settlements have correct avatar initials`() {
            val initials = vm.uiState.value.settlements.map { it.avatarInitial }
            assertThat(initials).containsExactly("A", "B", "C").inOrder()
        }

        @Test
        fun `valid UPI URIs when user VPA is set`() {
            val uris = vm.uiState.value.settlements.map { it.upiUri }
            uris.forEach { uri ->
                assertThat(uri).startsWith("upi://pay?pa=")
            }
        }

        @Test
        fun `empty UPI URI when user VPA is blank`() {
            val vmNoVpa = makeViewModel(displayName = "User", upiId = "")
            vmNoVpa.setSettlementData(description = "Lunch", splits = testSplits)
            val uris = vmNoVpa.uiState.value.settlements.map { it.upiUri }
            uris.forEach { uri ->
                assertThat(uri).isEmpty()
            }
        }

        @Test
        fun `settlements have share messages containing name, amount, description`() {
            val alice = vm.uiState.value.settlements[0]
            assertThat(alice.shareMessage).contains("Alice")
            assertThat(alice.shareMessage).contains("250.00")
            assertThat(alice.shareMessage).contains("Shared: Dinner")
        }

        @Test
        fun `all start as PENDING`() {
            val statuses = vm.uiState.value.settlements.map { it.status }
            assertThat(statuses).containsExactly(
                SettlementStatus.PENDING,
                SettlementStatus.PENDING,
                SettlementStatus.PENDING,
            )
        }

        @Test
        fun `all start with showQr false`() {
            val showQrValues = vm.uiState.value.settlements.map { it.showQr }
            assertThat(showQrValues).containsExactly(false, false, false)
        }
    }

    // ── ToggleQr ──────────────────────────────────────────────────────────────

    @Nested
    inner class ToggleQr {

        private lateinit var vm: SettleViewModel

        @BeforeEach
        fun setUp() {
            vm = makeViewModel()
            vm.setSettlementData(description = "Dinner", splits = testSplits)
        }

        @Test
        fun `flips showQr for correct index only`() {
            vm.toggleQr(1)
            val settlements = vm.uiState.value.settlements
            assertThat(settlements[0].showQr).isFalse()
            assertThat(settlements[1].showQr).isTrue()
            assertThat(settlements[2].showQr).isFalse()
        }

        @Test
        fun `toggleQr twice returns to false`() {
            vm.toggleQr(0)
            vm.toggleQr(0)
            assertThat(vm.uiState.value.settlements[0].showQr).isFalse()
        }
    }

    // ── MarkPaid ──────────────────────────────────────────────────────────────

    @Nested
    inner class MarkPaid {

        private lateinit var vm: SettleViewModel

        @BeforeEach
        fun setUp() {
            vm = makeViewModel()
            vm.setSettlementData(description = "Dinner", splits = testSplits)
        }

        @Test
        fun `updates status to PAID`() {
            vm.markPaid(0)
            assertThat(vm.uiState.value.settlements[0].status).isEqualTo(SettlementStatus.PAID)
        }

        @Test
        fun `recalculates totalToCollect after marking Alice paid`() {
            // Alice = 250, Bob = 150, Charlie = 100 → total = 500
            // After Alice paid → remaining = 150 + 100 = 250
            vm.markPaid(0)
            assertThat(vm.uiState.value.totalToCollect).isEqualTo(250.0)
        }

        @Test
        fun `recalculates activeDebtsCount`() {
            vm.markPaid(0)
            assertThat(vm.uiState.value.activeDebtsCount).isEqualTo(2)
        }

        @Test
        fun `does not affect other settlements`() {
            vm.markPaid(0)
            val settlements = vm.uiState.value.settlements
            assertThat(settlements[1].status).isEqualTo(SettlementStatus.PENDING)
            assertThat(settlements[2].status).isEqualTo(SettlementStatus.PENDING)
        }
    }

    // ── SettleAll ─────────────────────────────────────────────────────────────

    @Nested
    inner class SettleAll {

        private lateinit var vm: SettleViewModel

        @BeforeEach
        fun setUp() {
            vm = makeViewModel()
            vm.setSettlementData(description = "Dinner", splits = testSplits)
        }

        @Test
        fun `combines non-PAID share messages`() {
            val message = vm.settleAllMessage()
            assertThat(message).contains("Alice")
            assertThat(message).contains("Bob")
            assertThat(message).contains("Charlie")
        }

        @Test
        fun `excludes PAID items from combined message`() {
            vm.markPaid(0) // mark Alice as PAID
            val message = vm.settleAllMessage()
            assertThat(message).doesNotContain("Alice")
            assertThat(message).contains("Bob")
            assertThat(message).contains("Charlie")
        }
    }

    // ── RefreshProfile ───────────────────────────────────────────────────────

    @Nested
    inner class RefreshProfile {

        @Test
        fun `refreshProfile re-reads VPA from store`() {
            val vm = makeViewModel(displayName = "Old", upiId = "old@upi")
            vm.setSettlementData(description = "Dinner", splits = testSplits)

            // Simulate profile edit
            fakeProfile.saveProfile(name = "New", upiId = "new@ybl")
            vm.refreshProfile()

            assertThat(vm.uiState.value.userVpa).isEqualTo("new@ybl")
            assertThat(vm.uiState.value.userDisplayName).isEqualTo("New")
        }

        @Test
        fun `refreshProfile regenerates URIs with new VPA`() {
            val vm = makeViewModel(displayName = "Old", upiId = "old@upi")
            vm.setSettlementData(description = "Dinner", splits = testSplits)

            fakeProfile.saveProfile(name = "New", upiId = "new@ybl")
            vm.refreshProfile()

            vm.uiState.value.settlements.forEach { item ->
                assertThat(item.upiUri).contains("pa=new@ybl")
                assertThat(item.upiUri).doesNotContain("old@upi")
            }
        }

        @Test
        fun `refreshProfile regenerates share messages with new UPI link`() {
            val vm = makeViewModel(displayName = "Old", upiId = "old@upi")
            vm.setSettlementData(description = "Dinner", splits = testSplits)

            fakeProfile.saveProfile(name = "New", upiId = "new@ybl")
            vm.refreshProfile()

            vm.uiState.value.settlements.forEach { item ->
                assertThat(item.shareMessage).contains("new@ybl")
            }
        }

        @Test
        fun `refreshProfile blank VPA produces empty URIs`() {
            val vm = makeViewModel(displayName = "User", upiId = "user@upi")
            vm.setSettlementData(description = "Dinner", splits = testSplits)

            fakeProfile.saveProfile(name = "User", upiId = "")
            // clearProfile sets null, but we want blank — saveProfile with empty works
            fakeProfile.clearProfile()
            vm.refreshProfile()

            vm.uiState.value.settlements.forEach { item ->
                assertThat(item.upiUri).isEmpty()
            }
        }
    }

    // ── QR generation edge cases ─────────────────────────────────────────────

    @Nested
    inner class QrGenerationEdgeCases {

        @Test
        fun `generates URI even when friend has no UPI ID`() {
            val vm = makeViewModel(displayName = "User", upiId = "user@upi")
            vm.setSettlementData(description = "Lunch", splits = testSplitsNoUpi)

            vm.uiState.value.settlements.forEach { item ->
                assertThat(item.upiUri).startsWith("upi://pay?pa=user@upi")
            }
        }

        @Test
        fun `setSettlementData reads fresh VPA not cached init value`() {
            val vm = makeViewModel(displayName = "Old", upiId = "old@upi")

            // Change profile after VM init but before setSettlementData
            fakeProfile.saveProfile(name = "New", upiId = "fresh@upi")
            vm.setSettlementData(description = "Dinner", splits = testSplits)

            vm.uiState.value.settlements.forEach { item ->
                assertThat(item.upiUri).contains("pa=fresh@upi")
            }
        }

        @Test
        fun `empty user VPA produces empty URI regardless of friend UPI`() {
            val vm = makeViewModel(displayName = "User", upiId = "")
            vm.setSettlementData(description = "Lunch", splits = testSplits)

            vm.uiState.value.settlements.forEach { item ->
                assertThat(item.upiUri).isEmpty()
            }
        }
    }
}
