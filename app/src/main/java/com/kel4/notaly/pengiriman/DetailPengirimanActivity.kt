package com.kel4.notaly.pengiriman

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.pengiriman.TambahPengirimanActivity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DetailPengirimanActivity : AppCompatActivity() {

    private val db by lazy { AppDatabase.getDatabase(this) }
    private var pengirimanId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengiriman_detail)

        pengirimanId = intent.getIntExtra("ID_PENGIRIMAN", -1)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnEdit).setOnClickListener {
            val intent = Intent(this, TambahPengirimanActivity::class.java)
            intent.putExtra("ID_PENGIRIMAN", pengirimanId)
            startActivity(intent)
        }
        findViewById<View>(R.id.btnHapus).setOnClickListener {
            showDeleteDialog()
        }

        loadDetail()
    }

    override fun onResume() {
        super.onResume()
        loadDetail()
    }

    private fun loadDetail() {
        if (pengirimanId == -1) { finish(); return }

        lifecycleScope.launch {
            val p = db.pengirimanDao().getPengirimanById(pengirimanId)
            if (p == null) { finish(); return@launch }

            findViewById<TextView>(R.id.tvIdPengiriman).text = "#${p.idPengiriman}"
            findViewById<TextView>(R.id.tvIdTransaksi).text = p.idTransaksi ?: "-"
            findViewById<TextView>(R.id.tvEkspedisi).text = p.namaEkspedisi ?: "-"
            findViewById<TextView>(R.id.tvNomorResi).text = p.noResi ?: "-"
            findViewById<TextView>(R.id.tvAlamatLengkap).text = p.alamatLengkap ?: "-"

            val biayaFormatted = p.biayaKirim?.let {
                "Rp ${NumberFormat.getNumberInstance(Locale("id","ID")).format(it)}"
            } ?: "Rp 0"
            findViewById<TextView>(R.id.tvTotalOngkir).text = biayaFormatted
            findViewById<TextView>(R.id.tvBiayaKirim).text = biayaFormatted

            val status = p.statusKirim ?: "Diproses"
            findViewById<TextView>(R.id.tvStatusPengiriman).text = status
            findViewById<TextView>(R.id.tvStatusBadge).text = status

            val badgeView = findViewById<TextView>(R.id.tvStatusBadge)
            val statusColor = when (status) {
                "Terkirim"   -> 0xFF2E7D5B.toInt()
                "Dikirim"    -> 0xFF1976D2.toInt()
                "Diproses"   -> 0xFFFF8F00.toInt()
                "Dibatalkan" -> 0xFFD32F2F.toInt()
                else         -> 0xFF888888.toInt()
            }
            badgeView.setTextColor(statusColor)

            // Tombol salin resi
            findViewById<ImageView>(R.id.ivSalinResi).setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Nomor Resi", p.noResi ?: "")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@DetailPengirimanActivity, "Nomor resi disalin", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Pengiriman")
            .setMessage("Apakah Anda yakin ingin menghapus data pengiriman ini? Tindakan ini tidak dapat dibatalkan.")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    db.pengirimanDao().deletePengirimanById(pengirimanId)
                    Toast.makeText(this@DetailPengirimanActivity, "Data pengiriman dihapus", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}