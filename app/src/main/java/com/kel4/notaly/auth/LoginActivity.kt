package com.kel4.notaly.auth

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<TextView>(R.id.btnLogin).setOnClickListener {
            startActivity(Intent(this, DetailLoginActivity::class.java))
            finish()
        }

        findViewById<TextView>(R.id.tvLupaPin).setOnClickListener {
            startActivity(Intent(this, LupaPinActivity::class.java))
        }
    }
}