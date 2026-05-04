package com.kel4.notaly.transaksi

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.home.BerandaActivity
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

    // View Baru untuk DP
    private lateinit var llInfoDp        : LinearLayout
    private lateinit var tvDpDibayar     : TextView
    private lateinit var tvSisaTagihan   : TextView
    private lateinit var btnLunasi       : TextView

    private lateinit var btnBagikan      : TextView
    private lateinit var btnCetak        : TextView

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

        // Bind View DP & Pelunasan
        llInfoDp         = findViewById(R.id.llInfoDp)
        tvDpDibayar      = findViewById(R.id.tvDpDibayar)
        tvSisaTagihan    = findViewById(R.id.tvSisaTagihan)
        btnLunasi        = findViewById(R.id.btnLunasi)

        btnBagikan       = findViewById(R.id.btnBagikan)
        btnCetak         = findViewById(R.id.btnCetakStruk)

        btnBack.setOnClickListener {
            finish()
        }

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
        tvIdTransaksi.text = transaksi.idTransaksi
        tvTanggal.text = formatTanggal(transaksi.tanggalTransaksi)

        val adapter = AdapterProdukDetail(listDetail, mapBarang)
        rvProduk.adapter = adapter

        // Hitung ringkasan biaya
        val subtotal     = listDetail.sumOf { it.subtotal }
        val diskon       = transaksi.totalDiskon ?: 0
        val pajak        = (subtotal * 0.11).toInt()    // PPN 11%
        val totalBelanja = transaksi.totalBelanja

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

        // ── LOGIKA TAMPILAN STATUS (DP / LUNAS) ──
        if (transaksi.statusPembayaran == "DP") {
            tvStatusTransaksi.text = "DP / Belum Lunas"
            tvStatusBadge.text = "DP"
            tvStatusTransaksi.setTextColor(android.graphics.Color.parseColor("#B45309")) // Warna orange peringatan
            tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#B45309"))

            // Tangkap nilai DP dari Intent yang dilempar dari TransaksiActivity
            val nominalDp = intent.getIntExtra("NOMINAL_DP", 0)
            val sisaTagihan = totalBelanja - nominalDp

            // Tampilkan UI DP
            llInfoDp.visibility = View.VISIBLE
            btnLunasi.visibility = View.VISIBLE

            tvDpDibayar.text = "Rp ${rupiahFormat.format(nominalDp)}"
            tvSisaTagihan.text = "Rp ${rupiahFormat.format(sisaTagihan)}"

            // Aksi Tombol Lunasi Tagihan
            btnLunasi.setOnClickListener {
                konfirmasiPelunasan(transaksi)
            }

        } else {
            tvStatusTransaksi.text = "Berhasil Diselesaikan"
            tvStatusBadge.text = "PAID FULL"
            tvStatusTransaksi.setTextColor(android.graphics.Color.parseColor("#0A3D26"))
            tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#0D5C3A"))

            // Sembunyikan UI DP karena sudah Lunas
            llInfoDp.visibility = View.GONE
            btnLunasi.visibility = View.GONE
        }

        // Tombol Bagikan & Cetak
        btnBagikan.setOnClickListener {
            bagikanStruk(transaksi, listDetail, mapBarang)
        }
        btnCetak.setOnClickListener {
            Toast.makeText(this, "Fitur cetak struk belum tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────────────────────
    //  PROSES PELUNASAN DP
    // ─────────────────────────────────────────────────────────
    private fun konfirmasiPelunasan(transaksi: TransaksiPenjualan) {
        AlertDialog.Builder(this)
            .setTitle("Lunasi Tagihan")
            .setMessage("Apakah pelanggan sudah melunasi sisa tagihan? Status transaksi akan diubah menjadi Lunas.")
            .setPositiveButton("Ya, Lunasi") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        // Membuat salinan transaksi dengan status yang sudah diperbarui
                        val transaksiLunas = transaksi.copy(statusPembayaran = "Lunas")

                        // Perintah update ke database
                        db.transaksiPenjualanDao().updateTransaksi(transaksiLunas)
                    }

                    Toast.makeText(this@DetailTransaksiActivity, "Pembayaran berhasil dilunasi!", Toast.LENGTH_SHORT).show()

                    // Segarkan halaman agar UI langsung berubah menjadi Lunas
                    muatData(transaksi.idTransaksi)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
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