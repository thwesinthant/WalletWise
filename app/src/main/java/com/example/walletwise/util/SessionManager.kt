package com.example.walletwise.util

import android.content.Context

object SessionManager {

    private const val PREFS_NAME = "walletwise_session"

    private const val KEY_USER_ID = "USER_ID"
    private const val KEY_REMEMBER_ME = "REMEMBER_ME"

    // Save the current logged-in user
    fun saveUserSession(
        context: Context,
        userId: Int,
        rememberMe: Boolean
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putInt(KEY_USER_ID, userId)
            .putBoolean(KEY_REMEMBER_ME, rememberMe)
            .apply()
    }

    // Get current logged-in user ID
    fun getUserId(context: Context): Int {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .getInt(KEY_USER_ID, -1)
    }

    // Check whether a current user session exists
    fun isLoggedIn(context: Context): Boolean {
        return getUserId(context) != -1
    }

    // Check whether Remember Me was selected
    fun isRememberMeEnabled(context: Context): Boolean {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .getBoolean(KEY_REMEMBER_ME, false)
    }

    // Clear the entire session
    // This should be called when the user explicitly logs out.
    fun clearSession(context: Context) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .clear()
            .apply()
    }
}