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
import com.kel4.notaly.home.BerandaActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MutasiBarang(
    val tanggal : String,   // untuk sorting
    val nama    : String,
    val keterangan: String, // "Restock dari Supplier X" atau "Terjual - ID Transaksi"
    val masuk   : Int,      // > 0 jika barang masuk, 0 jika keluar
    val keluar  : Int       // > 0 jika barang keluar, 0 jika masuk
)

class BarangLaporanActivity : AppCompatActivity() {

    private lateinit var adapter: LaporanAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan_barang)

        val rv = findViewById<RecyclerView>(R.id.rvLaporan)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = LaporanAdapter(emptyList())
        rv.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {finish()}
        setupTopNav()
        muatDataMutasi()
    }

    private fun muatDataMutasi() {
        lifecycleScope.launch(Dispatchers.IO) {
            val semuaBarang      = db.barangDao().ambilSemuaBarang()
            val semuaBarangMasuk = db.barangMasukDao().riwayatBarangMasuk()
            val semuaDetail      = db.detailPenjualanDao().ambilSemuaDetail()
            val semuaTransaksi   = db.transaksiPenjualanDao().ambilSemuaTransaksi()
            val semuaSupplier    = db.supplierDao().ambilSemuaSupplier()

            val listMutasi = mutableListOf<MutasiBarang>()

            // ── BARANG MASUK: dari tabel barang_masuk (restock) ───────────
            semuaBarangMasuk.forEach { bm ->
                val namaBarang  = semuaBarang.find { it.idBarang == bm.idBarang }?.namaBarang ?: bm.idBarang
                val namaSupplier = semuaSupplier.find { it.idSupplier == bm.idSupplier }?.namaSupplier ?: "Supplier"
                listMutasi.add(
                    MutasiBarang(
                        tanggal    = bm.tanggalMasuk,
                        nama       = namaBarang,
                        keterangan = "Restock · $namaSupplier",
                        masuk      = bm.qtyMasuk,
                        keluar     = 0
                    )
                )
            }

            // ── BARANG KELUAR: dari detail_penjualan ──────────────────────
            semuaDetail.forEach { detail ->
                val namaBarang = semuaBarang.find { it.idBarang == detail.idBarang }?.namaBarang ?: detail.idBarang
                val tanggalTrx = semuaTransaksi.find { it.idTransaksi == detail.idTransaksi }
                    ?.tanggalTransaksi ?: ""
                listMutasi.add(
                    MutasiBarang(
                        tanggal    = tanggalTrx,
                        nama       = namaBarang,
                        keterangan = "Terjual · ${detail.idTransaksi}",
                        masuk      = 0,
                        keluar     = detail.qty
                    )
                )
            }

            // Kelompokkan per tanggal, urutkan DESC
            val grouped = listMutasi
                .groupBy { it.tanggal.take(10).ifEmpty { "Tanpa Tanggal" } }
                .toSortedMap(reverseOrder())

            val listFinal = mutableListOf<Any>()
            grouped.forEach { (tglRaw, items) ->
                val totalMasukHarian  = items.sumOf { it.masuk }
                val totalKeluarHarian = items.sumOf { it.keluar }
                listFinal.add(HeaderTanggal(formatTanggal(tglRaw), 0))
                listFinal.addAll(items)
            }

            withContext(Dispatchers.Main) {
                adapter.updateData(listFinal)
            }
        }
    }

    private fun formatTanggal(raw: String): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val out = java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale("id", "ID"))
            out.format(sdf.parse(raw)!!)
        } catch (e: Exception) { raw }
    }

    private fun setupTopNav() {
        findViewById<TextView>(R.id.nav_keuangan).setOnClickListener {
            val intent = Intent(this, KeuanganLaporanActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION }
            val options = android.app.ActivityOptions.makeCustomAnimation(this, 0, 0)
            startActivity(intent, options.toBundle())
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        findViewById<TextView>(R.id.nav_riwayat).setOnClickListener {
            val intent = Intent(this, RiwayatLaporanActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION }
            val options = android.app.ActivityOptions.makeCustomAnimation(this, 0, 0)
            startActivity(intent, options.toBundle())
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}