package com.kel4.notaly.auth

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R

class RegisterActivity : AppCompatActivity() {

    lateinit var etPin: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etPin = findViewById(R.id.etPin)

        findViewById<TextView>(R.id.btnRegister).setOnClickListener {

            val inputPin = etPin.text.toString()

            if (inputPin.isEmpty()) {
                Toast.makeText(this, "PIN anda belum diisi", Toast.LENGTH_SHORT).show()
            } else {
                val sharedPref = getSharedPreferences("DataToko", MODE_PRIVATE)
                sharedPref.edit().putString("PIN", inputPin).apply()

                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }
}