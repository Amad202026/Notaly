package com.kel4.notaly.pelanggan

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.model.Pelanggan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TambahPelangganActivity : AppCompatActivity() {

    private lateinit var etNama:        EditText
    private lateinit var etWa:          EditText
    private lateinit var etDaerah:      EditText
    private lateinit var spinnerKat:    Spinner
    private lateinit var btnSimpan:     Button
    private lateinit var btnBatal:      Button
    private lateinit var btnBack:       ImageButton
    private lateinit var tvIdPelanggan: TextView
    private lateinit var tvJudul:       TextView
    private lateinit var tvTopBarJudul: TextView

    private var pelangganLama: Pelanggan? = null
    private val isEditMode get() = pelangganLama != null

    private val kategoriList = listOf("Umum", "Grosir", "Member")

    companion object {
        const val EXTRA_ID_PELANGGAN_EDIT = "extra_id_pelanggan_edit"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pelanggan_tambah)

        initViews()
        setupListeners()
        setupSpinner()

        val idEdit = intent.getIntExtra(EXTRA_ID_PELANGGAN_EDIT, -1)
        if (idEdit != -1) {
            muatDataUntukEdit(idEdit)
        } else {
            tvIdPelanggan.text  = "Otomatis"
            tvJudul.text        = "Tambah Pelanggan"
            tvTopBarJudul.text  = "TAMBAH PELANGGAN"
        }
    }

    private fun initViews() {
        etNama        = findViewById(R.id.etNamaLengkap)
        etWa          = findViewById(R.id.etNomorWA)
        etDaerah      = findViewById(R.id.etAsalDaerah)
        spinnerKat    = findViewById(R.id.spinnerKategori)
        btnSimpan     = findViewById(R.id.btnSimpanPelanggan)
        btnBatal      = findViewById(R.id.btnBatal)
        btnBack       = findViewById(R.id.btnBack)
        tvIdPelanggan = findViewById(R.id.tvIdPelanggan)
        tvJudul       = findViewById(R.id.tvJudul)
        tvTopBarJudul = findViewById(R.id.tvTopBarJudul)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnBatal.setOnClickListener { finish() }
        btnSimpan.setOnClickListener { simpanData() }
    }

    private fun setupSpinner() {
        spinnerKat.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            kategoriList
        )
    }

    private fun muatDataUntukEdit(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val p = AppDatabase.getDatabase(this@TambahPelangganActivity)
                .pelangganDao()
                .ambilSemuaPelanggan()
                .find { it.idPelanggan == id }

            withContext(Dispatchers.Main) {
                if (p == null) {
                    Toast.makeText(this@TambahPelangganActivity, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                    return@withContext
                }

                pelangganLama       = p
                tvJudul.text        = "Edit Pelanggan"
                tvTopBarJudul.text  = "EDIT PELANGGAN"
                tvIdPelanggan.text  = String.format("%03d", p.idPelanggan)

                etNama.setText(p.namaPelanggan)
                etWa.setText(p.noWa ?: "")
                etDaerah.setText(p.asalDaerah ?: "")

                val idx = kategoriList.indexOfFirst {
                    it.equals(p.kategoriPelanggan, ignoreCase = true)
                }.coerceAtLeast(0)
                spinnerKat.setSelection(idx)
            }
        }
    }

    private fun simpanData() {
        val nama    = etNama.text.toString().trim()
        val wa      = etWa.text.toString().trim()
        val daerah  = etDaerah.text.toString().trim()
        val kategori = spinnerKat.selectedItem.toString()

        if (nama.isBlank()) {
            etNama.error = "Nama pelanggan wajib diisi"
            etNama.requestFocus()
            return
        }

        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (isEditMode) {
                    val diperbarui = pelangganLama!!.copy(
                        namaPelanggan     = nama,
                        noWa              = wa.ifBlank { null },
                        asalDaerah        = daerah.ifBlank { null },
                        kategoriPelanggan = kategori
                    )
                    db.pelangganDao().ubahPelanggan(diperbarui)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TambahPelangganActivity, "Pelanggan berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                } else {
                    db.pelangganDao().tambahPelanggan(
                        Pelanggan(
                            namaPelanggan     = nama,
                            noWa              = wa.ifBlank { null },
                            asalDaerah        = daerah.ifBlank { null },
                            kategoriPelanggan = kategori
                        )
                    )

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TambahPelangganActivity, "Pelanggan berhasil disimpan", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TambahPelangganActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}