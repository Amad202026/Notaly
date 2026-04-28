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

// 1. DATA CLASS DIGABUNG DI SINI SAJA (Tidak perlu file baru)
data class MutasiBarang(
    val nama: String,
    val masuk: Int,
    val keluar: Int,
    val sisa: Int
)

class BarangLaporanActivity : AppCompatActivity() {

    private lateinit var rvBarang: RecyclerView
    private lateinit var adapter: LaporanAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan_barang) // Pastikan nama XML-nya sesuai

        rvBarang = findViewById(R.id.rvLaporan) // ID RecyclerView di XML
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        setupTopNav()
        setupRecyclerView()
        muatDataMutasi()
    }

    private fun setupRecyclerView() {
        rvBarang.layoutManager = LinearLayoutManager(this)
        // Kita menggunakan Adapter serbaguna yang sudah dibuat sebelumnya
        adapter = LaporanAdapter(emptyList())
        rvBarang.adapter = adapter
    }

    private fun muatDataMutasi() {
        lifecycleScope.launch {
            val listMutasi = mutableListOf<Any>()

            withContext(Dispatchers.IO) {
                // Ambil semua barang dari gudang
                val semuaBarang = db.barangDao().ambilSemuaBarang()
                // Ambil semua detail transaksi yang pernah terjadi
                val semuaDetailTerjual = db.detailPenjualanDao().ambilSemuaDetail() // Pastikan fungsi ini ada di DAO kamu

                // Proses setiap barang
                for (barang in semuaBarang) {
                    // Hitung total barang ini yang sudah terjual
                    val totalKeluar = semuaDetailTerjual
                        .filter { it.idBarang == barang.idBarang }
                        .sumOf { it.qty }

                    val sisaSekarang = barang.stok

                    // Trik Logika Sederhana:
                    // Jika Sisa = Total Masuk - Total Keluar
                    // Maka Total Masuk = Sisa + Total Keluar
                    val totalMasuk = sisaSekarang + totalKeluar

                    // Masukkan ke dalam data class lokal kita
                    listMutasi.add(
                        MutasiBarang(
                            nama = barang.namaBarang,
                            masuk = totalMasuk,
                            keluar = totalKeluar,
                            sisa = sisaSekarang
                        )
                    )
                }
            }

            withContext(Dispatchers.Main) {
                // Urutkan berdasarkan barang yang paling banyak terjual
                val dataSorted = listMutasi.sortedByDescending { (it as MutasiBarang).keluar }
                adapter.updateData(dataSorted)
            }
        }
    }

    private fun setupTopNav() {
        val navKeuangan = findViewById<TextView>(R.id.nav_keuangan)
        val navRiwayat  = findViewById<TextView>(R.id.nav_riwayat)

        navKeuangan.setOnClickListener {
            startActivity(Intent(this, KeuanganLaporanActivity::class.java).apply {
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