package com.quicksettle.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
class UserProfileStore @Inject constructor(@ApplicationContext context: Context) : UserProfile {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        // Keystore keys corrupted (common on emulators / after backup restore).
        // Delete the corrupted file and recreate.
        Log.w("UserProfileStore", "EncryptedSharedPreferences corrupted, resetting", e)
        context.deleteSharedPreferences(PREFS_FILE)
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /**
     * Persists the user's display name and UPI VPA together as an atomic operation.
     */
    override fun saveProfile(name: String, upiId: String) {
        prefs.edit()
            .putString(KEY_DISPLAY_NAME, name)
            .putString(KEY_UPI_ID, upiId)
            .apply()
    }

    /** Returns the stored display name, or null if not yet set. */
    override fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)

    /** Returns the stored UPI VPA, or null if not yet set. */
    override fun getUpiId(): String? = prefs.getString(KEY_UPI_ID, null)

    /**
     * Returns true only when both a non-blank name and a non-blank UPI ID have been saved.
     */
    override fun isProfileSetup(): Boolean =
        !getDisplayName().isNullOrBlank() && !getUpiId().isNullOrBlank()

    /** Removes all stored profile data. */
    override fun clearProfile() {
        prefs.edit()
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_UPI_ID)
            .apply()
    }

    private companion object {
        const val PREFS_FILE = "quicksettle_user_profile"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_UPI_ID = "upi_id"
    }
}
