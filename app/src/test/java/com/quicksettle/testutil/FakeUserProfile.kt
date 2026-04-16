package com.quicksettle.testutil

import com.quicksettle.data.local.UserProfile

/**
 * In-memory fake implementing [UserProfile] for unit testing.
 *
 * Supports both read and write operations so that ViewModels
 * that call [saveProfile] can be tested without Android Context.
 */
class FakeUserProfile(
    private var displayName: String? = null,
    private var upiId: String? = null,
) : UserProfile {

    override fun getDisplayName(): String? = displayName

    override fun getUpiId(): String? = upiId

    override fun isProfileSetup(): Boolean =
        !displayName.isNullOrBlank() && !upiId.isNullOrBlank()

    override fun saveProfile(name: String, upiId: String) {
        this.displayName = name
        this.upiId = upiId
    }

    override fun clearProfile() {
        displayName = null
        upiId = null
    }
}
