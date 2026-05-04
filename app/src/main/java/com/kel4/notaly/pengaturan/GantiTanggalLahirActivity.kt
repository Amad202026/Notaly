package com.kel4.notaly.pengaturan

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R
import java.util.Calendar

class GantiTanggalLahirActivity : AppCompatActivity() {

    lateinit var btnSimpan : TextView
    lateinit var spHariLahir: Spinner
    lateinit var spBulanLahir: Spinner
    lateinit var spTahunLahir: Spinner
    lateinit var btnBack : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ganti_tanggallahir)

        btnSimpan = findViewById(R.id.btnSimpan)
        spHariLahir = findViewById(R.id.spHariLahir)
        spBulanLahir = findViewById(R.id.spBulanLahir)
        spTahunLahir = findViewById(R.id.spTahunLahir)
        btnBack = findViewById(R.id.btnBack)

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

        btnBack.setOnClickListener {
            startActivity(Intent(this, PengaturanActivity::class.java))
            finish()
        }

        btnSimpan.setOnClickListener {
            val inputHariLahir = spHariLahir.selectedItem.toString()
            val inputBulanLahir = spBulanLahir.selectedItem.toString()
            val inputTahunLahir = spTahunLahir.selectedItem.toString()

            if(inputHariLahir.isEmpty() or inputBulanLahir.isEmpty() or inputTahunLahir.isEmpty()) {
                Toast.makeText(this, "Data ada yang kosong", Toast.LENGTH_SHORT).show()
            } else {
                val sharedPref = getSharedPreferences("DataToko", MODE_PRIVATE)
                sharedPref.edit().putString("HARI_LAHIR", inputHariLahir).apply()
                sharedPref.edit().putString("BULAN_LAHIR", inputBulanLahir).apply()
                sharedPref.edit().putString("TAHUN_LAHIR", inputTahunLahir).apply()
                startActivity(Intent(this, BerhasilTanggalLahirActivity::class.java))
                finish()
            }
        }
    }
}