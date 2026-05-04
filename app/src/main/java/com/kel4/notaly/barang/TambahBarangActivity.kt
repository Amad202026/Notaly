package com.kel4.notaly.barang

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
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

    private lateinit var btnBack: ImageView
    private lateinit var etKodeBarang: EditText
    private lateinit var spinnerKategori: Spinner
    private lateinit var etNamaBarang: EditText
    private lateinit var etHargaModal: EditText
    private lateinit var etHargaJual: EditText
    private lateinit var etHargaGrosir: EditText
    private lateinit var etStokMin: EditText

    private var barangLama: Barang? = null
    private val isEditMode get() = barangLama != null

    companion object {
        const val EXTRA_ID_BARANG_EDIT = "extra_id_barang_edit"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barang_tambah)

        inisialisasiUI()

        val idEdit = intent.getStringExtra(EXTRA_ID_BARANG_EDIT)

        muatkanKategoriKeSpinner {
            if (!idEdit.isNullOrEmpty()) {
                muatDataBarang(idEdit)
            }
        }

        btnBack.setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnSimpan).setOnClickListener { simpanBarang() }
    }

    private fun inisialisasiUI() {
        btnBack        = findViewById(R.id.btnBack)
        etKodeBarang   = findViewById(R.id.etKodeBarang)
        spinnerKategori= findViewById(R.id.spinnerKategori)
        etNamaBarang   = findViewById(R.id.etNamaBarang)
        etHargaModal   = findViewById(R.id.etHargaModal)
        etHargaJual    = findViewById(R.id.etHargaJual)
        etHargaGrosir  = findViewById(R.id.etHargaGrosir)
        etStokMin      = findViewById(R.id.etStokMin)   // ganti dari etStokAwal
    }

    private fun muatkanKategoriKeSpinner(onComplete: () -> Unit) {
        val sharedPref   = getSharedPreferences("KategoriPrefs", MODE_PRIVATE)
        val setKategori  = sharedPref.getStringSet("DAFTAR_KATEGORI", mutableSetOf()) ?: mutableSetOf()
        val daftarKategori = setKategori.toMutableList()

        if (daftarKategori.isEmpty()) daftarKategori.add("Belum ada kategori")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, daftarKategori)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKategori.adapter = adapter

        onComplete()
    }

    private fun muatDataBarang(id: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db     = AppDatabase.getDatabase(this@TambahBarangActivity)
            val barang = db.barangDao().cariBarangBerdasarkanId(id)

            withContext(Dispatchers.Main) {
                if (barang != null) {
                    barangLama = barang

                    findViewById<TextView>(R.id.btnSimpan).text = "Simpan Perubahan"

                    etKodeBarang.isEnabled = false
                    etKodeBarang.alpha     = 0.5f

                    etKodeBarang.setText(barang.idBarang)
                    etNamaBarang.setText(barang.namaBarang)
                    etHargaModal.setText(barang.hargaModal.toString())
                    etHargaJual.setText(barang.hargaJual.toString())

                    // Ambil hargaGrosir & stokMin dari SharedPreferences
                    val sharedPref  = getSharedPreferences("DataEkstraBarang", Context.MODE_PRIVATE)
                    val hargaGrosir = sharedPref.getInt("GROSIR_${barang.idBarang}", 0)
                    val stokMin     = sharedPref.getInt("STOKMIN_${barang.idBarang}", 0)

                    if (hargaGrosir > 0) etHargaGrosir.setText(hargaGrosir.toString())
                    if (stokMin     > 0) etStokMin.setText(stokMin.toString())

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
    }

    private fun simpanBarang() {
        val kodeBarang    = etKodeBarang.text.toString().trim()
        val namaBarang    = etNamaBarang.text.toString().trim()
        val kategori      = spinnerKategori.selectedItem?.toString() ?: "Umum"
        val hargaModalStr = etHargaModal.text.toString().trim()
        val hargaJualStr  = etHargaJual.text.toString().trim()
        val hargaGrosirStr= etHargaGrosir.text.toString().trim()
        val stokMinStr    = etStokMin.text.toString().trim()

        // stokMin tidak wajib diisi (boleh 0)
        if (kodeBarang.isEmpty() || namaBarang.isEmpty() || hargaModalStr.isEmpty() || hargaJualStr.isEmpty()) {
            Toast.makeText(this, "Sila lengkapkan maklumat wajib!", Toast.LENGTH_SHORT).show()
            return
        }

        val hargaModal = hargaModalStr.toIntOrNull() ?: 0
        val hargaJual  = hargaJualStr.toIntOrNull()  ?: 0
        val hargaGrosir= hargaGrosirStr.toIntOrNull()?: 0
        val stokMin    = stokMinStr.toIntOrNull()    ?: 0

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db         = AppDatabase.getDatabase(this@TambahBarangActivity)
                val sharedPref = getSharedPreferences("DataEkstraBarang", Context.MODE_PRIVATE)

                if (isEditMode) {
                    val barangDiperbarui = barangLama!!.copy(
                        namaBarang = namaBarang,
                        kategori   = kategori,
                        hargaModal = hargaModal,
                        hargaJual  = hargaJual
                        // stok TIDAK diubah di sini, stok diubah lewat transaksi
                    )
                    db.barangDao().ubahBarang(barangDiperbarui)

                    sharedPref.edit()
                        .putInt("GROSIR_${kodeBarang}",   hargaGrosir)
                        .putInt("STOKMIN_${kodeBarang}",  stokMin)
                        .apply()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TambahBarangActivity, "Barang berjaya diperbarui", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                } else {
                    val barangBaru = Barang(
                        idBarang      = kodeBarang,
                        namaBarang    = namaBarang,
                        kategori      = kategori,
                        hargaModal    = hargaModal,
                        hargaJual     = hargaJual,
                        stok          = 0,           // stok awal selalu 0, diisi lewat transaksi
                        statusKondisi = "Normal"
                    )
                    db.barangDao().tambahBarang(barangBaru)

                    sharedPref.edit()
                        .putInt("GROSIR_${kodeBarang}",  hargaGrosir)
                        .putInt("STOKMIN_${kodeBarang}", stokMin)
                        .apply()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TambahBarangActivity, "Barang berjaya disimpan", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TambahBarangActivity, "Ralat: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}