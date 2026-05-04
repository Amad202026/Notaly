package com.kel4.notaly.home

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R
import com.kel4.notaly.barang.BarangActivity
import com.kel4.notaly.cacat.CacatActivity
import com.kel4.notaly.daftransaksi.DafTransaksiActivity
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.kategori.KategoriActivity
import com.kel4.notaly.laporan.KeuanganLaporanActivity
import com.kel4.notaly.pelanggan.PelangganActivity
import com.kel4.notaly.pengaturan.PengaturanActivity
import com.kel4.notaly.pengiriman.PengirimanActivity
import com.kel4.notaly.restock.StockActivity
import com.kel4.notaly.supplier.SupplierActivity
import com.kel4.notaly.transaksi.TransaksiActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BerandaActivity : AppCompatActivity() {

    private lateinit var tvNamaToko: TextView
    private lateinit var tvTanggal: TextView
    private lateinit var tvPendapatan: TextView
    private lateinit var tvBarangTerjual: TextView
    private lateinit var tvJumlahStokMenipis: TextView
    private lateinit var layoutStokMenipis: LinearLayout
    private lateinit var layoutNotifikasi: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beranda)

        initViews()
        loadNamaToko()
        setTanggalHariIni()
        setupMenuListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data setiap kali beranda tampil kembali (misal setelah balik dari menu lain)
        loadDashboardData()
    }

    private fun initViews() {
        tvNamaToko          = findViewById(R.id.tvNamaToko)
        tvTanggal           = findViewById(R.id.tvTanggal)
        tvPendapatan        = findViewById(R.id.tvPendapatan)
        tvBarangTerjual     = findViewById(R.id.tvBarangTerjual)
        tvJumlahStokMenipis = findViewById(R.id.tvJumlahStokMenipis)
        layoutStokMenipis   = findViewById(R.id.layoutStokMenipis)
        layoutNotifikasi    = findViewById(R.id.layoutNotifikasi)
    }

    private fun loadNamaToko() {
        val sharedPref = getSharedPreferences("DataToko", MODE_PRIVATE)
        tvNamaToko.text = sharedPref.getString("NAMA_TOKO", "Notaly Store")
    }

    private fun setTanggalHariIni() {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        tvTanggal.text = sdf.format(Date()).uppercase()
    }

    private fun loadDashboardData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@BerandaActivity)

            // Tanggal hari ini sebagai prefix untuk filter
            val hariIni = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // ── 1. Pendapatan & barang terjual hari ini ────────────────────
            val semuaTransaksi = db.transaksiPenjualanDao().ambilSemuaTransaksi()

            val transaksiLunasHariIni = semuaTransaksi.filter { t ->
                t.tanggalTransaksi.startsWith(hariIni) && t.statusPembayaran == "Lunas"
            }
            val pendapatanHariIni = transaksiLunasHariIni.sumOf { it.totalBelanja }

            val semuaDetail = db.detailPenjualanDao().ambilSemuaDetail()
            val idLunasHariIni = transaksiLunasHariIni.map { it.idTransaksi }.toSet()
            val barangTerjualHariIni = semuaDetail
                .filter { it.idTransaksi in idLunasHariIni }
                .sumOf { it.qty }

            // ── 2. Stok menipis ────────────────────────────────────────────
            val semuaBarang = db.barangDao().ambilSemuaBarang()
            val prefEkstra  = getSharedPreferences("DataEkstraBarang", MODE_PRIVATE)
            val barangMenipis = semuaBarang.filter { barang ->
                val stokMin = prefEkstra.getInt("STOKMIN_${barang.idBarang}", 0)
                stokMin > 0 && barang.stok <= stokMin
            }

            // ── 3. Transaksi berstatus DP ──────────────────────────────────
            val transaksiDp = semuaTransaksi.filter { it.statusPembayaran == "DP" }

            // ── 4. Pengiriman belum dikirim (status "Diproses") ─────────────
            val pengirimanMenunggu = db.pengirimanDao().ambilPengirimanMenunggu()

            // ── Render UI ──────────────────────────────────────────────────
            withContext(Dispatchers.Main) {
                tampilkanPendapatan(pendapatanHariIni, barangTerjualHariIni)
                tampilkanStokMenipis(barangMenipis.map { it.namaBarang to it.stok })
                tampilkanNotifikasi(transaksiDp.size, pengirimanMenunggu.size)
            }
        }
    }

    // ── Render: pendapatan ─────────────────────────────────────────────────────

    private fun tampilkanPendapatan(jumlah: Int, itemTerjual: Int) {
        val formatRupiah = NumberFormat.getNumberInstance(Locale("id", "ID"))
        tvPendapatan.text    = "Rp ${formatRupiah.format(jumlah)}"
        tvBarangTerjual.text = "$itemTerjual Item"
    }

    // ── Render: stok menipis ───────────────────────────────────────────────────

    private fun tampilkanStokMenipis(daftar: List<Pair<String, Int>>) {
        tvJumlahStokMenipis.text = "${daftar.size} ITEM"
        layoutStokMenipis.removeAllViews()

        if (daftar.isEmpty()) {
            val tv = TextView(this).apply {
                text      = "Semua stok aman ✓"
                textSize  = 12f
                setTextColor(0xFF888888.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dp(4) }
            }
            layoutStokMenipis.addView(tv)
            return
        }

        daftar.forEach { (nama, stok) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dp(8) }
            }

            val tvNama = TextView(this).apply {
                text     = "• $nama"
                textSize = 12.5f
                setTextColor(0xFF444444.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvStok = TextView(this).apply {
                text     = "Sisa $stok"
                textSize = 12f
                setTextColor(0xFFB45309.toInt())
                typeface = Typeface.DEFAULT_BOLD
            }

            row.addView(tvNama)
            row.addView(tvStok)
            layoutStokMenipis.addView(row)
        }
    }

    // ── Render: notifikasi ────────────────────────────────────────────────────

    private fun tampilkanNotifikasi(jumlahDp: Int, jumlahPengiriman: Int) {
        layoutNotifikasi.removeAllViews()

        val adaNotif = jumlahDp > 0 || jumlahPengiriman > 0
        layoutNotifikasi.visibility = if (adaNotif) View.VISIBLE else View.GONE
        if (!adaNotif) return

        if (jumlahDp > 0) {
            layoutNotifikasi.addView(
                buatItemNotifikasi(
                    emoji     = "💳",
                    pesan     = "$jumlahDp transaksi masih berstatus DP",
                    warnaBg   = 0xFFFFF3CD.toInt(),
                    warnaText = 0xFF856404.toInt()
                )
            )
        }

        if (jumlahPengiriman > 0) {
            layoutNotifikasi.addView(
                buatItemNotifikasi(
                    emoji     = "📦",
                    pesan     = "$jumlahPengiriman pengiriman belum dikirim",
                    warnaBg   = 0xFFD1ECF1.toInt(),
                    warnaText = 0xFF0C5460.toInt()
                )
            )
        }
    }

    private fun buatItemNotifikasi(
        emoji: String,
        pesan: String,
        warnaBg: Int,
        warnaText: Int
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            setBackgroundColor(warnaBg)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(6) }
        }

        val tvEmoji = TextView(this).apply {
            text     = emoji
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginEnd = dp(10) }
        }

        val tvPesan = TextView(this).apply {
            text     = pesan
            textSize = 12.5f
            setTextColor(warnaText)
            typeface = Typeface.DEFAULT_BOLD
        }

        row.addView(tvEmoji)
        row.addView(tvPesan)
        return row
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    // ── Menu listeners ────────────────────────────────────────────────────────

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