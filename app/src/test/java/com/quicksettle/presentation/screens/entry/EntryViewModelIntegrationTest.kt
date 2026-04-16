package com.quicksettle.presentation.screens.entry

import com.google.common.truth.Truth.assertThat
import com.quicksettle.testutil.FakeUserProfile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests the [EntryViewModel] itself — init, wiring of digit/decimal/backspace
 * into state, description changes, and profile save flow.
 *
 * [AmountInputRules] is tested separately in [AmountInputRulesTest].
 */
class EntryViewModelIntegrationTest {

    private lateinit var fakeProfile: FakeUserProfile

    private fun makeViewModel(
        displayName: String? = "Alice",
        upiId: String? = "alice@upi",
    ): EntryViewModel {
        fakeProfile = FakeUserProfile(displayName = displayName, upiId = upiId)
        return EntryViewModel(userProfile = fakeProfile)
    }

    // ── Initialization ───────────────────────────────────────────────────────

    @Nested
    inner class Initialization {

        @Test
        fun `shows onboarding when profile not set up`() {
            val vm = makeViewModel(displayName = null, upiId = null)
            assertThat(vm.uiState.value.showOnboarding).isTrue()
        }

        @Test
        fun `hides onboarding when profile is set up`() {
            val vm = makeViewModel(displayName = "Alice", upiId = "alice@upi")
            assertThat(vm.uiState.value.showOnboarding).isFalse()
        }

        @Test
        fun `loads display name from profile`() {
            val vm = makeViewModel(displayName = "Alice")
            assertThat(vm.uiState.value.displayName).isEqualTo("Alice")
        }

        @Test
        fun `null display name defaults to empty`() {
            val vm = makeViewModel(displayName = null)
            assertThat(vm.uiState.value.displayName).isEmpty()
        }
    }

    // ── Amount Input Wiring ──────────────────────────────────────────────────

    @Nested
    inner class AmountInputWiring {

        private lateinit var vm: EntryViewModel

        @BeforeEach
        fun setUp() {
            vm = makeViewModel()
        }

        @Test
        fun `onDigitPress updates rawAmount`() {
            vm.onDigitPress('5')
            assertThat(vm.uiState.value.rawAmount).isEqualTo("5")
        }

        @Test
        fun `onDecimalPress updates rawAmount`() {
            vm.onDecimalPress()
            assertThat(vm.uiState.value.rawAmount).isEqualTo("0.")
        }

        @Test
        fun `onBackspace removes last character`() {
            vm.onDigitPress('1')
            vm.onDigitPress('2')
            vm.onDigitPress('3')
            vm.onBackspace()
            assertThat(vm.uiState.value.rawAmount).isEqualTo("12")
        }

        @Test
        fun `onBackspace on empty stays empty`() {
            vm.onBackspace()
            assertThat(vm.uiState.value.rawAmount).isEmpty()
        }

        @Test
        fun `onClear resets rawAmount`() {
            vm.onDigitPress('9')
            vm.onDigitPress('9')
            vm.onClear()
            assertThat(vm.uiState.value.rawAmount).isEmpty()
        }
    }

    // ── Description ──────────────────────────────────────────────────────────

    @Nested
    inner class Description {

        @Test
        fun `onDescriptionChange updates state`() {
            val vm = makeViewModel()
            vm.onDescriptionChange("Dinner at Taj")
            assertThat(vm.uiState.value.description).isEqualTo("Dinner at Taj")
        }
    }

    // ── Profile Save ─────────────────────────────────────────────────────────

    @Nested
    inner class ProfileSave {

        @Test
        fun `onProfileSaved saves to store`() {
            val vm = makeViewModel(displayName = null, upiId = null)
            vm.onProfileSaved(name = "Bob", upiId = "bob@upi")

            assertThat(fakeProfile.getDisplayName()).isEqualTo("Bob")
            assertThat(fakeProfile.getUpiId()).isEqualTo("bob@upi")
        }

        @Test
        fun `onProfileSaved hides onboarding`() {
            val vm = makeViewModel(displayName = null, upiId = null)
            assertThat(vm.uiState.value.showOnboarding).isTrue()

            vm.onProfileSaved(name = "Bob", upiId = "bob@upi")
            assertThat(vm.uiState.value.showOnboarding).isFalse()
        }

        @Test
        fun `onProfileSaved updates display name in state`() {
            val vm = makeViewModel(displayName = null, upiId = null)
            vm.onProfileSaved(name = "Bob", upiId = "bob@upi")
            assertThat(vm.uiState.value.displayName).isEqualTo("Bob")
        }
    }
}
