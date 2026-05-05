package com.kel4.notaly.transaksi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.kel4.notaly.model.Barang
import com.kel4.notaly.model.DetailPenjualan
import com.kel4.notaly.model.Pelanggan
import com.kel4.notaly.model.TransaksiPenjualan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DetailTransaksiActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    // ===================== VIEW =====================
    private lateinit var btnBack:              ImageView
    private lateinit var tvIdTransaksi:        TextView
    private lateinit var tvStatusTransaksi:    TextView
    private lateinit var tvTanggal:            TextView
    private lateinit var rvProduk:             RecyclerView
    private lateinit var tvSubtotal:           TextView
    private lateinit var tvDiskon:             TextView
    private lateinit var tvLaba:               TextView
    private lateinit var tvTotalBelanja:       TextView
    private lateinit var tvStatusBadge:        TextView

    // Pelanggan
    private lateinit var llInfoPelanggan:      LinearLayout
    private lateinit var tvNamaPelanggan:      TextView
    private lateinit var tvKategoriPelanggan:  TextView

    // Diskon pelanggan di ringkasan
    private lateinit var llDiskonPelanggan:    LinearLayout
    private lateinit var tvDiskonPelangganNominal: TextView
    private lateinit var tvDiskonPelangganPersen: TextView

    // DP
    private lateinit var llInfoDp:             LinearLayout
    private lateinit var tvDpDibayar:          TextView
    private lateinit var tvSisaTagihan:        TextView
    private lateinit var btnLunasi:            TextView

    private lateinit var btnBagikan:           TextView
    private lateinit var btnCetak:             TextView

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

    private fun bindView() {
        btnBack               = findViewById(R.id.btnBack)
        tvIdTransaksi         = findViewById(R.id.tvIdTransaksi)
        tvStatusTransaksi     = findViewById(R.id.tvStatusTransaksi)
        tvTanggal             = findViewById(R.id.tvTanggalTransaksi)
        rvProduk              = findViewById(R.id.rvProduk)
        tvSubtotal            = findViewById(R.id.tvSubtotal)
        tvDiskon              = findViewById(R.id.tvDiskon)
        tvLaba                = findViewById(R.id.tvLaba)
        tvTotalBelanja        = findViewById(R.id.tvTotalBelanja)
        tvStatusBadge         = findViewById(R.id.tvStatusBadge)

        llInfoPelanggan       = findViewById(R.id.llInfoPelanggan)
        tvNamaPelanggan       = findViewById(R.id.tvNamaPelanggan)
        tvKategoriPelanggan   = findViewById(R.id.tvKategoriPelanggan)

        llDiskonPelanggan     = findViewById(R.id.llDiskonPelanggan)
        tvDiskonPelangganNominal = findViewById(R.id.tvDiskonPelangganNominal)
        tvDiskonPelangganPersen  = findViewById(R.id.tvDiskonPelangganPersen)

        llInfoDp              = findViewById(R.id.llInfoDp)
        tvDpDibayar           = findViewById(R.id.tvDpDibayar)
        tvSisaTagihan         = findViewById(R.id.tvSisaTagihan)
        btnLunasi             = findViewById(R.id.btnLunasi)
        btnBagikan            = findViewById(R.id.btnBagikan)
        btnCetak              = findViewById(R.id.btnCetakStruk)

        btnBack.setOnClickListener { finish() }
        rvProduk.layoutManager = LinearLayoutManager(this)
    }

    private fun muatData(idTransaksi: String) {
        lifecycleScope.launch {
            val transaksi: TransaksiPenjualan?
            val listDetail: List<DetailPenjualan>
            val mapBarang: Map<String, Barang>
            var pelanggan: Pelanggan? = null

            withContext(Dispatchers.IO) {
                transaksi  = db.transaksiPenjualanDao().cariTransaksiById(idTransaksi)
                listDetail = db.detailPenjualanDao().ambilStrukTransaksi(idTransaksi)
                mapBarang  = db.barangDao().ambilSemuaBarang().associateBy { it.idBarang }

                transaksi?.idPelanggan?.let { idP ->
                    pelanggan = db.pelangganDao()
                        .ambilSemuaPelanggan()
                        .find { it.idPelanggan == idP }
                }
            }

            if (transaksi == null) {
                Toast.makeText(this@DetailTransaksiActivity, "Data transaksi tidak ditemukan", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            tampilkanData(transaksi, listDetail, mapBarang, pelanggan)
        }
    }

    private fun tampilkanData(
        transaksi: TransaksiPenjualan,
        listDetail: List<DetailPenjualan>,
        mapBarang: Map<String, Barang>,
        pelanggan: Pelanggan?
    ) {
        tvIdTransaksi.text = transaksi.idTransaksi
        tvTanggal.text     = formatTanggal(transaksi.tanggalTransaksi)

        rvProduk.adapter = AdapterProdukDetail(listDetail, mapBarang)

        val sp = getSharedPreferences("DataDiskonTransaksi", Context.MODE_PRIVATE)
        val diskonPersen  = sp.getInt("DISKON_PERSEN_${transaksi.idTransaksi}", intent.getIntExtra("DISKON_PELANGGAN_PERSEN", 0))
        val diskonNominal = sp.getInt("DISKON_NOMINAL_${transaksi.idTransaksi}", intent.getIntExtra("DISKON_PELANGGAN_NOMINAL", 0))
        val nominalDp     = sp.getInt("DP_NOMINAL_${transaksi.idTransaksi}", intent.getIntExtra("NOMINAL_DP", 0))

        // 🔥 PERBAIKAN LOGIKA DISKON & SUBTOTAL 🔥
        // Subtotal kotor: Harga asli normal dikali jumlah (sebelum dipotong nego/member)
        val subtotalKotor = listDetail.sumOf { it.hargaSatuan * it.qty }

        // Total diskon keseluruhan (Diskon Nego + Diskon Member) diambil dari DB
        val totalDiskonSemua = transaksi.totalDiskon ?: 0

        val totalBelanja = transaksi.totalBelanja

        // Laba bersih tetap sama: Laba kotor item - diskon member
        val labaKotor = listDetail.sumOf { d ->
            val b = mapBarang[d.idBarang]
            val hargaJual = d.hargaNego ?: d.hargaSatuan
            (hargaJual - (b?.hargaModal ?: 0)) * d.qty
        }
        val labaBersih = labaKotor - diskonNominal

        tvSubtotal.text    = "Rp ${rupiahFormat.format(subtotalKotor)}"
        tvDiskon.text      = "- Rp ${rupiahFormat.format(totalDiskonSemua)}" // Sekarang diskon Nego terlihat!
        tvLaba.text        = "Rp ${rupiahFormat.format(labaBersih)}"
        tvTotalBelanja.text = "Rp ${rupiahFormat.format(totalBelanja)}"

        // ── PELANGGAN ──────────────────────────────────────────────
        if (pelanggan != null) {
            llInfoPelanggan.visibility = View.VISIBLE
            tvNamaPelanggan.text       = pelanggan.namaPelanggan
            tvKategoriPelanggan.text   = pelanggan.kategoriPelanggan

            if (diskonNominal > 0) {
                llDiskonPelanggan.visibility = View.VISIBLE
                tvDiskonPelangganNominal.text = "-Rp ${rupiahFormat.format(diskonNominal)}"
                tvDiskonPelangganPersen.text  = "(${diskonPersen}%)"
            } else {
                llDiskonPelanggan.visibility = View.GONE
            }
        } else {
            llInfoPelanggan.visibility   = View.GONE
            llDiskonPelanggan.visibility = View.GONE
        }

        // ── STATUS DP / LUNAS ──────────────────────────────────────
        if (transaksi.statusPembayaran == "DP") {
            tvStatusTransaksi.text = "DP / Belum Lunas"
            tvStatusBadge.text     = "DP"
            tvStatusTransaksi.setTextColor(android.graphics.Color.parseColor("#B45309"))
            tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#B45309"))

            val sisaTagihan = totalBelanja - nominalDp

            llInfoDp.visibility   = View.VISIBLE
            btnLunasi.visibility  = View.VISIBLE
            tvDpDibayar.text      = "Rp ${rupiahFormat.format(nominalDp)}"
            tvSisaTagihan.text    = "Rp ${rupiahFormat.format(sisaTagihan)}"

            btnLunasi.setOnClickListener { konfirmasiPelunasan(transaksi) }
        } else {
            tvStatusTransaksi.text = "Berhasil Diselesaikan"
            tvStatusBadge.text     = "PAID FULL"
            tvStatusTransaksi.setTextColor(android.graphics.Color.parseColor("#0A3D26"))
            tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#0D5C3A"))
            llInfoDp.visibility  = View.GONE
            btnLunasi.visibility = View.GONE
        }

        btnBagikan.setOnClickListener {
            bagikanStruk(transaksi, listDetail, mapBarang, pelanggan, subtotalKotor, totalDiskonSemua, nominalDp)
        }
        btnCetak.setOnClickListener {
            Toast.makeText(this, "Fitur cetak struk belum tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun konfirmasiPelunasan(transaksi: TransaksiPenjualan) {
        AlertDialog.Builder(this)
            .setTitle("Lunasi Tagihan")
            .setMessage("Apakah pelanggan sudah melunasi sisa tagihan? Status transaksi akan diubah menjadi Lunas.")
            .setPositiveButton("Ya, Lunasi") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        db.transaksiPenjualanDao().updateTransaksi(transaksi.copy(statusPembayaran = "Lunas"))
                    }
                    Toast.makeText(this@DetailTransaksiActivity, "Pembayaran berhasil dilunasi!", Toast.LENGTH_SHORT).show()
                    muatData(transaksi.idTransaksi)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // 🔥 STRUK DIPERBARUI AGAR LEBIH PROFESIONAL
    private fun bagikanStruk(
        transaksi: TransaksiPenjualan,
        listDetail: List<DetailPenjualan>,
        mapBarang: Map<String, Barang>,
        pelanggan: Pelanggan?,
        subtotalKotor: Int,
        totalDiskonSemua: Int,
        nominalDp: Int
    ) {
        val sb = StringBuilder()
        sb.appendLine("===== STRUK TRANSAKSI =====")
        sb.appendLine("ID    : ${transaksi.idTransaksi}")
        sb.appendLine("Tgl   : ${formatTanggal(transaksi.tanggalTransaksi)}")
        sb.appendLine("Metode: ${transaksi.metode ?: "-"}")
        sb.appendLine("Status: ${transaksi.statusPembayaran}")
        if (pelanggan != null) {
            sb.appendLine("---------------------------")
            sb.appendLine("Pelanggan : ${pelanggan.namaPelanggan}")
            sb.appendLine("Kategori  : ${pelanggan.kategoriPelanggan}")
        }
        sb.appendLine("---------------------------")
        listDetail.forEach { d ->
            val nama  = mapBarang[d.idBarang]?.namaBarang ?: d.idBarang
            sb.appendLine(nama)
            sb.appendLine("  ${d.qty} x Rp ${rupiahFormat.format(d.hargaSatuan)} = Rp ${rupiahFormat.format(d.hargaSatuan * d.qty)}")
        }
        sb.appendLine("---------------------------")
        sb.appendLine("Subtotal: Rp ${rupiahFormat.format(subtotalKotor)}")
        if (totalDiskonSemua > 0) {
            sb.appendLine("Diskon  : -Rp ${rupiahFormat.format(totalDiskonSemua)}")
        }
        sb.appendLine("TOTAL   : Rp ${rupiahFormat.format(transaksi.totalBelanja)}")

        // Tampilkan info cicilan jika masih DP
        if (transaksi.statusPembayaran == "DP") {
            sb.appendLine("---------------------------")
            sb.appendLine("DP Awal      : Rp ${rupiahFormat.format(nominalDp)}")
            sb.appendLine("Sisa Tagihan : Rp ${rupiahFormat.format(transaksi.totalBelanja - nominalDp)}")
        }

        sb.appendLine("===========================")
        sb.appendLine("Terima kasih sudah berbelanja!")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(intent, "Bagikan Struk via"))
    }

    private fun formatTanggal(raw: String): String {
        return try {
            val inFmt  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outFmt = SimpleDateFormat("dd MMMM yyyy • HH:mm 'WIB'", Locale("id", "ID"))
            outFmt.format(inFmt.parse(raw) ?: Date())
        } catch (e: Exception) { raw }
    }
}