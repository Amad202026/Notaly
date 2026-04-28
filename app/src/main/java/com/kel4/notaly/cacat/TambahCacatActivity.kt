package com.kel4.notaly.cacat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.model.Barang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TambahCacatActivity : AppCompatActivity() {

    private var daftarBarang  : List<Barang> = emptyList()
    private var barangDipilih : Barang?      = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cacat_tambah)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSimpan).setOnClickListener { validasiDanSimpan() }

        muatSpinner()
    }

    private fun muatSpinner() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Hanya barang normal (bukan -C / -C2 dst) dengan stok > 0
            daftarBarang = AppDatabase.getDatabase(this@TambahCacatActivity)
                .barangDao().ambilSemuaBarang()
                .filter { it.stok > 0 && !it.idBarang.contains("-C") }

            withContext(Dispatchers.Main) {
                val spinner = findViewById<Spinner>(R.id.spinnerBarang)
                spinner.adapter = ArrayAdapter(
                    this@TambahCacatActivity,
                    android.R.layout.simple_spinner_item,
                    daftarBarang.map { "${it.namaBarang} (Stok: ${it.stok})" }
                ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                        barangDipilih = daftarBarang.getOrNull(pos)
                    }
                    override fun onNothingSelected(p: AdapterView<*>?) { barangDipilih = null }
                }
            }
        }
    }

    private fun validasiDanSimpan() {
        val barang = barangDipilih
            ?: run { Toast.makeText(this, "Pilih barang!", Toast.LENGTH_SHORT).show(); return }
        val jumlah = findViewById<EditText>(R.id.etJumlahBarang).text.toString().trim()
            .toIntOrNull()?.takeIf { it > 0 }
            ?: run { Toast.makeText(this, "Jumlah tidak valid!", Toast.LENGTH_SHORT).show(); return }
        val harga = findViewById<EditText>(R.id.etHargaBarang).text.toString()
            .replace(".", "").replace(",", "").trim().toIntOrNull()
            ?: run { Toast.makeText(this, "Harga tidak valid!", Toast.LENGTH_SHORT).show(); return }
        val ket = findViewById<EditText>(R.id.etKeteranganKerusakan).text.toString().trim().ifEmpty { "-" }

        if (jumlah > barang.stok) {
            Toast.makeText(this, "Melebihi stok (${barang.stok})", Toast.LENGTH_SHORT).show(); return
        }

        val tanggal = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())

        lifecycleScope.launch(Dispatchers.IO) {
            val db      = AppDatabase.getDatabase(this@TambahCacatActivity)
            val dao     = db.barangDao()
            val baseId  = barang.idBarang

            // ── Cari slot cacat yang cocok (harga sama) atau buat baru ──────────
            // Ambil semua varian cacat: BRG001-C, BRG001-C2, BRG001-C3, dst
            val semuaBarang  = dao.ambilSemuaBarang()
            val varianCacat  = semuaBarang
                .filter { it.idBarang == "$baseId-C" || it.idBarang.matches(Regex("${Regex.escape(baseId)}-C\\d+")) }
                .sortedBy { it.idBarang }

            // Cari varian yang harganya sama
            val varianCocok = varianCacat.firstOrNull { it.hargaJual == harga }

            if (varianCocok != null) {
                // Harga sama → akumulasi stok di varian yang ada
                dao.ubahBarang(varianCocok.copy(stok = varianCocok.stok + jumlah))
            } else {
                // Harga beda / belum ada sama sekali → buat ID & Nama baru
                val nomorBaru: Int
                val idBaru: String
                val namaBaru: String

                if (varianCacat.isEmpty()) {
                    nomorBaru = 1
                    idBaru = "$baseId-C"
                    namaBaru = "${barang.namaBarang} [C]"
                } else {
                    // Cari nomor berikutnya: C2, C3, ...
                    val nomorTerpakai = varianCacat.mapNotNull { b ->
                        val suffix = b.idBarang.removePrefix("$baseId-C")
                        if (suffix.isEmpty()) 1 else suffix.toIntOrNull()
                    }
                    nomorBaru = (1..999).first { it !in nomorTerpakai }

                    idBaru = if (nomorBaru == 1) "$baseId-C" else "$baseId-C$nomorBaru"
                    namaBaru = if (nomorBaru == 1) "${barang.namaBarang} [C]" else "${barang.namaBarang} [C$nomorBaru]"
                }

                dao.tambahBarang(
                    Barang(
                        idBarang      = idBaru,
                        namaBarang    = namaBaru, // Menggunakan nama yang sudah ditambahkan [C]
                        kategori      = barang.kategori,
                        hargaModal    = barang.hargaModal,
                        hargaJual     = harga,          // harga obral jadi harga jual
                        stok          = jumlah,
                        statusKondisi = "Cacat/Obral"
                    )
                )
            }

            // ── Kurangi stok barang normal, kondisi TIDAK diubah ───────────────
            dao.ubahBarang(barang.copy(stok = barang.stok - jumlah))

            // ── Simpan log ke SharedPreferences ───────────────────────────────
            val prefs = getSharedPreferences("NotalyPrefs", Context.MODE_PRIVATE)
            fun append(key: String, nilai: String) {
                val lama = prefs.getString(key, "")!!
                prefs.edit().putString(key, if (lama.isEmpty()) nilai else "$lama|$nilai").apply()
            }
            append("CACAT_JUMLAH_$baseId", jumlah.toString())
            append("CACAT_HARGA_$baseId",  harga.toString())
            append("CACAT_KET_$baseId",    ket.replace("|", "/"))
            append("CACAT_TGL_$baseId",    tanggal)

            withContext(Dispatchers.Main) {
                startActivity(Intent(this@TambahCacatActivity, BerhasilCacatActivity::class.java).apply {
                    putExtra("nama",       barang.namaBarang)
                    putExtra("jumlah",     jumlah)
                    putExtra("kategori",   barang.kategori)
                    putExtra("keterangan", ket)
                })
                finish()
            }
        }
    }
}