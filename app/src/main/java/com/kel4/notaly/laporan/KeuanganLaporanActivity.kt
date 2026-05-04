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
import java.text.NumberFormat
import java.util.Locale

class KeuanganLaporanActivity : AppCompatActivity() {

    private lateinit var adapter: LaporanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan_keuangan)

        val rv = findViewById<RecyclerView>(R.id.rvKeuangan)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = LaporanAdapter(emptyList())
        rv.adapter = adapter

        setupTopNav()
        muatDataKeuangan()
    }

    private fun muatDataKeuangan() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@KeuanganLaporanActivity)

            // ── Data mentah ───────────────────────────────────────────────
            val semuaTransaksi  = db.transaksiPenjualanDao().ambilSemuaTransaksi()
            val semuaBarangMasuk = db.barangMasukDao().riwayatBarangMasuk()
            val semuaPengiriman = db.pengirimanDao().getAllPengiriman()
            val semuaBarang     = db.barangDao().ambilSemuaBarang()
            val semuaDetail     = db.detailPenjualanDao().ambilSemuaDetail()

            // ── UANG MASUK: total pendapatan penjualan (semua status) ──────
            val totalMasuk = semuaTransaksi.sumOf { it.totalBelanja.toLong() }

            // ── UANG KELUAR: 3 sumber ────────────────────────────────────
            // 1. Pembelian stok restock (harga beli × qty)
            val keluarRestock = semuaBarangMasuk.sumOf { (it.hargaBeli * it.qtyMasuk).toLong() }

            // 2. Biaya pengiriman yang sudah dicatat
            val keluarPengiriman = semuaPengiriman
                .sumOf { (it.biayaKirim ?: 0.0).toLong() }

            // 3. Modal barang yang terjual (harga modal × qty terjual per item)
            val prefEkstra = getSharedPreferences("DataEkstraBarang", MODE_PRIVATE)
            val keluarModalTerjual = semuaDetail.sumOf { detail ->
                val modal = semuaBarang.find { it.idBarang == detail.idBarang }?.hargaModal ?: 0
                (modal * detail.qty).toLong()
            }
            val totalKeluar = keluarRestock + keluarPengiriman + keluarModalTerjual
            val saldoAkhir  = totalMasuk - totalKeluar

            // ── Bangun list item untuk RecyclerView ───────────────────────
            // Gabungkan semua transaksi keuangan lalu urutkan per tanggal DESC
            val semuaItem = mutableListOf<ItemKeuangan>()

            // Pemasukan dari penjualan
            semuaTransaksi.forEach { trx ->
                semuaItem.add(
                    ItemKeuangan(
                        tanggal  = trx.tanggalTransaksi,
                        judul    = trx.idTransaksi,
                        detail   = "Penjualan · ${trx.statusPembayaran} · ${trx.metode ?: "-"}",
                        nominal  = trx.totalBelanja.toLong(),
                        jenisPos = true   // true = masuk
                    )
                )
            }

            // Pengeluaran restock
            semuaBarangMasuk.forEach { bm ->
                val namaBarang = semuaBarang.find { it.idBarang == bm.idBarang }?.namaBarang ?: bm.idBarang
                semuaItem.add(
                    ItemKeuangan(
                        tanggal  = bm.tanggalMasuk,
                        judul    = "Restock · $namaBarang",
                        detail   = "${bm.qtyMasuk} pcs × Rp ${NumberFormat.getNumberInstance(Locale("id","ID")).format(bm.hargaBeli.toLong())}",
                        nominal  = (bm.hargaBeli * bm.qtyMasuk).toLong(),
                        jenisPos = false
                    )
                )
            }

            // Pengeluaran biaya pengiriman
            semuaPengiriman.filter { (it.biayaKirim ?: 0.0) > 0 }.forEach { pg ->
                semuaItem.add(
                    ItemKeuangan(
                        tanggal  = "",          // Pengiriman tidak punya field tanggal sendiri
                        judul    = "Ongkir · ${pg.idTransaksi ?: "-"}",
                        detail   = "${pg.namaEkspedisi ?: "Ekspedisi"} · ${pg.statusKirim ?: "-"}",
                        nominal  = (pg.biayaKirim ?: 0.0).toLong(),
                        jenisPos = false
                    )
                )
            }

            // Kelompokkan per tanggal (ambil prefix yyyy-MM-dd), urutkan DESC
            val grouped = semuaItem
                .groupBy { it.tanggal.take(10).ifEmpty { "Tanpa Tanggal" } }
                .toSortedMap(reverseOrder())

            val listFinal = mutableListOf<Any>()
            grouped.forEach { (tglRaw, items) ->
                val totalHarian = items.sumOf { if (it.jenisPos) it.nominal else -it.nominal }
                listFinal.add(HeaderTanggal(formatTanggal(tglRaw), totalHarian.toInt()))
                listFinal.addAll(items)
            }

            withContext(Dispatchers.Main) {
                val rupiah = NumberFormat.getNumberInstance(Locale("id", "ID"))
                findViewById<TextView>(R.id.tvUangMasuk).text  = "Rp ${rupiah.format(totalMasuk)}"
                findViewById<TextView>(R.id.tvUangKeluar).text = "Rp ${rupiah.format(totalKeluar)}"
                findViewById<TextView>(R.id.tvSaldo).text      = "Rp ${rupiah.format(saldoAkhir)}"
                adapter.updateData(listFinal)
            }
        }
    }

    private fun formatTanggal(raw: String): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val out = java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            out.format(sdf.parse(raw)!!)
        } catch (e: Exception) { raw }
    }

    private fun setupTopNav() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            startActivity(Intent(this, BerandaActivity::class.java)); finish()
        }
        findViewById<TextView>(R.id.nav_barang).setOnClickListener {
            startActivity(Intent(this, BarangLaporanActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            }); finish()
        }
        findViewById<TextView>(R.id.nav_riwayat).setOnClickListener {
            startActivity(Intent(this, RiwayatLaporanActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            }); finish()
        }
    }
}