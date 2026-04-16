package com.quicksettle.presentation.screens.profile

import com.google.common.truth.Truth.assertThat
import com.quicksettle.testutil.FakeUserProfile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ProfileViewModelTest {

    private lateinit var fakeProfile: FakeUserProfile

    private fun makeViewModel(
        displayName: String? = "Alice",
        upiId: String? = "alice@upi",
    ): ProfileViewModel {
        fakeProfile = FakeUserProfile(displayName = displayName, upiId = upiId)
        return ProfileViewModel(userProfile = fakeProfile)
    }

    // ── Initialization ───────────────────────────────────────────────────────

    @Nested
    inner class Initialization {

        @Test
        fun `init loads display name from store`() {
            val vm = makeViewModel(displayName = "Alice")
            assertThat(vm.uiState.value.displayName).isEqualTo("Alice")
        }

        @Test
        fun `init loads UPI ID from store`() {
            val vm = makeViewModel(upiId = "alice@upi")
            assertThat(vm.uiState.value.upiId).isEqualTo("alice@upi")
        }

        @Test
        fun `init with null profile defaults to empty strings`() {
            val vm = makeViewModel(displayName = null, upiId = null)
            assertThat(vm.uiState.value.displayName).isEmpty()
            assertThat(vm.uiState.value.upiId).isEmpty()
        }
    }

    // ── Field Changes ────────────────────────────────────────────────────────

    @Nested
    inner class FieldChanges {

        private lateinit var vm: ProfileViewModel

        @BeforeEach
        fun setUp() {
            vm = makeViewModel()
        }

        @Test
        fun `onDisplayNameChange updates state`() {
            vm.onDisplayNameChange("Bob")
            assertThat(vm.uiState.value.displayName).isEqualTo("Bob")
        }

        @Test
        fun `onUpiIdChange updates state`() {
            vm.onUpiIdChange("bob@ybl")
            assertThat(vm.uiState.value.upiId).isEqualTo("bob@ybl")
        }

        @Test
        fun `field change resets saved flag`() {
            vm.saveProfile() // sets saved = true
            assertThat(vm.uiState.value.saved).isTrue()

            vm.onDisplayNameChange("Changed")
            assertThat(vm.uiState.value.saved).isFalse()
        }

        @Test
        fun `upi change also resets saved flag`() {
            vm.saveProfile()
            vm.onUpiIdChange("new@upi")
            assertThat(vm.uiState.value.saved).isFalse()
        }
    }

    // ── Save Profile ─────────────────────────────────────────────────────────

    @Nested
    inner class SaveProfile {

        @Test
        fun `saveProfile persists to store and sets saved flag`() {
            val vm = makeViewModel()
            vm.onDisplayNameChange("NewName")
            vm.onUpiIdChange("new@upi")
            vm.saveProfile()

            assertThat(vm.uiState.value.saved).isTrue()
            assertThat(fakeProfile.getDisplayName()).isEqualTo("NewName")
            assertThat(fakeProfile.getUpiId()).isEqualTo("new@upi")
        }

        @Test
        fun `saveProfile is no-op when name is blank`() {
            val vm = makeViewModel()
            vm.onDisplayNameChange("   ")
            vm.onUpiIdChange("valid@upi")
            vm.saveProfile()

            assertThat(vm.uiState.value.saved).isFalse()
            // Store should still have original values
            assertThat(fakeProfile.getDisplayName()).isEqualTo("Alice")
        }

        @Test
        fun `saveProfile is no-op when UPI ID is blank`() {
            val vm = makeViewModel()
            vm.onDisplayNameChange("ValidName")
            vm.onUpiIdChange("")
            vm.saveProfile()

            assertThat(vm.uiState.value.saved).isFalse()
            assertThat(fakeProfile.getUpiId()).isEqualTo("alice@upi")
        }

        @Test
        fun `saveProfile trims whitespace before saving`() {
            val vm = makeViewModel()
            vm.onDisplayNameChange("  Bob  ")
            vm.onUpiIdChange("  bob@upi  ")
            vm.saveProfile()

            assertThat(fakeProfile.getDisplayName()).isEqualTo("Bob")
            assertThat(fakeProfile.getUpiId()).isEqualTo("bob@upi")
        }
    }
}
