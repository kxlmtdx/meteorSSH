package com.example.meteorssh

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.meteorssh.R
import com.example.meteorssh.SessionManager

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val sessionManager = SessionManager(this)
        val user = sessionManager.getUser()

        findViewById<TextView>(R.id.tvProfileName).text = user?.username ?: "Guest"
        findViewById<TextView>(R.id.tvProfileEmail).text = user?.email ?: ""

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            sessionManager.logout()
            finish()
            startActivity(packageManager.getLaunchIntentForPackage(packageName))
        }
    }
}