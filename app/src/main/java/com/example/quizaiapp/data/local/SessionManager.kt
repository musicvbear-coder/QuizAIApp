package com.example.quizaiapp.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "quiz_ai_user_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
    }

    fun taoPhienDangNhap(userId: Int, name: String, email: String) {
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putInt(KEY_USER_ID, userId)
        editor.putString(KEY_USER_NAME, name)
        editor.putString(KEY_USER_EMAIL, email)
        editor.apply()
    }

    fun isDaDangNhap(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun layTenNguoiDung(): String {
        return prefs.getString(KEY_USER_NAME, "Người dùng") ?: "Người dùng"
    }

    fun layEmailNguoiDung(): String {
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun layUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun dangXuat() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}