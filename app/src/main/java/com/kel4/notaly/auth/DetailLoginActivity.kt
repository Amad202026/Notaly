package com.kel4.notaly.auth

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R
import android.widget.EditText
import android.widget.Toast
import com.kel4.notaly.home.BerandaActivity
import com.kel4.notaly.kirim.KirimActivity

class DetailLoginActivity : AppCompatActivity() {
    lateinit var etNamaToko: EditText
    lateinit var etHariLahir: EditText
    lateinit var etBulanLahir: EditText
    lateinit var etTahunLahir: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_detail)

        etNamaToko = findViewById(R.id.etNamaToko)

        // etHariLahir = findViewById(R.id.etHariLahir)
        // etBulanLahir = findViewById(R.id.etBulanLahir)
        // etTahunLahir = findViewById(R.id.etTahunLahir)

        findViewById<TextView>(R.id.btnLogin).setOnClickListener {

            val inputNamaToko = etNamaToko.text.toString()

            if (inputNamaToko.isEmpty()) {
                Toast.makeText(this, "Nama toko tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            } else {
                val sharedPref = getSharedPreferences("DataToko", MODE_PRIVATE)
                sharedPref.edit().putString("NAMA_TOKO", inputNamaToko).apply()

                startActivity(Intent(this, BerandaActivity::class.java))
                finish()
            }
        }
    }
}