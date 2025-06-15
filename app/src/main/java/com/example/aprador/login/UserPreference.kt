package com.example.aprador.login

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class UserPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHOTO_URL = "user_photo_url"
        private const val KEY_USER_ID = "user_id"
    }

    fun setUserLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit() { putBoolean(KEY_IS_LOGGED_IN, isLoggedIn) }
    }

    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun saveUserData(name: String, email: String, photoUrl: String?, userId: String) {
        with(sharedPreferences.edit()) {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_PHOTO_URL, photoUrl)
            putString(KEY_USER_ID, userId)
            apply()
        }
    }

    fun getUserName(): String {
        return sharedPreferences.getString(KEY_USER_NAME, "User") ?: "User"
    }

    fun getUserEmail(): String {
        return sharedPreferences.getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun getUserPhotoUrl(): String? {
        return sharedPreferences.getString(KEY_USER_PHOTO_URL, null)
    }

    fun getUserId(): String {
        return sharedPreferences.getString(KEY_USER_ID, "") ?: ""
    }

    fun clearUserData() {
        sharedPreferences.edit() { clear() }
    }
}