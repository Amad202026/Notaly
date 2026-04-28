package com.kel4.notaly.laporan

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class KeuanganLaporanActivity : AppCompatActivity() {

    private lateinit var adapter: LaporanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan_keuangan)

        val rv = findViewById<RecyclerView>(R.id.rvKeuangan)
        rv.layoutManager = LinearLayoutManager(this)

        // Menggunakan LaporanAdapter serbaguna
        adapter = LaporanAdapter(emptyList())
        rv.adapter = adapter

        setupTopNav()
        muatDataKeuangan()
    }

    private fun muatDataKeuangan() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@KeuanganLaporanActivity)

            // 1. Ambil Pemasukan (Semua Transaksi Penjualan)
            val listTrx = withContext(Dispatchers.IO) {
                db.transaksiPenjualanDao().ambilSemuaTransaksi()
            }

            // 2. Ambil Pengeluaran (Hitung total Harga Modal dari stok gudang & barang terjual)
            val semuaBarang = withContext(Dispatchers.IO) {
                db.barangDao().ambilSemuaBarang()
            }
            val semuaDetail = withContext(Dispatchers.IO) {
                db.detailPenjualanDao().ambilSemuaDetail()
            }

            // --- PROSES PERHITUNGAN ---
            val uangMasuk = listTrx.sumOf { it.totalBelanja }

            var uangKeluar = 0
            // Tambahkan modal barang yang masih mengendap di gudang
            for (barang in semuaBarang) {
                uangKeluar += (barang.hargaModal * barang.stok)
            }
            // Tambahkan modal barang yang sudah terjual
            for (detail in semuaDetail) {
                val modalItem = semuaBarang.find { it.idBarang == detail.idBarang }?.hargaModal ?: 0
                uangKeluar += (modalItem * detail.qty)
            }

            val saldoAkhir = uangMasuk - uangKeluar

            // --- UPDATE TAMPILAN ---
            val rupiah = NumberFormat.getNumberInstance(Locale("id", "ID"))
            findViewById<TextView>(R.id.tvUangMasuk).text = "Rp ${rupiah.format(uangMasuk)}"
            findViewById<TextView>(R.id.tvUangKeluar).text = "Rp ${rupiah.format(uangKeluar)}"
            findViewById<TextView>(R.id.tvSaldo).text = "Rp ${rupiah.format(saldoAkhir)}"

            // Update isi list di bawah kartu (menggunakan method updateData dari adaptermu)
            adapter.updateData(listTrx)
        }
    }

    private fun setupTopNav() {
        // Tombol Kembali
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Navigasi Tab
        val navBarang  = findViewById<TextView>(R.id.nav_barang)
        val navRiwayat = findViewById<TextView>(R.id.nav_riwayat)

        navBarang.setOnClickListener {
            startActivity(Intent(this, BarangLaporanActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            })
            finish()
        }

        navRiwayat.setOnClickListener {
            startActivity(Intent(this, RiwayatLaporanActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            })
            finish()
        }
    }
}