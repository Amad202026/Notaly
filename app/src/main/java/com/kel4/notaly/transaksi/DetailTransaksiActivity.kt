package com.kel4.notaly.transaksi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.model.Barang
import com.kel4.notaly.model.DetailPenjualan
import com.kel4.notaly.model.TransaksiPenjualan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DetailTransaksiActivity : AppCompatActivity() {

    // ===================== DAO =====================
    private lateinit var db: AppDatabase

    // ===================== VIEW =====================
    private lateinit var btnBack         : ImageView
    private lateinit var tvIdTransaksi   : TextView
    private lateinit var tvStatusTransaksi: TextView
    private lateinit var tvTanggal       : TextView
    private lateinit var rvProduk        : RecyclerView
    private lateinit var tvSubtotal      : TextView
    private lateinit var tvDiskon        : TextView
    private lateinit var tvPajak         : TextView
    private lateinit var tvLaba          : TextView
    private lateinit var tvTotalBelanja  : TextView
    private lateinit var tvStatusBadge   : TextView
    private lateinit var btnBagikan      : Button
    private lateinit var btnCetak        : Button

    // ===================== FORMAT =====================
    private val rupiahFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaksi_detail)

        db = AppDatabase.getDatabase(this)

        bindView()

        val idTransaksi = intent.getStringExtra("ID_TRANSAKSI") ?: run {
            Toast.makeText(this, "ID Transaksi tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        muatData(idTransaksi)
    }

    // ─────────────────────────────────────────────────────────
    //  BIND VIEW
    // ─────────────────────────────────────────────────────────
    private fun bindView() {
        btnBack          = findViewById(R.id.btnBack)
        tvIdTransaksi    = findViewById(R.id.tvIdTransaksi)
        tvStatusTransaksi = findViewById(R.id.tvStatusTransaksi)
        tvTanggal        = findViewById(R.id.tvTanggalTransaksi)
        rvProduk         = findViewById(R.id.rvProduk)
        tvSubtotal       = findViewById(R.id.tvSubtotal)
        tvDiskon         = findViewById(R.id.tvDiskon)
        tvPajak          = findViewById(R.id.tvPajak)
        tvLaba           = findViewById(R.id.tvLaba)
        tvTotalBelanja   = findViewById(R.id.tvTotalBelanja)
        tvStatusBadge    = findViewById(R.id.tvStatusBadge)
        btnBagikan       = findViewById(R.id.btnBagikan)
        btnCetak         = findViewById(R.id.btnCetakStruk)

        btnBack.setOnClickListener { finish() }

        rvProduk.layoutManager = LinearLayoutManager(this)
    }

    // ─────────────────────────────────────────────────────────
    //  MUAT DATA TRANSAKSI
    // ─────────────────────────────────────────────────────────
    private fun muatData(idTransaksi: String) {
        lifecycleScope.launch {
            val transaksi: TransaksiPenjualan?
            val listDetail: List<DetailPenjualan>
            val mapBarang : Map<String, Barang>

            withContext(Dispatchers.IO) {
                transaksi   = db.transaksiPenjualanDao().cariTransaksiById(idTransaksi)
                listDetail  = db.detailPenjualanDao().ambilStrukTransaksi(idTransaksi)
                val barangList = db.barangDao().ambilSemuaBarang()
                mapBarang   = barangList.associateBy { it.idBarang }
            }

            if (transaksi == null) {
                Toast.makeText(this@DetailTransaksiActivity, "Data transaksi tidak ditemukan", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            tampilkanData(transaksi, listDetail, mapBarang)
        }
    }

    // ─────────────────────────────────────────────────────────
    //  TAMPILKAN DATA KE UI
    // ─────────────────────────────────────────────────────────
    private fun tampilkanData(
        transaksi: TransaksiPenjualan,
        listDetail: List<DetailPenjualan>,
        mapBarang: Map<String, Barang>
    ) {
        // ── ID Transaksi ──
        tvIdTransaksi.text = transaksi.idTransaksi

        // ── Status ──
        val statusLabel = if (transaksi.statusPembayaran == "Lunas") "Berhasil Diselesaikan" else "DP / Belum Lunas"
        tvStatusTransaksi.text = statusLabel
        tvStatusBadge.text     = if (transaksi.statusPembayaran == "Lunas") "PAID FULL" else "DP"

        // ── Tanggal ──
        tvTanggal.text = formatTanggal(transaksi.tanggalTransaksi)

        // ── RecyclerView produk ──
        val adapter = AdapterProdukDetail(listDetail, mapBarang)
        rvProduk.adapter = adapter

        // ── Hitung ringkasan biaya ──
        val subtotal     = listDetail.sumOf { it.subtotal }
        val diskon       = transaksi.totalDiskon ?: 0
        val pajak        = (subtotal * 0.11).toInt()    // PPN 11%
        val totalBelanja = transaksi.totalBelanja

        // Laba = subtotal (harga jual) - total harga modal
        val laba = listDetail.sumOf { d ->
            val b = mapBarang[d.idBarang]
            val hargaJual = d.hargaNego ?: d.hargaSatuan
            (hargaJual - (b?.hargaModal ?: 0)) * d.qty
        }

        tvSubtotal.text    = "Rp ${rupiahFormat.format(subtotal)}"
        tvDiskon.text      = "Rp ${rupiahFormat.format(diskon)}"
        tvPajak.text       = "Rp ${rupiahFormat.format(pajak)}"
        tvLaba.text        = "Rp ${rupiahFormat.format(laba)}"
        tvTotalBelanja.text = "Rp ${rupiahFormat.format(totalBelanja)}"

        // ── Tombol Bagikan ──
        btnBagikan.setOnClickListener {
            bagikanStruk(transaksi, listDetail, mapBarang)
        }

        // ── Tombol Cetak ──
        btnCetak.setOnClickListener {
            Toast.makeText(this, "Fitur cetak struk belum tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────────────────────
    //  BAGIKAN STRUK (Share Intent)
    // ─────────────────────────────────────────────────────────
    private fun bagikanStruk(
        transaksi: TransaksiPenjualan,
        listDetail: List<DetailPenjualan>,
        mapBarang: Map<String, Barang>
    ) {
        val sb = StringBuilder()
        sb.appendLine("===== STRUK TRANSAKSI =====")
        sb.appendLine("ID    : ${transaksi.idTransaksi}")
        sb.appendLine("Tgl   : ${formatTanggal(transaksi.tanggalTransaksi)}")
        sb.appendLine("Metode: ${transaksi.metode ?: "-"}")
        sb.appendLine("Status: ${transaksi.statusPembayaran}")
        sb.appendLine("---------------------------")
        listDetail.forEach { d ->
            val nama = mapBarang[d.idBarang]?.namaBarang ?: d.idBarang
            val harga = d.hargaNego ?: d.hargaSatuan
            sb.appendLine("$nama")
            sb.appendLine("  ${d.qty} x Rp ${rupiahFormat.format(harga)} = Rp ${rupiahFormat.format(d.subtotal)}")
        }
        sb.appendLine("---------------------------")
        sb.appendLine("TOTAL : Rp ${rupiahFormat.format(transaksi.totalBelanja)}")
        sb.appendLine("===========================")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(intent, "Bagikan Struk via"))
    }

    // ─────────────────────────────────────────────────────────
    //  FORMAT TANGGAL
    // ─────────────────────────────────────────────────────────
    private fun formatTanggal(raw: String): String {
        return try {
            val inFormat  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outFormat = SimpleDateFormat("dd MMMM yyyy • HH:mm 'WIB'", Locale("id", "ID"))
            val date = inFormat.parse(raw)
            outFormat.format(date ?: Date())
        } catch (e: Exception) {
            raw
        }
    }
}