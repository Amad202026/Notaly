package com.kel4.notaly.auth

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R

class LupaPinActivity : AppCompatActivity() {

    lateinit var etNamaToko: EditText
    lateinit var etHariLahir: EditText
    lateinit var etBulanLahir: EditText
    lateinit var etTahunLahir: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lupapin)

        etNamaToko = findViewById(R.id.etNamaToko)
        etHariLahir = findViewById(R.id.etHariLahir)
        etBulanLahir = findViewById(R.id.etBulanLahir)
        etTahunLahir = findViewById(R.id.etTahunLahir)

        val sharedPref = getSharedPreferences("DataToko", MODE_PRIVATE)
        val namaToko = sharedPref.getString("NAMA_TOKO", "")
        val hariLahir = sharedPref.getString("HARI_LAHIR", "")
        val bulanLahir = sharedPref.getString("BULAN_LAHIR", "")
        val tahunLahir = sharedPref.getString("TAHUN_LAHIR", "")

        findViewById<TextView>(R.id.btnVerifikasi).setOnClickListener {
            val inputNamaToko = etNamaToko.text.toString()
            val inputHariLahir = etHariLahir.text.toString()
            val inputBulanLahir = etBulanLahir.text.toString()
            val inputTahunLahir = etTahunLahir.text.toString()

            if (inputNamaToko == namaToko && inputHariLahir == hariLahir && inputBulanLahir == bulanLahir && inputTahunLahir == tahunLahir){ 
                startActivity(Intent(this, RegisterActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Input data anda salah", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            finish()
        }
    }
}