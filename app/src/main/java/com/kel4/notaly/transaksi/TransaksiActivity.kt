package com.kel4.notaly.transaksi

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R
import com.kel4.notaly.barang.TambahBarangActivity
import com.kel4.notaly.dao.BarangDao
import com.kel4.notaly.dao.DetailPenjualanDao
import com.kel4.notaly.dao.TransaksiPenjualanDao
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

class TransaksiActivity : AppCompatActivity() {

    // ===================== DAO & DB =====================
    private lateinit var db:          AppDatabase
    private lateinit var barangDao:   BarangDao
    private lateinit var transaksiDao: TransaksiPenjualanDao
    private lateinit var detailDao:   DetailPenjualanDao

    // ===================== DATA =====================
    private var semuaBarang:          List<Barang>    = emptyList()
    private var semuaPelanggan:       List<Pelanggan> = emptyList()
    private var pelangganDipilih:     Pelanggan?      = null
    private var diskonPelangganPersen: Double         = 0.0

    /**
     * Satu baris item di form transaksi.
     * [hargaNego] = harga final yang tampil di kolom input (bisa harga normal, nego, atau grosir).
     */
    data class ItemBaris(
        var barang:    Barang? = null,
        var hargaNego: Int?    = null,
        var jumlah:    Int     = 0
    )

    private val daftarItem       = mutableListOf(ItemBaris())
    private var metodeBayar:      String = "Transfer"
    private var statusPembayaran: String = "Lunas"
    private lateinit var idTransaksi: String

    // ===================== VIEW =====================
    private lateinit var containerItem:        LinearLayout
    private lateinit var btnTambahItem:        Button
    private lateinit var tvTotalPenjualan:     TextView
    private lateinit var tvItemTerpilih:       TextView
    private lateinit var tvIdTransaksi:        TextView
    private lateinit var rbTransfer:           RadioButton
    private lateinit var rbQris:               RadioButton
    private lateinit var rbTunai:              RadioButton

    private lateinit var spinnerPelanggan:     Spinner
    private lateinit var layoutDiskonPelanggan: LinearLayout
    private lateinit var tvDiskonPelanggan:    TextView
    private lateinit var layoutDiskonTotal:    LinearLayout
    private lateinit var tvDiskonTotal:        TextView

    private lateinit var btnBayarDp:    LinearLayout
    private lateinit var btnLunas:      LinearLayout
    private lateinit var tvBayarDp:     TextView
    private lateinit var tvLunas:       TextView
    private lateinit var indicatorDp:   View
    private lateinit var indicatorLunas: View
    private lateinit var layoutInputDp: LinearLayout
    private lateinit var etJumlahDp:    EditText

    private lateinit var btnSimpan: TextView
    private lateinit var btnBack:   ImageView

    // ===================== FORMAT =====================
    private val rupiahFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    companion object {
        private const val PREF_DISKON         = "pref_diskon_pelanggan"
        private const val KEY_DISKON_GROSIR   = "diskon_terakhir_grosir"
        private const val KEY_DISKON_MEMBER   = "diskon_terakhir_member"
        private const val PREF_EKSTRA_BARANG  = "DataEkstraBarang"
    }

    // ===================== LIFECYCLE =====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaksi)

        db           = AppDatabase.getDatabase(this)
        barangDao    = db.barangDao()
        transaksiDao = db.transaksiPenjualanDao()
        detailDao    = db.detailPenjualanDao()
        idTransaksi  = generateIdTransaksi()

        bindView()
        setupListeners()
        muatBarang()
        muatPelanggan()
    }

    // ===================== SETUP =====================

    private fun bindView() {
        containerItem         = findViewById(R.id.containerItem)
        btnTambahItem         = findViewById(R.id.btnTambahItem)
        tvTotalPenjualan      = findViewById(R.id.tvTotalPenjualan)
        tvItemTerpilih        = findViewById(R.id.tvItemTerpilih)
        tvIdTransaksi         = findViewById(R.id.tvIdTransaksi)
        rbTransfer            = findViewById(R.id.rbTransfer)
        rbQris                = findViewById(R.id.rbQris)
        rbTunai               = findViewById(R.id.rbTunai)

        spinnerPelanggan      = findViewById(R.id.spinnerPelanggan)
        layoutDiskonPelanggan = findViewById(R.id.layoutDiskonPelanggan)
        tvDiskonPelanggan     = findViewById(R.id.tvDiskonPelanggan)
        layoutDiskonTotal     = findViewById(R.id.layoutDiskonTotal)
        tvDiskonTotal         = findViewById(R.id.tvDiskonTotal)

        btnBayarDp            = findViewById(R.id.btnBayarDp)
        btnLunas              = findViewById(R.id.btnLunas)
        tvBayarDp             = findViewById(R.id.tvBayarDp)
        tvLunas               = findViewById(R.id.tvLunas)
        indicatorDp           = findViewById(R.id.indicatorDp)
        indicatorLunas        = findViewById(R.id.indicatorLunas)
        layoutInputDp         = findViewById(R.id.layoutInputDp)
        etJumlahDp            = findViewById(R.id.etJumlahDp)

        btnSimpan             = findViewById(R.id.btnSimpan)
        btnBack               = findViewById(R.id.btnBack)

        tvIdTransaksi.text = idTransaksi
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnTambahItem.setOnClickListener {
            daftarItem.add(ItemBaris())
            tambahBarisItemUI(daftarItem.size - 1)
            updateTotal()
        }

        rbTransfer.setOnClickListener { setMetodePembayaran("Transfer") }
        rbQris.setOnClickListener     { setMetodePembayaran("QRIS") }
        rbTunai.setOnClickListener    { setMetodePembayaran("Tunai") }

        btnBayarDp.setOnClickListener { setStatusDP() }
        btnLunas.setOnClickListener   { setStatusLunas() }

        btnSimpan.setOnClickListener  { konfirmasiSimpan() }

        setMetodePembayaran("Transfer")
        setStatusLunas()
    }

    // ===================== LOAD DATA =====================

    private fun muatBarang() {
        lifecycleScope.launch {
            semuaBarang = withContext(Dispatchers.IO) { barangDao.ambilSemuaBarang() }
            containerItem.removeAllViews()
            daftarItem.forEachIndexed { idx, _ -> tambahBarisItemUI(idx) }
        }
    }

    private fun muatPelanggan() {
        lifecycleScope.launch {
            semuaPelanggan = withContext(Dispatchers.IO) { db.pelangganDao().ambilSemuaPelanggan() }

            val labelList = mutableListOf("-- Tanpa Pelanggan --")
            labelList.addAll(semuaPelanggan.map { p ->
                "#${String.format("%03d", p.idPelanggan)} · ${p.namaPelanggan} (${p.kategoriPelanggan})"
            })

            val adapter = ArrayAdapter(this@TransaksiActivity, android.R.layout.simple_spinner_dropdown_item, labelList)
            spinnerPelanggan.adapter = adapter

            spinnerPelanggan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (pos == 0) {
                        pelangganDipilih      = null
                        diskonPelangganPersen = 0.0
                        layoutDiskonPelanggan.visibility = View.GONE
                        layoutDiskonTotal.visibility     = View.GONE
                        toggleNegoInput(true)
                    } else {
                        val p = semuaPelanggan[pos - 1]
                        pelangganDipilih      = p
                        diskonPelangganPersen = getDiskonPelanggan(p.kategoriPelanggan.toString())

                        if (diskonPelangganPersen > 0) {
                            tvDiskonPelanggan.text = "${formatAngka(diskonPelangganPersen)}%"
                            layoutDiskonPelanggan.visibility = View.VISIBLE
                            // Kunci nego saat pelanggan dapat diskon member/grosir
                            toggleNegoInput(false)
                            Toast.makeText(this@TransaksiActivity, "Harga dikunci (Pelanggan Diskon)", Toast.LENGTH_SHORT).show()
                        } else {
                            layoutDiskonPelanggan.visibility = View.GONE
                            toggleNegoInput(true)
                        }
                    }
                    updateTotal()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    // ===================== UI HELPER =====================

    /** Mengunci/membuka input harga nego berdasarkan status diskon pelanggan. */
    private fun toggleNegoInput(isEnable: Boolean) {
        for (i in 0 until containerItem.childCount) {
            val rowView = containerItem.getChildAt(i)
            val etNego  = rowView.findViewById<EditText>(R.id.etHargaNego) ?: continue

            etNego.isEnabled = isEnable

            if (!isEnable) {
                // Kembalikan ke harga normal jika dikunci
                val hargaNormal = daftarItem[i].barang?.hargaJual
                etNego.setText(hargaNormal?.toString() ?: "")
            }
        }
    }

    private fun getDiskonPelanggan(kategori: String): Double {
        val key = when (kategori) {
            "Member" -> KEY_DISKON_MEMBER
            "Grosir" -> KEY_DISKON_GROSIR
            else     -> return 0.0
        }
        return getSharedPreferences(PREF_DISKON, Context.MODE_PRIVATE).getInt(key, 0).toDouble()
    }

    private fun setMetodePembayaran(metode: String) {
        metodeBayar          = metode
        rbTransfer.isChecked = (metode == "Transfer")
        rbQris.isChecked     = (metode == "QRIS")
        rbTunai.isChecked    = (metode == "Tunai")
    }

    private fun setStatusDP() {
        statusPembayaran = "DP"
        btnBayarDp.setBackgroundResource(R.drawable.bg_white_rounded)
        tvBayarDp.setTextColor(Color.parseColor("#1A1A1A"))
        indicatorDp.visibility    = View.VISIBLE
        btnLunas.setBackgroundResource(android.R.color.transparent)
        tvLunas.setTextColor(Color.parseColor("#888888"))
        indicatorLunas.visibility = View.GONE
        layoutInputDp.visibility  = View.VISIBLE
    }

    private fun setStatusLunas() {
        statusPembayaran = "Lunas"
        btnLunas.setBackgroundResource(R.drawable.bg_white_rounded)
        tvLunas.setTextColor(Color.parseColor("#1A1A1A"))
        indicatorLunas.visibility = View.VISIBLE
        btnBayarDp.setBackgroundResource(android.R.color.transparent)
        tvBayarDp.setTextColor(Color.parseColor("#888888"))
        indicatorDp.visibility   = View.GONE
        layoutInputDp.visibility = View.GONE
        etJumlahDp.text.clear()
    }

    private fun formatAngka(nilai: Double): String =
        if (nilai == kotlin.math.floor(nilai)) nilai.toInt().toString() else nilai.toString()

    // ===================== BARIS ITEM =====================

    private fun tambahBarisItemUI(index: Int) {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_baris_transaksi, containerItem, false)

        val tvLabel    : TextView  = view.findViewById(R.id.tvLabelItem)
        val spinner    : Spinner   = view.findViewById(R.id.spinnerBarang)
        val etHargaNego: EditText  = view.findViewById(R.id.etHargaNego)
        val etJumlah   : EditText  = view.findViewById(R.id.etJumlahTerjual)
        val tvSubtotal : TextView  = view.findViewById(R.id.tvSubtotal)
        val btnHapus   : ImageView = view.findViewById(R.id.btnHapusItem)

        tvLabel.text           = "ITEM #${index + 1}"
        etHargaNego.isEnabled  = diskonPelangganPersen <= 0

        btnHapus.visibility = if (daftarItem.size > 1) View.VISIBLE else View.GONE
        btnHapus.setOnClickListener {
            daftarItem.removeAt(index)
            containerItem.removeView(view)
            refreshLabelItem()
            updateTotal()
        }

        // Isi spinner barang
        val namaList = semuaBarang.map { "${it.namaBarang} (Stok: ${it.stok})" }.toMutableList()
        namaList.add(0, "-- Pilih Barang --")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, namaList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Restore selection jika ada (misal setelah reload)
        daftarItem[index].barang?.let { b ->
            val pos = semuaBarang.indexOfFirst { it.idBarang == b.idBarang }
            if (pos >= 0) spinner.setSelection(pos + 1)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val barang = if (pos == 0) null else semuaBarang[pos - 1]
                daftarItem[index].barang = barang

                if (barang != null) {
                    // Default: tampilkan harga normal. Grosir akan dicek saat qty berubah.
                    etHargaNego.setText(barang.hargaJual.toString())
                } else {
                    etHargaNego.setText("")
                }

                hitungSubtotal(index, etHargaNego, etJumlah, tvSubtotal)
                updateTotal()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Cek apakah qty memenuhi syarat grosir, lalu update harga otomatis
                cekDanTerapkanGrosir(index, etHargaNego, etJumlah)
                hitungSubtotal(index, etHargaNego, etJumlah, tvSubtotal)
                updateTotal()
            }
        }
        etHargaNego.addTextChangedListener(watcher)
        etJumlah.addTextChangedListener(watcher)

        containerItem.addView(view)
    }

    /**
     * Cek apakah qty yang diinput memenuhi syarat grosir untuk barang ini.
     * Jika ya → set harga otomatis ke harga grosir (masuk sebagai harga nego/diskon).
     * Jika tidak → kembalikan ke harga normal.
     *
     * Logika ini TIDAK berjalan jika pelanggan sedang mendapat diskon member/kategori,
     * karena dalam kondisi itu kolom harga dikunci dan diskon dihitung dari sisi lain.
     */
    private fun cekDanTerapkanGrosir(index: Int, etHargaNego: EditText, etJumlah: EditText) {
        // Jangan proses grosir jika ada diskon pelanggan aktif (harga sudah dikunci)
        if (diskonPelangganPersen > 0) return

        val barang = daftarItem[index].barang ?: return
        val qty    = etJumlah.text.toString().trim().toIntOrNull() ?: 0

        val pref      = getSharedPreferences(PREF_EKSTRA_BARANG, Context.MODE_PRIVATE)
        val hargaGrosir = pref.getInt(TambahBarangActivity.keyGrosir(barang.idBarang), 0)
        val minGrosir   = pref.getInt(TambahBarangActivity.keyMinGrosir(barang.idBarang), 0)

        // Grosir hanya aktif jika keduanya terdefinisi dan qty mencukupi
        val grosirAktif = hargaGrosir > 0 && minGrosir > 0 && qty >= minGrosir

        val hargaBaru = if (grosirAktif) hargaGrosir else barang.hargaJual

        // Hanya update jika harga berubah, untuk menghindari loop TextWatcher
        val hargaSekarang = etHargaNego.text.toString().trim().toIntOrNull()
        if (hargaSekarang != hargaBaru) {
            etHargaNego.removeTextChangedListener(null) // watcher sudah anonymous, pakai tag
            etHargaNego.setText(hargaBaru.toString())
            etHargaNego.setSelection(etHargaNego.text.length)
        }
    }

    private fun hitungSubtotal(index: Int, etNego: EditText, etJumlah: EditText, tvSubtotal: TextView) {
        val item = daftarItem[index]
        if (item.barang == null) {
            tvSubtotal.text = "Rp 0"
            return
        }

        val hargaInput = etNego.text.toString().trim().toIntOrNull()   ?: 0
        val jumlah     = etJumlah.text.toString().trim().toIntOrNull() ?: 0

        daftarItem[index] = item.copy(hargaNego = hargaInput, jumlah = jumlah)
        tvSubtotal.text = "Rp ${rupiahFormat.format(hargaInput * jumlah)}"
    }

    private fun refreshLabelItem() {
        for (i in 0 until containerItem.childCount) {
            val child = containerItem.getChildAt(i)
            child.findViewById<TextView>(R.id.tvLabelItem)?.text  = "ITEM #${i + 1}"
            child.findViewById<ImageView>(R.id.btnHapusItem)?.visibility =
                if (containerItem.childCount > 1) View.VISIBLE else View.GONE
        }
    }

    // ===================== KALKULASI TOTAL =====================

    private fun updateTotal() {
        var subtotal  = 0
        var itemCount = 0

        daftarItem.forEach { item ->
            val harga  = item.hargaNego ?: 0
            subtotal  += harga * item.jumlah
            if (item.jumlah > 0) itemCount++
        }

        val potongan = if (diskonPelangganPersen > 0) {
            (subtotal * diskonPelangganPersen / 100.0).toInt()
        } else 0

        val total = subtotal - potongan

        tvTotalPenjualan.text = "Rp ${rupiahFormat.format(total)}"
        tvItemTerpilih.text   = "$itemCount Item Terpilih"

        if (potongan > 0) {
            tvDiskonTotal.text           = "-Rp ${rupiahFormat.format(potongan)}"
            layoutDiskonTotal.visibility = View.VISIBLE
        } else {
            layoutDiskonTotal.visibility = View.GONE
        }
    }

    // ===================== SIMPAN TRANSAKSI =====================

    private fun validasiForm(): Boolean {
        for ((idx, item) in daftarItem.withIndex()) {
            if (item.barang == null) {
                Toast.makeText(this, "Item #${idx + 1}: Pilih barang terlebih dahulu", Toast.LENGTH_SHORT).show()
                return false
            }
            if (item.jumlah <= 0) {
                Toast.makeText(this, "Item #${idx + 1}: Jumlah harus lebih dari 0", Toast.LENGTH_SHORT).show()
                return false
            }
            if (item.jumlah > (item.barang?.stok ?: 0)) {
                Toast.makeText(this, "Item #${idx + 1}: Jumlah melebihi stok (${item.barang?.stok})", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun konfirmasiSimpan() {
        if (!validasiForm()) return

        val subtotal  = daftarItem.sumOf { (it.hargaNego ?: 0) * it.jumlah }
        val potongan  = if (diskonPelangganPersen > 0) (subtotal * diskonPelangganPersen / 100.0).toInt() else 0
        val total     = subtotal - potongan
        var nominalDp = 0

        if (statusPembayaran == "DP") {
            val teksDp = etJumlahDp.text.toString().trim()
            if (teksDp.isEmpty()) {
                Toast.makeText(this, "Masukkan nominal DP terlebih dahulu", Toast.LENGTH_SHORT).show()
                return
            }
            nominalDp = teksDp.toIntOrNull() ?: 0
            if (nominalDp <= 0 || nominalDp >= total) {
                Toast.makeText(this, "Nominal DP tidak valid.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val infoNama   = pelangganDipilih?.namaPelanggan?.let { "\nPelanggan : $it" } ?: ""
        val infoDiskon = if (potongan > 0) "\nDiskon     : ${formatAngka(diskonPelangganPersen)}% (-Rp ${rupiahFormat.format(potongan)})" else ""
        val infoDp     = if (statusPembayaran == "DP") "\nNominal DP : Rp ${rupiahFormat.format(nominalDp)}" else ""

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Transaksi")
            .setMessage(
                "Apakah Anda yakin ingin menyimpan transaksi ini?\n\n" +
                        "ID       : $idTransaksi$infoNama\n" +
                        "Total    : Rp ${rupiahFormat.format(total)}\n" +
                        "Metode   : $metodeBayar\n" +
                        "Status   : $statusPembayaran$infoDiskon$infoDp"
            )
            .setPositiveButton("Ya, Simpan") { _, _ -> simpanTransaksi(nominalDp, potongan, total) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun simpanTransaksi(nominalDp: Int, diskonPelanggan: Int, totalAkhir: Int) {
        val tanggalNow = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val pref       = getSharedPreferences(PREF_EKSTRA_BARANG, Context.MODE_PRIVATE)

        // Hitung total diskon nego:
        // 1. Diskon dari harga grosir  → selisih harga jual normal vs harga grosir × qty
        // 2. Diskon dari nego manual   → selisih harga jual normal vs harga input × qty
        // Keduanya masuk ke totalDiskon untuk ditampilkan di detail transaksi.
        val diskonNego = daftarItem.sumOf { item ->
            val b          = item.barang ?: return@sumOf 0
            val hargaInput = item.hargaNego ?: 0
            if (hargaInput < b.hargaJual) (b.hargaJual - hargaInput) * item.jumlah else 0
        }
        val totalDiskon = diskonNego + diskonPelanggan

        val transaksi = TransaksiPenjualan(
            idTransaksi      = idTransaksi,
            idPelanggan      = pelangganDipilih?.idPelanggan,
            tanggalTransaksi = tanggalNow,
            metode           = metodeBayar,
            statusPembayaran = statusPembayaran,
            totalBelanja     = totalAkhir,
            totalDiskon      = totalDiskon
        )

        val listDetail = daftarItem.mapNotNull { item ->
            val b         = item.barang ?: return@mapNotNull null
            val hargaAkhir = item.hargaNego ?: b.hargaJual
            DetailPenjualan(
                idTransaksi = idTransaksi,
                idBarang    = b.idBarang,
                qty         = item.jumlah,
                hargaSatuan = b.hargaJual,     // harga normal selalu dicatat sebagai referensi
                hargaNego   = hargaAkhir,       // harga final (normal / nego / grosir)
                subtotal    = hargaAkhir * item.jumlah
            )
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                transaksiDao.buatTransaksi(transaksi)
                detailDao.tambahDetailBelanja(listDetail)
                listDetail.forEach { d -> barangDao.kurangiStok(d.idBarang, d.qty) }
            }

            // Simpan data pendukung untuk DetailTransaksiActivity
            getSharedPreferences("DataDiskonTransaksi", Context.MODE_PRIVATE).edit()
                .putInt("DISKON_PERSEN_$idTransaksi",  diskonPelangganPersen.toInt())
                .putInt("DISKON_NOMINAL_$idTransaksi", diskonPelanggan)
                .putString("PELANGGAN_NAMA_$idTransaksi", pelangganDipilih?.namaPelanggan ?: "Umum")
                .putInt("DP_NOMINAL_$idTransaksi",     nominalDp)
                .apply()

            Toast.makeText(this@TransaksiActivity, "Transaksi berhasil disimpan!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this@TransaksiActivity, DetailTransaksiActivity::class.java).apply {
                putExtra("ID_TRANSAKSI",           idTransaksi)
                putExtra("NOMINAL_DP",             nominalDp)
                putExtra("DISKON_PELANGGAN_PERSEN", diskonPelangganPersen)
                putExtra("DISKON_PELANGGAN_NOMINAL", diskonPelanggan)
                putExtra("NAMA_PELANGGAN",          pelangganDipilih?.namaPelanggan ?: "")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    // ===================== UTIL =====================

    private fun generateIdTransaksi(): String {
        val tgl = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val sp  = getSharedPreferences("DataToko", MODE_PRIVATE)
        val tanggalTerakhir = sp.getString("TANGGAL_TERAKHIR", "")
        var urutan = sp.getInt("URUTAN_TERAKHIR", 0)

        if (tgl == tanggalTerakhir) urutan++ else { urutan = 1; sp.edit().putString("TANGGAL_TERAKHIR", tgl).apply() }
        sp.edit().putInt("URUTAN_TERAKHIR", urutan).apply()

        return "TX-$tgl-${String.format(Locale.getDefault(), "%04d", urutan)}"
    }
}