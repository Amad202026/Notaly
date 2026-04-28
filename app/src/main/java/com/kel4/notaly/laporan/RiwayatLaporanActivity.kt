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
import java.text.SimpleDateFormat
import java.util.*

// --- DATA CLASS KHUSUS RIWAYAT ---
data class HeaderTanggal(val tanggalStr: String, val totalLabaHarian: Int)

data class ItemRiwayat(
    val namaBarang: String,
    val qty: Int,
    val hargaJual: Int,   // Total pendapatan dari penjualan item ini
    val hargaModal: Int,  // Total modal yang dikeluarkan untuk item ini
    val labaBersih: Int   // Selisih
)

class RiwayatLaporanActivity : AppCompatActivity() {

    private lateinit var rvLaporan: RecyclerView
    private lateinit var adapter: LaporanAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan_riwayat)

        rvLaporan = findViewById(R.id.rvLaporan)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        setupTopNav()
        setupRecyclerView()
        muatDataLaporan()
    }

    private fun setupRecyclerView() {
        rvLaporan.layoutManager = LinearLayoutManager(this)
        // Panggil adapter serbaguna kita
        adapter = LaporanAdapter(emptyList())
        rvLaporan.adapter = adapter
    }

    private fun muatDataLaporan() {
        lifecycleScope.launch {
            val dataLengkap = mutableListOf<Any>()

            withContext(Dispatchers.IO) {
                val semuaTransaksi = db.transaksiPenjualanDao().ambilSemuaTransaksi()
                val semuaDetail = db.detailPenjualanDao().ambilSemuaDetail()
                val semuaBarang = db.barangDao().ambilSemuaBarang()

                // Kelompokkan transaksi berdasarkan tanggal (YYYY-MM-DD)
                val groups = semuaTransaksi.groupBy { it.tanggalTransaksi.substring(0, 10) }

                // Urutkan dari tanggal terbaru ke terlama
                val sortedGroups = groups.toSortedMap(reverseOrder())

                sortedGroups.forEach { (tanggalRaw, transaksiList) ->
                    var totalLabaHariIni = 0
                    val daftarItemHariIni = mutableListOf<ItemRiwayat>()

                    transaksiList.forEach { trx ->
                        // Cari detail untuk transaksi ini
                        val details = semuaDetail.filter { it.idTransaksi == trx.idTransaksi }

                        details.forEach { d ->
                            // Cari modal barang
                            val barang = semuaBarang.find { it.idBarang == d.idBarang }
                            val nama = barang?.namaBarang ?: d.idBarang
                            val modalSatuan = barang?.hargaModal ?: 0

                            val hargaJualTotal = d.subtotal
                            val hargaModalTotal = modalSatuan * d.qty
                            val labaItem = hargaJualTotal - hargaModalTotal

                            totalLabaHariIni += labaItem

                            daftarItemHariIni.add(
                                ItemRiwayat(
                                    namaBarang = nama,
                                    qty = d.qty,
                                    hargaJual = hargaJualTotal,
                                    hargaModal = hargaModalTotal,
                                    labaBersih = labaItem
                                )
                            )
                        }
                    }

                    // 1. Masukkan Header Tanggal & Total Laba ke dalam list
                    val tanggalCantik = formatTanggalHeader(tanggalRaw)
                    dataLengkap.add(HeaderTanggal(tanggalCantik, totalLabaHariIni))

                    // 2. Masukkan semua rincian barang di hari tersebut ke dalam list
                    dataLengkap.addAll(daftarItemHariIni)
                }
            }

            withContext(Dispatchers.Main) {
                adapter.updateData(dataLengkap)
            }
        }
    }

    private fun formatTanggalHeader(raw: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(raw)
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(date!!)
        } catch (e: Exception) { raw }
    }

    private fun setupTopNav() {
        findViewById<TextView>(R.id.nav_keuangan).setOnClickListener {
            startActivity(Intent(this, KeuanganLaporanActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
            finish()
        }
        findViewById<TextView>(R.id.nav_barang).setOnClickListener {
            startActivity(Intent(this, BarangLaporanActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
            finish()
        }
    }
}