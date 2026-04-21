package com.kel4.notaly.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kel4.notaly.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        lateinit var btnGetStarted: Button

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnGetStarted = findViewById(R.id.btnGetStarted)
        val userPref = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        var isReg = userPref.getBoolean("IS_REG", false)

        btnGetStarted.setOnClickListener {

            if(isReg) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } else{
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}