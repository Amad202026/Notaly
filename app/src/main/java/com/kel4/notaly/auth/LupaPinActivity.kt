package com.kel4.notaly.auth

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R

class LupaPinActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lupapin)

        findViewById<TextView>(R.id.btnVerifikasi).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            finish()
        }
    }
}