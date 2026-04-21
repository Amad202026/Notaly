package com.kel4.notaly.pengaturan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kel4.notaly.R

class GantiPinActivity : AppCompatActivity() {

    lateinit var etPinLama: EditText
    lateinit var etPinBaru: EditText
    lateinit var btnSimpan: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ganti_pin)

        etPinLama = findViewById(R.id.etPinLama)
        etPinBaru = findViewById(R.id.etPinBaru)
        btnSimpan = findViewById(R.id.btnSimpan)

        val sharedPref = getSharedPreferences("DataToko", MODE_PRIVATE)
        val pinLama = sharedPref.getString("PIN", "")

        btnSimpan.setOnClickListener {
            val inputPinLama = etPinLama.text.toString()
            val inputPinBaru = etPinBaru.text.toString()

            if (inputPinLama.isEmpty() or inputPinBaru.isEmpty()) {
                Toast.makeText(this, "Pin tidak boleh kosong", Toast.LENGTH_SHORT).show()
            } else if (inputPinLama != pinLama) {
                Toast.makeText(this, "Pin lama salah", Toast.LENGTH_SHORT).show()
            } else {
                val sharedPref = getSharedPreferences("DataToko", MODE_PRIVATE)
                sharedPref.edit().putString("PIN", inputPinBaru).apply()
                startActivity(Intent(this, BerhasilPinActivity::class.java))
                finish()
            }
        }
    }
}