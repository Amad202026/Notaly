package com.kel4.notaly.pelanggan

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.model.Pelanggan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailPelangganActivity : AppCompatActivity() {

    private var pelanggan: Pelanggan? = null
    private var idPelanggan: Int = -1

    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            muatDetail(idPelanggan)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pelanggan_detail)

        idPelanggan = intent.getIntExtra("ID_PELANGGAN", -1)
        if (idPelanggan == -1) { finish(); return }

        findViewById<ImageButton>(R.id.ivBack).setOnClickListener { finish() }

        muatDetail(idPelanggan)
    }

    private fun muatDetail(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@DetailPelangganActivity)
            val p  = db.pelangganDao().ambilSemuaPelanggan().find { it.idPelanggan == id }

            withContext(Dispatchers.Main) {
                if (p == null) { finish(); return@withContext }
                pelanggan = p
                isiView(p)
            }
        }
    }

    private fun isiView(p: Pelanggan) {
        val kategori = p.kategoriPelanggan ?: "Umum"

        val inisial = p.namaPelanggan.trim().firstOrNull()?.uppercase() ?: "?"
        findViewById<TextView>(R.id.tvAvatar).text          = inisial
        findViewById<TextView>(R.id.tvNama).text            = p.namaPelanggan
        findViewById<TextView>(R.id.tvKategori).text        = kategori.uppercase()
        findViewById<TextView>(R.id.tvId).text              = "ID: ${String.format("%03d", p.idPelanggan)}"
        findViewById<TextView>(R.id.tvWaNumber).text        = p.noWa ?: "-"
        findViewById<TextView>(R.id.tvDetailNama).text      = p.namaPelanggan
        findViewById<TextView>(R.id.tvDetailId).text        = String.format("%03d", p.idPelanggan)
        findViewById<TextView>(R.id.tvDetailWa).text        = p.noWa ?: "-"
        findViewById<TextView>(R.id.tvDetailDaerah).text    = p.asalDaerah ?: "-"
        findViewById<TextView>(R.id.tvDetailKategori).text  = "$kategori ✦"

        val badgeColor = when (kategori.lowercase()) {
            "member" -> android.graphics.Color.parseColor("#2E7D5B")
            "grosir" -> android.graphics.Color.parseColor("#1565C0")
            else     -> android.graphics.Color.parseColor("#757575")
        }
        findViewById<TextView>(R.id.tvKategori).setBackgroundColor(badgeColor)

        // Klik WA → buka WhatsApp
        findViewById<TextView>(R.id.tvWaNumber).setOnClickListener {
            val noWa = p.noWa?.replace(Regex("[^0-9+]"), "") ?: return@setOnClickListener
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$noWa")))
            } catch (e: Exception) {
                Toast.makeText(this, "WhatsApp tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }

        // Fungsi buka edit — pakai TambahPelangganActivity dengan EXTRA mode edit
        val bukaEdit = {
            val intent = Intent(this, TambahPelangganActivity::class.java)
            intent.putExtra(TambahPelangganActivity.EXTRA_ID_PELANGGAN_EDIT, p.idPelanggan)
            editLauncher.launch(intent)
        }


        findViewById<TextView>(R.id.tvEditBottom).setOnClickListener { bukaEdit() }

        // Hapus
        findViewById<TextView>(R.id.tvHapus).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Pelanggan")
                .setMessage("Hapus \"${p.namaPelanggan}\" dari daftar?")
                .setPositiveButton("Hapus") { _, _ -> hapusPelanggan(p) }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun hapusPelanggan(p: Pelanggan) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(this@DetailPelangganActivity)
                .pelangganDao().hapusPelanggan(p)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DetailPelangganActivity, "Pelanggan dihapus", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}