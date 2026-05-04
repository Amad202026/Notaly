package com.kel4.notaly.auth

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.kel4.notaly.home.BerandaActivity
import java.util.Calendar

class DetailLoginActivity : AppCompatActivity() {
    lateinit var etNamaToko: EditText
    lateinit var spHariLahir: Spinner
    lateinit var spBulanLahir: Spinner
    lateinit var spTahunLahir: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_detail)

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

        findViewById<TextView>(R.id.btnLogin).setOnClickListener {
            val inputNamaToko = etNamaToko.text.toString()
            val inputHariLahir = spHariLahir.selectedItem.toString()
            val inputBulanLahir = spBulanLahir.selectedItem.toString()
            val inputTahunLahir = spTahunLahir.selectedItem.toString()

            if (inputNamaToko.isEmpty() || inputHariLahir.isEmpty() || inputBulanLahir.isEmpty() || inputTahunLahir.isEmpty()) {
                Toast.makeText(this, "Data tidak boleh ada yang kosong!", Toast.LENGTH_SHORT).show()
            } else {
                val userPref = getSharedPreferences("UserPreferences", MODE_PRIVATE)
                userPref.edit().putBoolean("IS_REG", true).apply()
                val sharedPref = getSharedPreferences("DataToko", MODE_PRIVATE)
                sharedPref.edit().putString("NAMA_TOKO", inputNamaToko).apply()
                sharedPref.edit().putString("HARI_LAHIR", inputHariLahir).apply()
                sharedPref.edit().putString("BULAN_LAHIR", inputBulanLahir).apply()
                sharedPref.edit().putString("TAHUN_LAHIR", inputTahunLahir).apply()

                startActivity(Intent(this, BerandaActivity::class.java))
                finish()
            }
        }
    }
}