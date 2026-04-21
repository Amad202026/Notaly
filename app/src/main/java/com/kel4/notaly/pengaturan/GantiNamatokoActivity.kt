package com.kel4.notaly.pengaturan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R
import com.kel4.notaly.home.BerandaActivity

class GantiNamatokoActivity : AppCompatActivity() {

    lateinit var etNamaToko: EditText
    lateinit var btnSimpan: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ganti_namatoko)

        btnSimpan = findViewById(R.id.btnSimpan)
        etNamaToko = findViewById(R.id.etNamaToko)

        btnSimpan.setOnClickListener{
            val inputNamaToko = etNamaToko.text.toString()

            if(inputNamaToko.isEmpty()) {
                Toast.makeText(this, "Nama toko kosong", Toast.LENGTH_SHORT).show()
            } else {
                val sharedPref = getSharedPreferences("DataToko", MODE_PRIVATE)
                sharedPref.edit().putString("NAMA_TOKO", inputNamaToko).apply()
                startActivity(Intent(this, BerhasilNamatokoActivity::class.java))
                finish()
            }
        }
    }
}