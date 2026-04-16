package com.quicksettle.data.local

interface UserProfile {
    fun getDisplayName(): String?
    fun getUpiId(): String?
    fun isProfileSetup(): Boolean
    fun saveProfile(name: String, upiId: String)
    fun clearProfile()
}
