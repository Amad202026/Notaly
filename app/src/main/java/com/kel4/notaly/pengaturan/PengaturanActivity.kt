package com.kel4.notaly.pengaturan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.home.BerandaActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PengaturanActivity : AppCompatActivity() {

    lateinit var rlGantiNamatoko: RelativeLayout
    lateinit var rlGantiPin: RelativeLayout
    lateinit var rlTanggalLahir: RelativeLayout
    lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengaturan)

        rlGantiNamatoko = findViewById(R.id.rlGantiNamatoko)
        rlGantiPin      = findViewById(R.id.rlGantiPin)
        rlTanggalLahir  = findViewById(R.id.rlGantiTanggalLahir)
        btnBack         = findViewById(R.id.btnBack)

        rlGantiNamatoko.setOnClickListener { startActivity(Intent(this, GantiNamatokoActivity::class.java)) }
        rlGantiPin.setOnClickListener      { startActivity(Intent(this, GantiPinActivity::class.java)) }
        rlTanggalLahir.setOnClickListener  { startActivity(Intent(this, GantiTanggalLahirActivity::class.java)) }
        btnBack.setOnClickListener         { startActivity(Intent(this, BerandaActivity::class.java)); finish() }

        findViewById<CardView>(R.id.btnKeluar).setOnClickListener { konfirmasiReset() }
    }

    private fun konfirmasiReset() {
        AlertDialog.Builder(this)
            .setTitle("Reset Semua Data")
            .setMessage("Semua data barang, transaksi, pelanggan, supplier, dan riwayat cacat akan dihapus permanen.\n\nYakin ingin mereset?")
            .setPositiveButton("Reset") { _, _ -> eksekusiReset() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun eksekusiReset() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Reset database: tutup → hapus file → buat ulang dari asset
            AppDatabase.resetDatabase(this@PengaturanActivity)

            // Hapus SharedPreferences
            getSharedPreferences("KategoriPrefs",    Context.MODE_PRIVATE).edit().clear().apply()
            getSharedPreferences("NotalyPrefs",      Context.MODE_PRIVATE).edit().clear().apply()
            getSharedPreferences("DataEkstraBarang", Context.MODE_PRIVATE).edit().clear().apply()

            withContext(Dispatchers.Main) {
                Toast.makeText(this@PengaturanActivity, "Semua data berhasil direset.", Toast.LENGTH_SHORT).show()
                // Kembali ke layar awal, clear seluruh back stack
                val intent = Intent(this@PengaturanActivity, com.kel4.notaly.auth.MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }
}