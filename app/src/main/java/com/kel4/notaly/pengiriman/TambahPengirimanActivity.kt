package com.kel4.notaly.pengiriman

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
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
import com.kel4.notaly.model.Pengiriman
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TambahPengirimanActivity : AppCompatActivity() {

    private lateinit var tvJudul: TextView
    private lateinit var etTanggal: EditText
    private lateinit var spinnerIdTransaksi: Spinner
    private lateinit var etNamaEkspedisi: EditText
    private lateinit var etNomorResi: EditText
    private lateinit var etAlamatTujuan: EditText
    private lateinit var etBiayaKirim: EditText
    private lateinit var spinnerStatusPengiriman: Spinner
    private lateinit var btnSimpan: TextView

    private val db by lazy { AppDatabase.getDatabase(this) }
    private var editId: Int = -1

    private var transaksiIdList: List<String> = emptyList()
    private val calendar = Calendar.getInstance()

    companion object {
        const val EXTRA_ID = "ID_PENGIRIMAN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengiriman_tambah)

        editId = intent.getIntExtra(EXTRA_ID, -1)

        initViews()
        setupStatusSpinner()
        setupDatePicker()

        if (editId != -1) {
            tvJudul.text = "Edit Logistik"
            btnSimpan.text = "Simpan Perubahan"
        } else {
            updateLabelTanggal()
        }

        loadTransaksiListThenFillForm()

        btnSimpan.setOnClickListener { simpanData() }
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun initViews() {
        tvJudul                 = findViewById(R.id.tvJudul)
        etTanggal               = findViewById(R.id.etTanggal)
        spinnerIdTransaksi      = findViewById(R.id.spinnerIdTransaksi)
        etNamaEkspedisi         = findViewById(R.id.etNamaEkspedisi)
        etNomorResi             = findViewById(R.id.etNomorResi)
        etAlamatTujuan          = findViewById(R.id.etAlamatTujuan)
        etBiayaKirim            = findViewById(R.id.etBiayaKirim)
        spinnerStatusPengiriman = findViewById(R.id.spinnerStatusPengiriman)
        btnSimpan               = findViewById(R.id.btnSimpan)
    }

    private fun setupDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            updateLabelTanggal()
        }

        etTanggal.setOnClickListener {
            DatePickerDialog(
                this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateLabelTanggal() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        etTanggal.setText(sdf.format(calendar.time))
    }

    // 🔥 PERUBAHAN 1: Ganti opsi spinner jadi 3 saja
    private fun setupStatusSpinner() {
        val statusList = arrayListOf("Diproses", "Dikirim", "Selesai")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatusPengiriman.adapter = adapter
    }

    private fun loadTransaksiListThenFillForm() {
        lifecycleScope.launch {
            val semuaTransaksi = db.transaksiPenjualanDao().ambilSemuaTransaksi()
            transaksiIdList = semuaTransaksi.map { transaksi -> transaksi.idTransaksi }

            val adapterSpinner = ArrayAdapter(
                this@TambahPengirimanActivity,
                android.R.layout.simple_spinner_item,
                transaksiIdList
            )
            adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerIdTransaksi.adapter = adapterSpinner

            if (editId != -1) {
                fillFormForEdit()
            }
        }
    }

    private suspend fun fillFormForEdit() {
        val p = db.pengirimanDao().getPengirimanById(editId) ?: return

        // 🔥 AMBIL TANGGAL DARI SHAREDPREFERENCES
        val sp = getSharedPreferences("DataPengirimanEkstra", MODE_PRIVATE)
        val savedTanggal = sp.getString("TGL_KIRIM_${p.idTransaksi}", "")
        etTanggal.setText(savedTanggal)

        etNamaEkspedisi.setText(p.namaEkspedisi ?: "")
        etNomorResi.setText(p.noResi ?: "")
        etAlamatTujuan.setText(p.alamatLengkap ?: "")
        etBiayaKirim.setText(p.biayaKirim?.toString() ?: "")

        val targetTransaksi = p.idTransaksi
        var idxTransaksi = 0
        for (i in transaksiIdList.indices) {
            if (transaksiIdList[i] == targetTransaksi) {
                idxTransaksi = i
                break
            }
        }
        spinnerIdTransaksi.setSelection(idxTransaksi)

        // 🔥 PERUBAHAN 2: Sesuaikan pembacaan status saat edit
        val statusList = listOf("Diproses", "Dikirim", "Selesai")

        // Konversi data lama jika sebelumnya pakai Terkirim/Dibatalkan
        val targetStatus = when (p.statusKirim) {
            "Terkirim", "Dibatalkan" -> "Selesai"
            else -> p.statusKirim
        }

        var idxStatus = statusList.indexOf(targetStatus)
        if (idxStatus == -1) idxStatus = 0 // Setel ke Diproses jika tidak ditemukan

        spinnerStatusPengiriman.setSelection(idxStatus)
    }

    private fun simpanData() {
        val tanggal       = etTanggal.text.toString().trim()
        val idTransaksi   = spinnerIdTransaksi.selectedItem?.toString()
        val namaEkspedisi = etNamaEkspedisi.text.toString().trim()
        val noResi        = etNomorResi.text.toString().trim()
        val alamat        = etAlamatTujuan.text.toString().trim()
        val biayaStr      = etBiayaKirim.text.toString().trim()
        val statusKirim   = spinnerStatusPengiriman.selectedItem?.toString()

        if (tanggal.isEmpty()) {
            etTanggal.error = "Tanggal wajib diisi"
            return
        }
        if (idTransaksi.isNullOrBlank()) {
            Toast.makeText(this, "Pilih ID Transaksi", Toast.LENGTH_SHORT).show()
            return
        }
        if (namaEkspedisi.isEmpty()) {
            etNamaEkspedisi.error = "Nama ekspedisi wajib diisi"
            etNamaEkspedisi.requestFocus(); return
        }
        if (noResi.isEmpty()) {
            etNomorResi.error = "Nomor resi wajib diisi"
            etNomorResi.requestFocus(); return
        }
        if (alamat.isEmpty()) {
            etAlamatTujuan.error = "Alamat tujuan wajib diisi"
            etAlamatTujuan.requestFocus(); return
        }
        if (biayaStr.isEmpty()) {
            etBiayaKirim.error = "Biaya kirim wajib diisi"
            etBiayaKirim.requestFocus(); return
        }
        val biaya = biayaStr.toDoubleOrNull()
        if (biaya == null || biaya < 0) {
            etBiayaKirim.error = "Biaya tidak valid"
            etBiayaKirim.requestFocus(); return
        }

        // 🔥 SIMPAN TANGGAL KE SHAREDPREFERENCES
        val sp = getSharedPreferences("DataPengirimanEkstra", MODE_PRIVATE)
        sp.edit().putString("TGL_KIRIM_$idTransaksi", tanggal).apply()

        lifecycleScope.launch {
            val pengiriman = Pengiriman(
                idPengiriman  = if (editId != -1) editId else 0,
                idTransaksi   = idTransaksi,
                namaEkspedisi = namaEkspedisi,
                noResi        = noResi,
                alamatLengkap = alamat,
                biayaKirim    = biaya,
                statusKirim   = statusKirim
            )

            if (editId != -1) {
                db.pengirimanDao().updateStatusPengiriman(pengiriman)
                Toast.makeText(this@TambahPengirimanActivity, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
            } else {
                db.pengirimanDao().buatPengiriman(pengiriman)
                Toast.makeText(this@TambahPengirimanActivity, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}