package com.kel4.notaly.barang

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.model.Barang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TambahBarangActivity : AppCompatActivity() {

    // ===================== VIEW =====================
    private lateinit var btnBack:         ImageView
    private lateinit var etKodeBarang:    EditText
    private lateinit var spinnerKategori: Spinner
    private lateinit var etNamaBarang:    EditText
    private lateinit var etHargaModal:    EditText
    private lateinit var etHargaJual:     EditText
    private lateinit var etHargaGrosir:   EditText
    private lateinit var etMinGrosir:     EditText
    private lateinit var etStokMin:       EditText

    // ===================== STATE =====================
    private var barangLama: Barang? = null
    private val isEditMode get() = barangLama != null

    companion object {
        const val EXTRA_ID_BARANG_EDIT = "extra_id_barang_edit"

        // Key SharedPreferences untuk data ekstra barang
        fun keyGrosir(id: String)    = "GROSIR_$id"
        fun keyMinGrosir(id: String) = "MINGROSIR_$id"
        fun keyStokMin(id: String)   = "STOKMIN_$id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barang_tambah)

        inisialisasiUI()

        val idEdit = intent.getStringExtra(EXTRA_ID_BARANG_EDIT)

        muatKategoriKeSpinner {
            if (!idEdit.isNullOrEmpty()) muatDataBarang(idEdit)
        }

        btnBack.setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnSimpan).setOnClickListener { simpanBarang() }
    }

    // ===================== UI =====================

    private fun inisialisasiUI() {
        btnBack         = findViewById(R.id.btnBack)
        etKodeBarang    = findViewById(R.id.etKodeBarang)
        spinnerKategori = findViewById(R.id.spinnerKategori)
        etNamaBarang    = findViewById(R.id.etNamaBarang)
        etHargaModal    = findViewById(R.id.etHargaModal)
        etHargaJual     = findViewById(R.id.etHargaJual)
        etHargaGrosir   = findViewById(R.id.etHargaGrosir)
        etMinGrosir     = findViewById(R.id.etMinGrosir)
        etStokMin       = findViewById(R.id.etStokMin)
    }

    private fun muatKategoriKeSpinner(onComplete: () -> Unit) {
        val pref = getSharedPreferences("KategoriPrefs", MODE_PRIVATE)
        val set  = pref.getStringSet("DAFTAR_KATEGORI", mutableSetOf()) ?: mutableSetOf()
        val list = set.toMutableList().ifEmpty { mutableListOf("Belum ada kategori") }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, list)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKategori.adapter = adapter

        onComplete()
    }

    // ===================== LOAD DATA (mode edit) =====================

    private fun muatDataBarang(id: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db     = AppDatabase.getDatabase(this@TambahBarangActivity)
            val barang = db.barangDao().cariBarangBerdasarkanId(id)

            withContext(Dispatchers.Main) {
                if (barang == null) return@withContext
                barangLama = barang

                // Ubah label tombol dan kunci kode
                findViewById<TextView>(R.id.btnSimpan).text = "Simpan Perubahan"
                etKodeBarang.isEnabled = false
                etKodeBarang.alpha     = 0.5f

                // Isi field dari database
                etKodeBarang.setText(barang.idBarang)
                etNamaBarang.setText(barang.namaBarang)
                etHargaModal.setText(barang.hargaModal.toString())
                etHargaJual.setText(barang.hargaJual.toString())

                // Isi field ekstra dari SharedPreferences
                val pref     = getSharedPreferences("DataEkstraBarang", Context.MODE_PRIVATE)
                val grosir   = pref.getInt(keyGrosir(barang.idBarang), 0)
                val minGrosir = pref.getInt(keyMinGrosir(barang.idBarang), 0)
                val stokMin  = pref.getInt(keyStokMin(barang.idBarang), 0)

                if (grosir    > 0) etHargaGrosir.setText(grosir.toString())
                if (minGrosir > 0) etMinGrosir.setText(minGrosir.toString())
                if (stokMin   > 0) etStokMin.setText(stokMin.toString())

                // Pilih kategori di spinner
                val adapter = spinnerKategori.adapter as ArrayAdapter<String>
                for (i in 0 until adapter.count) {
                    if (adapter.getItem(i).equals(barang.kategori, ignoreCase = true)) {
                        spinnerKategori.setSelection(i)
                        break
                    }
                }
            }
        }
    }

    // ===================== SIMPAN =====================

    private fun simpanBarang() {
        val kodeBarang = etKodeBarang.text.toString().trim()
        val namaBarang = etNamaBarang.text.toString().trim()
        val kategori   = spinnerKategori.selectedItem?.toString() ?: "Umum"
        val hargaModal = etHargaModal.text.toString().trim()
        val hargaJual  = etHargaJual.text.toString().trim()

        // Validasi field wajib
        if (kodeBarang.isEmpty() || namaBarang.isEmpty() || hargaModal.isEmpty() || hargaJual.isEmpty()) {
            Toast.makeText(this, "Lengkapi semua field yang wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        val hargaModalInt  = hargaModal.toIntOrNull() ?: 0
        val hargaJualInt   = hargaJual.toIntOrNull()  ?: 0
        val hargaGrosirInt = etHargaGrosir.text.toString().trim().toIntOrNull() ?: 0
        val minGrosirInt   = etMinGrosir.text.toString().trim().toIntOrNull()   ?: 0
        val stokMinInt     = etStokMin.text.toString().trim().toIntOrNull()     ?: 0

        // Validasi konsistensi grosir: jika salah satu diisi, yang lain juga harus diisi
        val adaGrosir    = hargaGrosirInt > 0
        val adaMinGrosir = minGrosirInt   > 0
        if (adaGrosir != adaMinGrosir) {
            Toast.makeText(
                this,
                if (adaGrosir) "Isi juga minimal qty untuk harga grosir!"
                else           "Isi juga harga grosir jika minimal qty sudah diisi!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db   = AppDatabase.getDatabase(this@TambahBarangActivity)
                val pref = getSharedPreferences("DataEkstraBarang", Context.MODE_PRIVATE)

                if (isEditMode) {
                    // Update data utama barang (stok tidak diubah di sini)
                    val updated = barangLama!!.copy(
                        namaBarang = namaBarang,
                        kategori   = kategori,
                        hargaModal = hargaModalInt,
                        hargaJual  = hargaJualInt
                    )
                    db.barangDao().ubahBarang(updated)
                } else {
                    // Insert barang baru, stok awal = 0
                    val baru = Barang(
                        idBarang      = kodeBarang,
                        namaBarang    = namaBarang,
                        kategori      = kategori,
                        hargaModal    = hargaModalInt,
                        hargaJual     = hargaJualInt,
                        stok          = 0,
                        statusKondisi = "Normal"
                    )
                    db.barangDao().tambahBarang(baru)
                }

                // Simpan data ekstra (grosir, minGrosir, stokMin) ke SharedPreferences
                pref.edit()
                    .putInt(keyGrosir(kodeBarang),    hargaGrosirInt)
                    .putInt(keyMinGrosir(kodeBarang), minGrosirInt)
                    .putInt(keyStokMin(kodeBarang),   stokMinInt)
                    .apply()

                withContext(Dispatchers.Main) {
                    val pesan = if (isEditMode) "Barang berhasil diperbarui" else "Barang berhasil disimpan"
                    Toast.makeText(this@TambahBarangActivity, pesan, Toast.LENGTH_SHORT).show()
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TambahBarangActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}