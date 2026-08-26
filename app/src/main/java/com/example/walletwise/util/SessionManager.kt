package com.example.walletwise.util

import android.content.Context

object SessionManager {

    private const val PREFS_NAME = "walletwise_session"
    private const val KEY_USER_ID = "USER_ID"

    fun saveUserSession(context: Context, userId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_USER_ID, userId)
            .apply()
    }

    fun getUserId(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_USER_ID, -1)
    }

    fun isLoggedIn(context: Context): Boolean {
        return getUserId(context) != -1
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}