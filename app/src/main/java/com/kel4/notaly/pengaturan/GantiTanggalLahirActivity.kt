package com.kel4.notaly.pengaturan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R

class GantiTanggalLahirActivity : AppCompatActivity() {

    lateinit var btnSimpan : Button
    lateinit var etHariLahir : EditText
    lateinit var etBulanLahir : EditText
    lateinit var etTahunLahir : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ganti_tanggallahir)

        btnSimpan = findViewById(R.id.btnSimpan)
        etHariLahir = findViewById(R.id.etHariLahir)
        etBulanLahir = findViewById(R.id.etBulanLahir)
        etTahunLahir = findViewById(R.id.etTahunLahir)

        btnSimpan.setOnClickListener {
            val inputHariLahir = etHariLahir.text.toString()
            val inputBulanLahir = etBulanLahir.text.toString()
            val inputTahunLahir = etTahunLahir.text.toString()

            if(inputHariLahir.isEmpty() or inputBulanLahir.isEmpty() or inputTahunLahir.isEmpty()) {
                Toast.makeText(this, "Data ada yang kosong", Toast.LENGTH_SHORT).show()
            } else {
                val sharedPref = getSharedPreferences("DataToko", MODE_PRIVATE)
                sharedPref.edit().putString("HARI_LAHIR", inputHariLahir).apply()
                sharedPref.edit().putString("BULAN_LAHIR", inputBulanLahir).apply()
                sharedPref.edit().putString("TAHUN_LAHIR", inputTahunLahir).apply()
                startActivity(Intent(this, BerhasilNamatokoActivity::class.java))
                finish()
            }
        }
    }
}