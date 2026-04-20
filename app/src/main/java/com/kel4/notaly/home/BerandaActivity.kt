package com.kel4.notaly.home

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.kel4.notaly.barang.BarangActivity
import com.kel4.notaly.kategori.KategoriActivity
import com.kel4.notaly.pelanggan.PelangganActivity
import com.kel4.notaly.supplier.SupplierActivity
import com.kel4.notaly.R
import com.kel4.notaly.kirim.KirimActivity
import com.kel4.notaly.pengaturan.PengaturanActivity
import com.kel4.notaly.restock.StockActivity

class BerandaActivity : AppCompatActivity() {

    lateinit var tvNamaToko: TextView
    lateinit var tvTanggal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beranda)

        tvNamaToko = findViewById(R.id.tvNamaToko)
        tvTanggal = findViewById(R.id.tvTanggal)

        val sharedPref = getSharedPreferences("DataToko", MODE_PRIVATE)
        val namaToko = sharedPref.getString("NAMA_TOKO", "")
        tvNamaToko.text = namaToko

        findViewById<CardView>(R.id.menuDaftarSupplier).setOnClickListener {
            startActivity(Intent(this, SupplierActivity::class.java))
        }
        findViewById<CardView>(R.id.menuKategoriBarang).setOnClickListener {
            startActivity(Intent(this, KategoriActivity::class.java))
        }
        findViewById<CardView>(R.id.menuDaftarBarang).setOnClickListener {
            startActivity(Intent(this, BarangActivity::class.java))
        }
        findViewById<CardView>(R.id.menuDaftarPelanggan).setOnClickListener {
            startActivity(Intent(this, PelangganActivity::class.java))
        }
        findViewById<CardView>(R.id.menuRestock).setOnClickListener {
            startActivity(Intent(this, StockActivity::class.java))
        }
        findViewById<CardView>(R.id.menuKirimBarang).setOnClickListener {
            startActivity(Intent(this, KirimActivity::class.java))
        }
        findViewById<CardView>(R.id.menuPengaturan).setOnClickListener {
            startActivity(Intent(this, PengaturanActivity::class.java))
        }
    }
}