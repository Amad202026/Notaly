package com.kel4.notaly.pelanggan

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
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

    private lateinit var etNama:            EditText
    private lateinit var etWa:              EditText
    private lateinit var etDaerah:          EditText
    private lateinit var spinnerKat:        Spinner
    private lateinit var btnSimpan:         TextView
    private lateinit var btnBack:           ImageView
    private lateinit var tvIdPelanggan:     TextView
    private lateinit var tvJudul:           TextView

    // --- Diskon ---
    private lateinit var sectionDiskon:     LinearLayout
    private lateinit var etDiskon:          EditText
    private lateinit var layoutPreviewDiskon: LinearLayout
    private lateinit var tvPreviewDiskon:   TextView

    private var pelangganLama: Pelanggan? = null
    private val isEditMode get() = pelangganLama != null

    private val kategoriList = listOf("Umum", "Grosir", "Member")

    /**
     * Kategori yang berhak mendapatkan diskon.
     * Kalau "Umum" → input diskon disembunyikan.
     */
    private val kategoriDiskon = setOf("Grosir", "Member")

    companion object {
        const val EXTRA_ID_PELANGGAN_EDIT = "extra_id_pelanggan_edit"

        // Key SharedPreferences untuk menyimpan diskon terakhir per kategori
        private const val PREF_NAME           = "pref_diskon_pelanggan"
        private const val KEY_DISKON_GROSIR   = "diskon_terakhir_grosir"
        private const val KEY_DISKON_MEMBER   = "diskon_terakhir_member"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pelanggan_tambah)

        initViews()
        setupSpinner()
        setupDiskonListener()
        setupListeners()

        val idEdit = intent.getIntExtra(EXTRA_ID_PELANGGAN_EDIT, -1)
        if (idEdit != -1) {
            muatDataUntukEdit(idEdit)
        } else {
            tvIdPelanggan.text = "Otomatis"
            tvJudul.text       = "Tambah Pelanggan"
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────────

    private fun initViews() {
        etNama              = findViewById(R.id.etNamaLengkap)
        etWa                = findViewById(R.id.etNomorWA)
        etDaerah            = findViewById(R.id.etAsalDaerah)
        spinnerKat          = findViewById(R.id.spinnerKategori)
        btnSimpan           = findViewById(R.id.btnSimpanPelanggan)
        btnBack             = findViewById(R.id.btnBack)
        tvIdPelanggan       = findViewById(R.id.tvIdPelanggan)
        tvJudul             = findViewById(R.id.tvJudul)
        sectionDiskon       = findViewById(R.id.sectionDiskon)
        etDiskon            = findViewById(R.id.etDiskon)
        layoutPreviewDiskon = findViewById(R.id.layoutPreviewDiskon)
        tvPreviewDiskon     = findViewById(R.id.tvPreviewDiskon)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnSimpan.setOnClickListener { simpanData() }
    }

    // ─────────────────────────────────────────────────────────────────
    // SPINNER KATEGORI
    // ─────────────────────────────────────────────────────────────────

    private fun setupSpinner() {
        spinnerKat.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            kategoriList
        )

        spinnerKat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val kategori = kategoriList[pos]
                tampilkanAtauSembunyikanDiskon(kategori)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // LOGIKA DISKON
    // ─────────────────────────────────────────────────────────────────

    private fun setupDiskonListener() {
        etDiskon.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val nilai = s?.toString()?.toDoubleOrNull()
                if (nilai != null && nilai > 0) {
                    tvPreviewDiskon.text       = "${formatDiskon(nilai)}%"
                    layoutPreviewDiskon.visibility = View.VISIBLE
                } else {
                    layoutPreviewDiskon.visibility = View.GONE
                }
            }
        })
    }

    /**
     * Tampilkan atau sembunyikan section diskon berdasarkan kategori.
     * Saat muncul, isi default dari SharedPreferences (diskon terakhir kategori tersebut).
     */
    private fun tampilkanAtauSembunyikanDiskon(kategori: String) {
        if (kategori in kategoriDiskon) {
            // Isi default dari preferensi terakhir (hanya saat bukan edit mode supaya
            // tidak menimpa nilai yang sudah ada di DB — di edit mode nilai diambil
            // dari pelangganLama.diskon di muatDataUntukEdit)
            if (!isEditMode && sectionDiskon.visibility != View.VISIBLE) {
                val diskonDefault = getDiskonTerakhir(kategori)
                if (diskonDefault > 0) {
                    etDiskon.setText(formatDiskon(diskonDefault))
                }
            }

            // Animasi slide-down
            if (sectionDiskon.visibility != View.VISIBLE) {
                sectionDiskon.visibility = View.VISIBLE
                val anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
                anim.duration = 250
                sectionDiskon.startAnimation(anim)
            }
        } else {
            // Sembunyikan & bersihkan preview
            if (sectionDiskon.visibility == View.VISIBLE) {
                val anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
                anim.duration = 200
                sectionDiskon.startAnimation(anim)
            }
            sectionDiskon.visibility       = View.GONE
            layoutPreviewDiskon.visibility = View.GONE
            etDiskon.text?.clear()
        }
    }

    // ── SharedPreferences helpers ──────────────────────────────────

    private fun prefKey(kategori: String) = when (kategori) {
        "Member" -> KEY_DISKON_MEMBER
        "Grosir" -> KEY_DISKON_GROSIR
        else     -> null
    }

    private fun getDiskonTerakhir(kategori: String): Double {
        val key = prefKey(kategori) ?: return 0.0
        val pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getFloat(key, 0f).toDouble()
    }

    private fun simpanDiskonTerakhir(kategori: String, diskon: Double) {
        val key = prefKey(kategori) ?: return
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(key, diskon.toFloat())
            .apply()
    }

    // ── Utilitas format ────────────────────────────────────────────

    /**
     * Format angka diskon: hilangkan desimal kalau nilainya bulat.
     * Contoh: 10.0 → "10", 12.5 → "12.5"
     */
    private fun formatDiskon(nilai: Double): String {
        return if (nilai == kotlin.math.floor(nilai)) {
            nilai.toInt().toString()
        } else {
            nilai.toString()
        }
    }

    /**
     * Ambil nilai diskon dari EditText. Kembalikan null jika section
     * diskon tidak terlihat atau input kosong/tidak valid.
     */
    private fun getNilaiDiskon(): Double? {
        if (sectionDiskon.visibility != View.VISIBLE) return null
        return etDiskon.text.toString().toDoubleOrNull()
    }

    // ─────────────────────────────────────────────────────────────────
    // MUAT DATA EDIT
    // ─────────────────────────────────────────────────────────────────

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

                pelangganLama      = p
                tvJudul.text       = "Edit Pelanggan"
                tvIdPelanggan.text = String.format("%03d", p.idPelanggan)

                etNama.setText(p.namaPelanggan)
                etWa.setText(p.noWa ?: "")
                etDaerah.setText(p.asalDaerah ?: "")

                val idx = kategoriList.indexOfFirst {
                    it.equals(p.kategoriPelanggan, ignoreCase = true)
                }.coerceAtLeast(0)
                spinnerKat.setSelection(idx)

                // Isi diskon dari SharedPreferences jika kategorinya berhak diskon
                // (diskon tidak disimpan di DB, jadi kita ambil dari preferensi terakhir)
                val kategori = kategoriList[idx]
                if (kategori in kategoriDiskon) {
                    val diskonTerakhir = getDiskonTerakhir(kategori)
                    if (diskonTerakhir > 0) {
                        etDiskon.setText(formatDiskon(diskonTerakhir))
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // SIMPAN DATA
    // ─────────────────────────────────────────────────────────────────

    private fun simpanData() {
        val nama     = etNama.text.toString().trim()
        val wa       = etWa.text.toString().trim()
        val daerah   = etDaerah.text.toString().trim()
        val kategori = spinnerKat.selectedItem.toString()
        val diskon   = getNilaiDiskon()

        if (nama.isBlank()) {
            etNama.error = "Nama pelanggan wajib diisi"
            etNama.requestFocus()
            return
        }

        // Validasi diskon jika kategori butuh diskon
        if (kategori in kategoriDiskon) {
            val nilaiDiskon = diskon
            if (nilaiDiskon == null || nilaiDiskon < 0 || nilaiDiskon > 100) {
                etDiskon.error = "Masukkan diskon yang valid (0–100)"
                etDiskon.requestFocus()
                return
            }
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
                        // Simpan diskon terakhir ke preferensi (tidak ke DB)
                        diskon?.let { simpanDiskonTerakhir(kategori, it) }

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
                        // Simpan diskon terakhir ke preferensi (tidak ke DB)
                        diskon?.let { simpanDiskonTerakhir(kategori, it) }

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