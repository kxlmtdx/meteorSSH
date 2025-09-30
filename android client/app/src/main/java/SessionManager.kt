package com.example.meteorssh

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("MeteorSession", Context.MODE_PRIVATE)
    private val editor = prefs.edit()

    fun createLoginSession(user: com.example.meteorssh.User) {
        editor.putInt("user_id", user.id)
        editor.putString("username", user.username)
        editor.putString("email", user.email)
        editor.putBoolean("logged_in", true)
        editor.apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean("logged_in", false)

    fun getUser(): com.example.meteorssh.User? {
        if (!isLoggedIn()) return null
        return com.example.meteorssh.User(
            id = prefs.getInt("user_id", -1),
            username = prefs.getString("username", "") ?: "",
            email = prefs.getString("email", "") ?: ""
        )
    }

    fun logout() {
        editor.clear().apply()
    }

    fun getUserId(): Int = prefs.getInt("user_id", -1)
}