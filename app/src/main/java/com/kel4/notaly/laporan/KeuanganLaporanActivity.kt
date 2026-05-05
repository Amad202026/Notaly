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

            // ── UANG KELUAR ───────────────────────────────────────────────
            // 1. Pembelian stok restock (kulakan)
            val keluarRestock = semuaBarangMasuk.sumOf { (it.hargaBeli * it.qtyMasuk).toLong() }
            // 2. Biaya pengiriman ekspedisi
            val keluarPengiriman = semuaPengiriman.sumOf { (it.biayaKirim ?: 0.0).toLong() }
            val totalKeluar = keluarRestock + keluarPengiriman

            // ── Bangun list item & Hitung Uang Masuk ──────────────────────
            val semuaItem = mutableListOf<ItemKeuangan>()
            var totalMasuk = 0L // Variabel uang masuk kita siapkan di sini

            val spDiskon = getSharedPreferences("DataDiskonTransaksi", MODE_PRIVATE)

            semuaTransaksi.forEach { trx ->
                val pelangganNama = spDiskon.getString("PELANGGAN_NAMA_${trx.idTransaksi}", "Umum")
                val diskonPersen  = spDiskon.getInt("DISKON_PERSEN_${trx.idTransaksi}", 0)

                // 🔥 1. Tarik nominal DP dari memori
                val nominalDp     = spDiskon.getInt("DP_NOMINAL_${trx.idTransaksi}", 0)

                // 🔥 2. Logika Uang Riil (Jika DP, ambil nominal DP. Jika Lunas, ambil Total Belanja)
                val uangDiterima = if (trx.statusPembayaran == "DP" && nominalDp > 0) {
                    nominalDp.toLong()
                } else {
                    trx.totalBelanja.toLong()
                }

                // Tambahkan uang yang benar-benar diterima ke Total Pemasukan
                totalMasuk += uangDiterima

                val infoDiskon = if (diskonPersen > 0) " · Diskon $diskonPersen%" else ""
                val detailTeks = "Penjualan ($pelangganNama) · ${trx.statusPembayaran}$infoDiskon"

                semuaItem.add(
                    ItemKeuangan(
                        tanggal  = trx.tanggalTransaksi,
                        judul    = trx.idTransaksi,
                        detail   = detailTeks,
                        nominal  = uangDiterima, // 🔥 Tampilkan angka yang benar di daftar
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

            // ── BUKA MEMORI TANGGAL PENGIRIMAN ──
            val spPengiriman = getSharedPreferences("DataPengirimanEkstra", MODE_PRIVATE)

            // Pengeluaran biaya pengiriman
            semuaPengiriman.filter { (it.biayaKirim ?: 0.0) > 0 }.forEach { pg ->

                // 1. Tarik tanggal dari SharedPreferences
                val savedTanggalKirim = spPengiriman.getString("TGL_KIRIM_${pg.idTransaksi}", "") ?: ""

                // 2. Jika kosong (mungkin data lama), pinjam tanggal dari data Transaksinya
                val finalTanggal = if (savedTanggalKirim.isNotEmpty()) {
                    savedTanggalKirim
                } else {
                    semuaTransaksi.find { it.idTransaksi == pg.idTransaksi }?.tanggalTransaksi ?: ""
                }

                semuaItem.add(
                    ItemKeuangan(
                        tanggal  = finalTanggal, // 🔥 TANGGAL SUDAH TIDAK KOSONG
                        judul    = "Ongkir · ${pg.idTransaksi ?: "-"}",
                        detail   = "${pg.namaEkspedisi ?: "Ekspedisi"} · ${pg.statusKirim ?: "-"}",
                        nominal  = (pg.biayaKirim ?: 0.0).toLong(),
                        jenisPos = false
                    )
                )
            }

            // Kelompokkan per tanggal
            val grouped = semuaItem
                .groupBy { it.tanggal.take(10).ifEmpty { "Tanpa Tanggal" } }
                .toSortedMap(reverseOrder())

            val listFinal = mutableListOf<Any>()
            grouped.forEach { (tglRaw, items) ->
                val totalHarian = items.sumOf { if (it.jenisPos) it.nominal else -it.nominal }
                listFinal.add(HeaderTanggal(formatTanggal(tglRaw), totalHarian.toInt()))
                listFinal.addAll(items)
            }

            // Saldo akhir dihitung setelah totalMasuk selesai dijumlahkan
            val saldoAkhir  = totalMasuk - totalKeluar

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
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {finish()}
        findViewById<TextView>(R.id.nav_barang).setOnClickListener {
            val intent = Intent(this, BarangLaporanActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION }
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