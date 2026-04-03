package com.quicksettle.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure local storage for the user's own profile (display name and UPI VPA).
 *
 * Uses EncryptedSharedPreferences backed by AES256-GCM so that plaintext credentials
 * are never written to disk.
 */
@Singleton
class UserProfileStore @Inject constructor(@ApplicationContext context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "quicksettle_user_profile",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    /**
     * Persists the user's display name and UPI VPA together as an atomic operation.
     */
    fun saveProfile(name: String, upiId: String) {
        prefs.edit()
            .putString(KEY_DISPLAY_NAME, name)
            .putString(KEY_UPI_ID, upiId)
            .apply()
    }

    /** Returns the stored display name, or null if not yet set. */
    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)

    /** Returns the stored UPI VPA, or null if not yet set. */
    fun getUpiId(): String? = prefs.getString(KEY_UPI_ID, null)

    /**
     * Returns true only when both a non-blank name and a non-blank UPI ID have been saved.
     */
    fun isProfileSetup(): Boolean =
        !getDisplayName().isNullOrBlank() && !getUpiId().isNullOrBlank()

    /** Removes all stored profile data. */
    fun clearProfile() {
        prefs.edit()
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_UPI_ID)
            .apply()
    }

    private companion object {
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_UPI_ID = "upi_id"
    }
}
