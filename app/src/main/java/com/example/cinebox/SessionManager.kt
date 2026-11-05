package com.example.cinebox

import android.content.Context

class SessionManager(context: Context) {
    private val pref = context.getSharedPreferences("CineSession", Context.MODE_PRIVATE)

    fun save(token: String, username: String, remember: Boolean) {
        pref.edit()
            .putString("token", token)
            .putString("username", username)
            .putBoolean("remember", remember)
            .apply()
    }

    val token: String get() = pref.getString("token", "") ?: ""
    val username: String get() = pref.getString("username", "") ?: ""

    fun shouldAutoLogin(): Boolean = pref.getBoolean("remember", false) && token.isNotEmpty()

    fun clear() {
        pref.edit().clear().apply()
    }
}