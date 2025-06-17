package com.example.aprador.login

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class UserProfile(
    val userId: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
    val localPhotoPath: String?,
    val gender: String,
    val isLoggedIn: Boolean,
    val lastUpdated: Long = System.currentTimeMillis()
)

class UserPreferences(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHOTO_URL = "user_photo_url"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_LOCAL_PHOTO_PATH = "local_photo_path"
        private const val KEY_USER_GENDER = "user_gender"
    }

    fun setUserLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_IS_LOGGED_IN, isLoggedIn) }
    }

    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun saveUserData(name: String, email: String, photoUrl: String?, userId: String) {
        // Load existing profile data from JSON to preserve profile picture and gender
        val existingProfile = loadUserProfileFromJson(userId)

        with(sharedPreferences.edit()) {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_PHOTO_URL, photoUrl)
            putString(KEY_USER_ID, userId)

            // Restore profile picture and gender from existing profile if available
            existingProfile?.let { profile ->
                if (!profile.localPhotoPath.isNullOrEmpty()) {
                    putString(KEY_LOCAL_PHOTO_PATH, profile.localPhotoPath)
                }
                putString(KEY_USER_GENDER, profile.gender)
            }

            apply()
        }

        // Save to JSON database with preserved data
        saveUserProfileToJson(
            UserProfile(
                userId = userId,
                name = name,
                email = email,
                photoUrl = photoUrl,
                localPhotoPath = existingProfile?.localPhotoPath ?: getLocalPhotoPath(),
                gender = existingProfile?.gender ?: getUserGender(),
                isLoggedIn = true
            )
        )
    }

    fun getUserName(): String {
        return sharedPreferences.getString(KEY_USER_NAME, "User") ?: "User"
    }

    fun getUserEmail(): String {
        return sharedPreferences.getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun getUserPhotoUrl(): String? {
        // First check for local photo path, then fallback to original photo URL
        val localPhotoPath = getLocalPhotoPath()
        return if (!localPhotoPath.isNullOrEmpty()) {
            localPhotoPath
        } else {
            sharedPreferences.getString(KEY_USER_PHOTO_URL, null)
        }
    }

    fun getUserId(): String {
        return sharedPreferences.getString(KEY_USER_ID, "") ?: ""
    }

    fun saveLocalPhotoPath(photoPath: String) {
        sharedPreferences.edit {
            putString(KEY_LOCAL_PHOTO_PATH, photoPath)
        }
        // Update JSON database
        updateUserProfileInJson()
    }

    fun getLocalPhotoPath(): String? {
        return sharedPreferences.getString(KEY_LOCAL_PHOTO_PATH, null)
    }

    fun clearLocalPhotoPath() {
        sharedPreferences.edit {
            remove(KEY_LOCAL_PHOTO_PATH)
        }
        // Update JSON database
        updateUserProfileInJson()
    }

    fun getOriginalPhotoUrl(): String? {
        return sharedPreferences.getString(KEY_USER_PHOTO_URL, null)
    }

    fun saveUserGender(gender: String) {
        sharedPreferences.edit {
            putString(KEY_USER_GENDER, gender)
        }
        // Update JSON database
        updateUserProfileInJson()
    }

    fun getUserGender(): String {
        return sharedPreferences.getString(KEY_USER_GENDER, "Male") ?: "Male"
    }

    fun clearUserData() {
        // Save current profile data to JSON before clearing SharedPreferences
        updateUserProfileInJson()

        // Only clear SharedPreferences, keep JSON data for future logins
        sharedPreferences.edit {
            // Clear login session data but keep the profile data in JSON
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_PHOTO_URL)
            remove(KEY_USER_ID)
            // Don't remove local photo path and gender - they should persist
            // remove(KEY_LOCAL_PHOTO_PATH)
            // remove(KEY_USER_GENDER)
        }
    }

    private fun saveUserProfileToJson(userProfile: UserProfile) {
        val file = File(context.filesDir, "user_profiles.json")

        // Read existing profiles
        val profiles: MutableList<UserProfile> = if (file.exists() && file.readText().isNotBlank()) {
            val json = file.readText()
            val type = object : TypeToken<MutableList<UserProfile>>() {}.type
            try {
                Gson().fromJson(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        // Remove existing profile for this user if it exists
        profiles.removeAll { it.userId == userProfile.userId }

        // Add updated profile with isLoggedIn = false for logout
        profiles.add(userProfile.copy(isLoggedIn = false))

        // Write updated list back to file
        val updatedJson = Gson().toJson(profiles)
        file.writeText(updatedJson)
    }

    private fun updateUserProfileInJson() {
        val currentProfile = UserProfile(
            userId = getUserId(),
            name = getUserName(),
            email = getUserEmail(),
            photoUrl = sharedPreferences.getString(KEY_USER_PHOTO_URL, null),
            localPhotoPath = getLocalPhotoPath(),
            gender = getUserGender(),
            isLoggedIn = isUserLoggedIn()
        )
        saveUserProfileToJson(currentProfile)
    }

    // Updated to accept userId parameter for loading specific user profile
    private fun loadUserProfileFromJson(userId: String = getUserId()): UserProfile? {
        val file = File(context.filesDir, "user_profiles.json")

        if (file.exists() && file.readText().isNotBlank()) {
            val json = file.readText()
            val type = object : TypeToken<List<UserProfile>>() {}.type
            try {
                val profiles: List<UserProfile> = Gson().fromJson(json, type) ?: emptyList()
                return profiles.find { it.userId == userId }
            } catch (e: Exception) {
                // Handle JSON parsing error
                return null
            }
        }
        return null
    }

    // New method to restore user profile data from JSON when logging in
    fun restoreUserProfileFromJson(userId: String) {
        val profile = loadUserProfileFromJson(userId)
        profile?.let {
            sharedPreferences.edit {
                putString(KEY_LOCAL_PHOTO_PATH, it.localPhotoPath)
                putString(KEY_USER_GENDER, it.gender)
            }
        }
    }
}