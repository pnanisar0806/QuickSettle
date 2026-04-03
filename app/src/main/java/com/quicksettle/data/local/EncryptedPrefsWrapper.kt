package com.quicksettle.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around EncryptedSharedPreferences for storing the user profile
 * (name, UPI ID) locally and securely.
 */
@Singleton
class EncryptedPrefsWrapper @Inject constructor(@ApplicationContext context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "quicksettle_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userUpiId: String
        get() = prefs.getString(KEY_USER_UPI_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_UPI_ID, value).apply()

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_UPI_ID = "user_upi_id"
    }
}
