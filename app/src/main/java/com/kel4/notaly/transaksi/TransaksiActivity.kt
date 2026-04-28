package com.kel4.notaly.transaksi

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R
import com.kel4.notaly.dao.BarangDao
import com.kel4.notaly.dao.DetailPenjualanDao
import com.kel4.notaly.dao.TransaksiPenjualanDao
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

class TransaksiActivity : AppCompatActivity() {

    // ===================== DAO & DB =====================
    private lateinit var db: AppDatabase
    private lateinit var barangDao: BarangDao
    private lateinit var transaksiDao: TransaksiPenjualanDao
    private lateinit var detailDao: DetailPenjualanDao

    // ===================== DATA =====================
    private var semuaBarang: List<Barang> = emptyList()

    // Setiap item baris transaksi disimpan di sini
    data class ItemBaris(
        var barang: Barang? = null,
        var hargaNego: Int? = null,
        var jumlah: Int = 0
    )

    private val daftarItem = mutableListOf(ItemBaris()) // mulai dengan 1 baris

    // Metode pembayaran yang dipilih
    private var metodeBayar: String = "Transfer"

    // Status DP / Lunas
    private var statusPembayaran: String = "DP"

    // ===================== VIEW =====================
    private lateinit var containerItem: LinearLayout
    private lateinit var btnTambahItem: Button
    private lateinit var tvTotalPenjualan: TextView
    private lateinit var tvItemTerpilih: TextView
    private lateinit var tvIdTransaksi: TextView
    private lateinit var rbTransfer: RadioButton
    private lateinit var rbQris: RadioButton
    private lateinit var rbTunai: RadioButton
    private lateinit var btnBayarDp: Button
    private lateinit var btnLunas: Button
    private lateinit var btnSimpan: Button
    private lateinit var btnBack: ImageView

    // ID transaksi yang di-generate saat activity dibuka
    private lateinit var idTransaksi: String

    // ===================== FORMAT =====================
    private val rupiahFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaksi)

        db = AppDatabase.getDatabase(this)
        barangDao = db.barangDao()
        transaksiDao = db.transaksiPenjualanDao()
        detailDao = db.detailPenjualanDao()

        idTransaksi = generateIdTransaksi()

        bindView()
        setupListeners()
        muatBarang()
    }

    // ─────────────────────────────────────────────────────────
    //  BIND VIEW
    // ─────────────────────────────────────────────────────────
    private fun bindView() {
        containerItem   = findViewById(R.id.containerItem)
        btnTambahItem   = findViewById(R.id.btnTambahItem)
        tvTotalPenjualan = findViewById(R.id.tvTotalPenjualan)
        tvItemTerpilih  = findViewById(R.id.tvItemTerpilih)
        tvIdTransaksi   = findViewById(R.id.tvIdTransaksi)
        rbTransfer      = findViewById(R.id.rbTransfer)
        rbQris          = findViewById(R.id.rbQris)
        rbTunai         = findViewById(R.id.rbTunai)
        btnBayarDp      = findViewById(R.id.btnBayarDp)
        btnLunas        = findViewById(R.id.btnLunas)
        btnSimpan       = findViewById(R.id.btnSimpan)
        btnBack         = findViewById(R.id.btnBack)

        tvIdTransaksi.text = idTransaksi
    }

    // ─────────────────────────────────────────────────────────
    //  SETUP LISTENERS
    // ─────────────────────────────────────────────────────────
    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnTambahItem.setOnClickListener {
            daftarItem.add(ItemBaris())
            tambahBarisItemUI(daftarItem.size - 1)
            updateTotal()
        }

        // Radio metode pembayaran (YANG BARU)
        rbTransfer.setOnClickListener { setMetodePembayaran("Transfer") }
        rbQris.setOnClickListener     { setMetodePembayaran("QRIS") }
        rbTunai.setOnClickListener    { setMetodePembayaran("Tunai") }

        // Tombol DP / Lunas toggle
        btnBayarDp.setOnClickListener  { setStatusDP() }
        btnLunas.setOnClickListener    { setStatusLunas() }

        // Default saat halaman pertama dibuka: Transfer dipilih, DP aktif
        setMetodePembayaran("Transfer")
        setStatusDP()

        btnSimpan.setOnClickListener { konfirmasiSimpan() }

        // Tombol DP / Lunas toggle
        btnBayarDp.setOnClickListener  { setStatusDP() }
        btnLunas.setOnClickListener    { setStatusLunas() }

        // Default: Transfer dipilih, DP aktif
        rbTransfer.isChecked = true
        setStatusDP()

        btnSimpan.setOnClickListener { konfirmasiSimpan() }
    }

    // ─────────────────────────────────────────────────────────
    //  LOAD BARANG dari DB
    // ─────────────────────────────────────────────────────────
    private fun muatBarang() {
        lifecycleScope.launch {
            semuaBarang = withContext(Dispatchers.IO) { barangDao.ambilSemuaBarang() }
            // Render baris pertama setelah barang dimuat
            containerItem.removeAllViews()
            daftarItem.forEachIndexed { idx, _ -> tambahBarisItemUI(idx) }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  TAMBAH BARIS ITEM UI DINAMIS
    // ─────────────────────────────────────────────────────────
    private fun tambahBarisItemUI(index: Int) {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_baris_transaksi, containerItem, false)

        val tvLabel       : TextView  = view.findViewById(R.id.tvLabelItem)
        val spinner       : Spinner   = view.findViewById(R.id.spinnerBarang)
        val etHargaNego   : EditText  = view.findViewById(R.id.etHargaNego)
        val etJumlah      : EditText  = view.findViewById(R.id.etJumlahTerjual)
        val tvSubtotal    : TextView  = view.findViewById(R.id.tvSubtotal)
        val btnHapus      : ImageView = view.findViewById(R.id.btnHapusItem)

        tvLabel.text = "ITEM #${index + 1}"

        // Sembunyikan tombol hapus jika hanya 1 item
        btnHapus.visibility = if (daftarItem.size > 1) View.VISIBLE else View.GONE
        btnHapus.setOnClickListener {
            daftarItem.removeAt(index)
            containerItem.removeView(view)
            // Re-render semua label nomor item
            refreshLabelItem()
            updateTotal()
        }

        // Adapter spinner barang
        val namaBarang = semuaBarang.map { "${it.namaBarang} (Stok: ${it.stok})" }.toMutableList()
        namaBarang.add(0, "-- Pilih Barang --")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, namaBarang)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Restore pilihan jika ada
        daftarItem[index].barang?.let { b ->
            val posisi = semuaBarang.indexOfFirst { it.idBarang == b.idBarang }
            if (posisi >= 0) spinner.setSelection(posisi + 1)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos == 0) {
                    daftarItem[index].barang = null
                } else {
                    val barang = semuaBarang[pos - 1]
                    daftarItem[index].barang = barang
                    // Hitung subtotal otomatis
                    hitungSubtotal(index, etHargaNego, etJumlah, tvSubtotal)
                }
                updateTotal()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Watcher harga nego & jumlah
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                hitungSubtotal(index, etHargaNego, etJumlah, tvSubtotal)
                updateTotal()
            }
        }
        etHargaNego.addTextChangedListener(watcher)
        etJumlah.addTextChangedListener(watcher)

        containerItem.addView(view)
    }

    // Hitung subtotal per baris
    private fun hitungSubtotal(
        index: Int,
        etNego: EditText,
        etJumlah: EditText,
        tvSubtotal: TextView
    ) {
        val item   = daftarItem[index]
        val barang = item.barang ?: run {
            tvSubtotal.text = "Rp 0"
            return
        }
        val nego   = etNego.text.toString().trim().toIntOrNull()
        val jumlah = etJumlah.text.toString().trim().toIntOrNull() ?: 0

        val hargaSatuan = nego ?: barang.hargaJual
        val subtotal    = hargaSatuan * jumlah

        daftarItem[index] = item.copy(hargaNego = nego, jumlah = jumlah)
        tvSubtotal.text = "Rp ${rupiahFormat.format(subtotal)}"
    }

    // Refresh ulang label "ITEM #N" setelah ada yang dihapus
    private fun refreshLabelItem() {
        for (i in 0 until containerItem.childCount) {
            val child = containerItem.getChildAt(i)
            val tv = child.findViewById<TextView>(R.id.tvLabelItem)
            tv?.text = "ITEM #${i + 1}"
            // Hapus button visibility
            val btnHapus = child.findViewById<ImageView>(R.id.btnHapusItem)
            btnHapus?.visibility = if (containerItem.childCount > 1) View.VISIBLE else View.GONE
        }
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE TOTAL BAWAH
    // ─────────────────────────────────────────────────────────
    private fun updateTotal() {
        var total = 0
        var itemCount = 0
        daftarItem.forEach { item ->
            val barang = item.barang ?: return@forEach
            val harga  = item.hargaNego ?: barang.hargaJual
            val sub    = harga * item.jumlah
            total += sub
            if (item.jumlah > 0) itemCount++
        }
        tvTotalPenjualan.text = "Rp ${rupiahFormat.format(total)},00"
        tvItemTerpilih.text   = "$itemCount Item Terpilih"
    }

    // ─────────────────────────────────────────────────────────
    //  STATUS DP / LUNAS TOGGLE
    // ─────────────────────────────────────────────────────────
    private fun setStatusDP() {
        statusPembayaran = "DP"
        btnBayarDp.setBackgroundResource(R.drawable.bg_white_rounded)
        btnBayarDp.setTextColor(getColor(R.color.black))
        btnLunas.setBackgroundResource(R.drawable.bg_search_rounded)
        btnLunas.setTextColor(getColor(android.R.color.darker_gray))
    }

    private fun setStatusLunas() {
        statusPembayaran = "Lunas"
        btnLunas.setBackgroundResource(R.drawable.bg_white_rounded)
        btnLunas.setTextColor(getColor(R.color.black))
        btnBayarDp.setBackgroundResource(R.drawable.bg_search_rounded)
        btnBayarDp.setTextColor(getColor(android.R.color.darker_gray))
    }

    // ─────────────────────────────────────────────────────────
    //  VALIDASI FORM
    // ─────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────
    //  KONFIRMASI SIMPAN (AlertDialog)
    // ─────────────────────────────────────────────────────────
    private fun konfirmasiSimpan() {
        if (!validasiForm()) return

        val total = hitungTotalKeseluruhan()
        val totalFormatted = "Rp ${rupiahFormat.format(total)}"

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Transaksi")
            .setMessage(
                "Apakah Anda yakin ingin menyimpan transaksi ini?\n\n" +
                        "ID       : $idTransaksi\n" +
                        "Total    : $totalFormatted\n" +
                        "Metode   : $metodeBayar\n" +
                        "Status   : $statusPembayaran"
            )
            .setPositiveButton("Ya, Simpan") { _, _ -> simpanTransaksi() }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ─────────────────────────────────────────────────────────
    //  HITUNG TOTAL KESELURUHAN
    // ─────────────────────────────────────────────────────────
    private fun hitungTotalKeseluruhan(): Int {
        return daftarItem.sumOf { item ->
            val barang = item.barang ?: return@sumOf 0
            val harga  = item.hargaNego ?: barang.hargaJual
            harga * item.jumlah
        }
    }

    // ─────────────────────────────────────────────────────────
    //  SIMPAN TRANSAKSI ke DB
    // ─────────────────────────────────────────────────────────
    private fun simpanTransaksi() {
        val total        = hitungTotalKeseluruhan()
        val tanggalNow   = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val totalDiskon  = daftarItem.sumOf { item ->
            val b = item.barang ?: return@sumOf 0
            val nego = item.hargaNego
            if (nego != null && nego < b.hargaJual) (b.hargaJual - nego) * item.jumlah else 0
        }

        val transaksi = TransaksiPenjualan(
            idTransaksi      = idTransaksi,
            idPelanggan      = null,
            tanggalTransaksi = tanggalNow,
            metode           = metodeBayar,
            statusPembayaran = statusPembayaran,
            totalBelanja     = total,
            totalDiskon      = totalDiskon
        )

        val listDetail = daftarItem.mapNotNull { item ->
            val b = item.barang ?: return@mapNotNull null
            DetailPenjualan(
                idTransaksi  = idTransaksi,
                idBarang     = b.idBarang,
                qty          = item.jumlah,
                hargaSatuan  = b.hargaJual,
                hargaNego    = item.hargaNego,
                subtotal     = (item.hargaNego ?: b.hargaJual) * item.jumlah
            )
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // 1. Simpan header transaksi
                transaksiDao.buatTransaksi(transaksi)
                // 2. Simpan detail
                detailDao.tambahDetailBelanja(listDetail)
                // 3. Kurangi stok barang
                listDetail.forEach { d ->
                    barangDao.kurangiStok(d.idBarang, d.qty)
                }
            }

            Toast.makeText(this@TransaksiActivity, "Transaksi berhasil disimpan!", Toast.LENGTH_SHORT).show()

            // Pindah ke detail transaksi
            val intent = Intent(this@TransaksiActivity, DetailTransaksiActivity::class.java)
            intent.putExtra("ID_TRANSAKSI", idTransaksi)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    // ─────────────────────────────────────────────────────────
    //  GENERATE ID TRANSAKSI  (TX-YYYYMMDD-XXXX)
    // ─────────────────────────────────────────────────────────
    private fun generateIdTransaksi(): String {
        val tgl    = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val random = (1000..9999).random()
        return "TX-$tgl-$random"
    }

    // ─────────────────────────────────────────────────────────
    //  PILIH METODE PEMBAYARAN
    // ─────────────────────────────────────────────────────────
    private fun setMetodePembayaran(metode: String) {
        metodeBayar = metode

        // Hanya akan bernilai 'true' (tercentang) jika metodenya cocok
        rbTransfer.isChecked = (metode == "Transfer")
        rbQris.isChecked     = (metode == "QRIS")
        rbTunai.isChecked    = (metode == "Tunai")
    }
}