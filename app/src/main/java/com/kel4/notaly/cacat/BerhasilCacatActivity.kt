package com.kel4.notaly.cacat

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R

class BerhasilCacatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cacat_berhasil)

        findViewById<TextView>(R.id.tvNamaBarang)      .text = intent.getStringExtra("nama")       ?: "-"
        findViewById<TextView>(R.id.tvJumlah)          .text = "${intent.getIntExtra("jumlah", 0)} Pcs"
        findViewById<TextView>(R.id.tvKategori)        .text = intent.getStringExtra("kategori")   ?: "-"
        findViewById<TextView>(R.id.tvCatatanKerusakan).text = intent.getStringExtra("keterangan") ?: "-"

        findViewById<Button>(R.id.btnInputLagi).setOnClickListener {
            startActivity(Intent(this, TambahCacatActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.btnLihatLaporan).setOnClickListener {
            startActivity(Intent(this, CacatActivity::class.java))
            finish()
        }
    }
}