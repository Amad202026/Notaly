package com.kel4.notaly.home

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.kel4.notaly.R
import com.kel4.notaly.barang.BarangActivity
import com.kel4.notaly.cacat.CacatActivity
import com.kel4.notaly.daftransaksi.DafTransaksiActivity
import com.kel4.notaly.kategori.KategoriActivity
import com.kel4.notaly.laporan.KeuanganLaporanActivity
import com.kel4.notaly.pelanggan.PelangganActivity
import com.kel4.notaly.pengaturan.PengaturanActivity
import com.kel4.notaly.pengiriman.PengirimanActivity
import com.kel4.notaly.restock.StockActivity
import com.kel4.notaly.supplier.SupplierActivity
import com.kel4.notaly.transaksi.TransaksiActivity

class BerandaActivity : AppCompatActivity() {


    private lateinit var tvNamaToko: TextView
    private lateinit var tvTanggal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beranda)

        // Initialize Header Views
        tvNamaToko = findViewById(R.id.tvNamaToko)
        tvTanggal = findViewById(R.id.tvTanggal)

        // Load Store Name from SharedPreferences
        val sharedPref = getSharedPreferences("DataToko", MODE_PRIVATE)
        tvNamaToko.text = sharedPref.getString("NAMA_TOKO", "Notaly Store")

        // Setup Menu Click Listeners
        setupMenuListeners()
    }

    private fun setupMenuListeners() {
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
        findViewById<CardView>(R.id.menuDaftarSupplier).setOnClickListener {
            startActivity(Intent(this, SupplierActivity::class.java))
        }
        findViewById<CardView>(R.id.menuTransaksi).setOnClickListener {
            startActivity(Intent(this, TransaksiActivity::class.java))
        }
        findViewById<CardView>(R.id.menuDaftarTransaksi).setOnClickListener {
            startActivity(Intent(this, DafTransaksiActivity::class.java))
        }
        findViewById<CardView>(R.id.menuKirimBarang).setOnClickListener {
            startActivity(Intent(this, PengirimanActivity::class.java))
        }
        findViewById<CardView>(R.id.menuBarangCacat).setOnClickListener {
            startActivity(Intent(this, CacatActivity::class.java))
        }
        findViewById<CardView>(R.id.menuPengaturan).setOnClickListener {
            startActivity(Intent(this, PengaturanActivity::class.java))
        }
        findViewById<CardView>(R.id.menuLaporan).setOnClickListener {
            startActivity(Intent(this, KeuanganLaporanActivity::class.java))
        }
    }
}