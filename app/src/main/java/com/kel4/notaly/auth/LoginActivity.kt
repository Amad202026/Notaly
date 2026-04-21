package com.kel4.notaly.auth

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R
import com.kel4.notaly.home.BerandaActivity

class LoginActivity : AppCompatActivity() {

    lateinit var etPin : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etPin = findViewById(R.id.etPin)
        val sharedPref = getSharedPreferences("DataToko", MODE_PRIVATE)
        val pinTadi = sharedPref.getString("PIN", "")
        val userPref = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        var isReg = userPref.getBoolean("IS_REG", false)

        findViewById<TextView>(R.id.btnLogin).setOnClickListener {
            val inputPin = etPin.text.toString()

            if(inputPin.isEmpty()){
                Toast.makeText(this, "PIN anda belum diisi", Toast.LENGTH_SHORT).show()
            } else if(inputPin != pinTadi){
                Toast.makeText(this, "PIN anda salah", Toast.LENGTH_SHORT).show()
            } else if(isReg) {
                startActivity(Intent(this, BerandaActivity::class.java))
                finish()
            } else {
                startActivity(Intent(this, DetailLoginActivity::class.java))
                finish()
            }
        }

        findViewById<TextView>(R.id.tvLupaPin).setOnClickListener {
            startActivity(Intent(this, LupaPinActivity::class.java))
        }
    }
}