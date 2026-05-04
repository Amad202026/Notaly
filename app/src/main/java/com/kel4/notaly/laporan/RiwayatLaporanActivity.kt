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
import java.text.SimpleDateFormat
import java.util.Locale

// Satu baris item penjualan per barang per transaksi
data class ItemRiwayat(
    val namaBarang  : String,
    val qty         : Int,
    val hargaJual   : Int,   // subtotal harga jual
    val hargaModal  : Int,   // subtotal harga modal
    val labaBersih  : Int,   // hargaJual - hargaModal
    val idTransaksi : String,
    val statusBayar : String
)

class RiwayatLaporanActivity : AppCompatActivity() {

    private lateinit var adapter: LaporanAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan_riwayat)

        val rv = findViewById<RecyclerView>(R.id.rvLaporan)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = LaporanAdapter(emptyList())
        rv.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            startActivity(Intent(this, BerandaActivity::class.java)); finish()
        }
        setupTopNav()
        muatDataRiwayat()
    }

    private fun muatDataRiwayat() {
        lifecycleScope.launch(Dispatchers.IO) {
            val semuaTransaksi = db.transaksiPenjualanDao().ambilSemuaTransaksi()
            val semuaDetail    = db.detailPenjualanDao().ambilSemuaDetail()
            val semuaBarang    = db.barangDao().ambilSemuaBarang()

            // Kelompokkan transaksi per hari (prefix yyyy-MM-dd), urutkan DESC
            val grouped = semuaTransaksi
                .groupBy { it.tanggalTransaksi.take(10) }
                .toSortedMap(reverseOrder())

            val listFinal = mutableListOf<Any>()

            grouped.forEach { (tglRaw, transaksiList) ->
                val itemsHariIni = mutableListOf<ItemRiwayat>()
                var labaHarian = 0

                transaksiList.forEach { trx ->
                    val details = semuaDetail.filter { it.idTransaksi == trx.idTransaksi }
                    details.forEach { d ->
                        val barang    = semuaBarang.find { it.idBarang == d.idBarang }
                        val nama      = barang?.namaBarang ?: d.idBarang
                        val modal     = (barang?.hargaModal ?: 0) * d.qty
                        val laba      = d.subtotal - modal
                        labaHarian   += laba

                        itemsHariIni.add(
                            ItemRiwayat(
                                namaBarang  = nama,
                                qty         = d.qty,
                                hargaJual   = d.subtotal,
                                hargaModal  = modal,
                                labaBersih  = laba,
                                idTransaksi = trx.idTransaksi,
                                statusBayar = trx.statusPembayaran
                            )
                        )
                    }
                }

                // Tambahkan header hari + ringkasan jumlah transaksi
                val jumlahTrx = transaksiList.size
                listFinal.add(
                    HeaderTanggal(
                        tanggalStr     = formatTanggal(tglRaw),
                        totalLabaHarian = labaHarian,
                        infoTambahan   = "$jumlahTrx transaksi"
                    )
                )
                listFinal.addAll(itemsHariIni)
            }

            withContext(Dispatchers.Main) {
                adapter.updateData(listFinal)
            }
        }
    }

    private fun formatTanggal(raw: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val out = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            out.format(sdf.parse(raw)!!)
        } catch (e: Exception) { raw }
    }

    private fun setupTopNav() {
        findViewById<TextView>(R.id.nav_keuangan).setOnClickListener {
            startActivity(Intent(this, KeuanganLaporanActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            }); finish()
        }
        findViewById<TextView>(R.id.nav_barang).setOnClickListener {
            startActivity(Intent(this, BarangLaporanActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            }); finish()
        }
    }
}