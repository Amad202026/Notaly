package com.kel4.notaly.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R
import java.util.Calendar

class LupaPinActivity : AppCompatActivity() {

    lateinit var etNamaToko: EditText
    lateinit var spHariLahir: Spinner
    lateinit var spBulanLahir: Spinner
    lateinit var spTahunLahir: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lupapin)

        etNamaToko = findViewById(R.id.etNamaToko)
        spHariLahir = findViewById(R.id.spHariLahir)
        spBulanLahir = findViewById(R.id.spBulanLahir)
        spTahunLahir = findViewById(R.id.spTahunLahir)

        val tahunSekarang = Calendar.getInstance().get(Calendar.YEAR)

        val listHari = (1..31).toList()
        val listBulan = (1..12).toList()
        val listTahun = (1900..tahunSekarang).toList().reversed()

        val adapterHari = ArrayAdapter(this, android.R.layout.simple_spinner_item, listHari)
        adapterHari.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spHariLahir.adapter = adapterHari
        val adapterBulan = ArrayAdapter(this, android.R.layout.simple_spinner_item, listBulan)
        adapterBulan.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spBulanLahir.adapter = adapterBulan
        val adapterTahun = ArrayAdapter(this, android.R.layout.simple_spinner_item, listTahun)
        adapterTahun.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTahunLahir.adapter = adapterTahun

        val sharedPref = getSharedPreferences("DataToko", MODE_PRIVATE)
        val namaToko = sharedPref.getString("NAMA_TOKO", "")
        val hariLahir = sharedPref.getString("HARI_LAHIR", "")
        val bulanLahir = sharedPref.getString("BULAN_LAHIR", "")
        val tahunLahir = sharedPref.getString("TAHUN_LAHIR", "")

        findViewById<TextView>(R.id.btnVerifikasi).setOnClickListener {
            val inputNamaToko = etNamaToko.text.toString()
            val inputHariLahir = spHariLahir.selectedItem.toString()
            val inputBulanLahir = spBulanLahir.selectedItem.toString()
            val inputTahunLahir = spTahunLahir.selectedItem.toString()

            if (inputNamaToko == namaToko && inputHariLahir == hariLahir && inputBulanLahir == bulanLahir && inputTahunLahir == tahunLahir){
                getSharedPreferences("DataToko",Context.MODE_PRIVATE).edit().clear().apply()
                getSharedPreferences("UserPreferences",Context.MODE_PRIVATE).edit().clear().apply()
                startActivity(Intent(this, RegisterActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Input data anda salah", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}