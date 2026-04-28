package com.kel4.notaly.barang

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.model.Barang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class DetailBarangActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID_BARANG = "extra_id_barang"
    }

    private lateinit var tvHeaderNama       : TextView
    private lateinit var tvBadgeKategori    : TextView
    private lateinit var tvHeaderId         : TextView
    private lateinit var tvHeaderStok       : TextView
    private lateinit var tvDetailHargaModal : TextView
    private lateinit var tvDetailHargaJual  : TextView
    private lateinit var tvDetailHargaGrosir: TextView
    private lateinit var tvDetailStatus     : TextView
    private lateinit var tvDetailBarangRusak: TextView
    private lateinit var btnBack            : ImageView
    private lateinit var btnHapusTop        : ImageView
    private lateinit var btnEditTop         : ImageView

    private var barangSaatIni    : Barang? = null
    private var idBarangDiterima : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barang_detail)

        initViews()
        setupListeners()

        idBarangDiterima = intent.getStringExtra(EXTRA_ID_BARANG)
        if (idBarangDiterima != null) muatDataBarang(idBarangDiterima!!)
        else { Toast.makeText(this, "Data tidak ditemukan!", Toast.LENGTH_SHORT).show(); finish() }
    }

    override fun onResume() {
        super.onResume()
        idBarangDiterima?.let { muatDataBarang(it) }
    }

    private fun muatDataBarang(id: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val barang = AppDatabase.getDatabase(this@DetailBarangActivity)
                .barangDao().cariBarangBerdasarkanId(id)
            withContext(Dispatchers.Main) {
                if (barang != null) { barangSaatIni = barang; tampilkanDataKeUI(barang) }
                else { Toast.makeText(this@DetailBarangActivity, "Barang telah dihapus.", Toast.LENGTH_SHORT).show(); finish() }
            }
        }
    }

    private fun initViews() {
        tvHeaderNama        = findViewById(R.id.tvHeaderNama)
        tvBadgeKategori     = findViewById(R.id.tvBadgeKategori)
        tvHeaderId          = findViewById(R.id.tvHeaderId)
        tvHeaderStok        = findViewById(R.id.tvHeaderStok)
        tvDetailHargaModal  = findViewById(R.id.tvDetailHargaModal)
        tvDetailHargaJual   = findViewById(R.id.tvDetailHargaJual)
        tvDetailHargaGrosir = findViewById(R.id.tvDetailHargaGrosir)
        tvDetailStatus      = findViewById(R.id.tvDetailStatus)
        tvDetailBarangRusak = findViewById(R.id.tvDetailBarangRusak)
        btnBack             = findViewById(R.id.btnBack)
        btnHapusTop         = findViewById(R.id.btnHapusTop)
        btnEditTop          = findViewById(R.id.btnEditTop)
    }

    private fun tampilkanDataKeUI(barang: Barang) {
        val fmt            = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        val sharedPref     = getSharedPreferences("DataEkstraBarang", Context.MODE_PRIVATE)
        val hargaGrosir    = sharedPref.getInt("GROSIR_${barang.idBarang}", 0)

        // Total cacat — baca langsung dari SharedPrefs tanpa helper class
        val totalCacat     = getSharedPreferences("NotalyPrefs", Context.MODE_PRIVATE)
            .getString("CACAT_JUMLAH_${barang.idBarang}", "")
            ?.split("|")?.filter { it.isNotEmpty() }
            ?.sumOf { it.toIntOrNull() ?: 0 } ?: 0

        tvHeaderNama        .text = barang.namaBarang
        tvBadgeKategori     .text = barang.kategori.uppercase()
        tvHeaderId          .text = "ID: ${barang.idBarang}"
        tvHeaderStok        .text = "${barang.stok} Pcs"
        tvDetailHargaModal  .text = fmt.format(barang.hargaModal).replace("Rp", "Rp ")
        tvDetailHargaJual   .text = fmt.format(barang.hargaJual).replace("Rp", "Rp ")
        tvDetailHargaGrosir .text = fmt.format(hargaGrosir).replace("Rp", "Rp ")
        tvDetailStatus      .text = barang.statusKondisi ?: "Normal"
        tvDetailBarangRusak .text = "$totalCacat Pcs"
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnHapusTop.setOnClickListener { barangSaatIni?.let { konfirmasiHapus(it) } }
        btnEditTop.setOnClickListener {
            startActivity(Intent(this, TambahBarangActivity::class.java).apply {
                putExtra(TambahBarangActivity.EXTRA_ID_BARANG_EDIT, barangSaatIni?.idBarang)
            })
        }
    }

    private fun konfirmasiHapus(barang: Barang) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Barang")
            .setMessage("Anda yakin ingin menghapus '${barang.namaBarang}'?")
            .setPositiveButton("Hapus") { _, _ -> eksekusiHapus(barang) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun eksekusiHapus(barang: Barang) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(this@DetailBarangActivity).barangDao().hapusBarang(barang)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DetailBarangActivity, "Barang berhasil dihapus!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}