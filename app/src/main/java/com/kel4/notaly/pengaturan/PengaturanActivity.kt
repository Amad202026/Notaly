package com.kel4.notaly.pengaturan

import android.content.Intent
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R

class PengaturanActivity : AppCompatActivity() {

    lateinit var rlGantiNamatoko: RelativeLayout
    lateinit var rlGantiPin: RelativeLayout
    lateinit var rlTanggalLahir: RelativeLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengaturan)

        rlGantiNamatoko = findViewById(R.id.rlGantiNamatoko)
        rlGantiPin = findViewById(R.id.rlGantiPin)
        rlTanggalLahir = findViewById(R.id.rlGantiTanggalLahir)

        rlGantiNamatoko.setOnClickListener {
            startActivity(Intent(this, GantiNamatokoActivity::class.java))
        }
        rlGantiPin.setOnClickListener {
            startActivity(Intent(this, GantiPinActivity::class.java))
        }
        rlTanggalLahir.setOnClickListener {
            startActivity(Intent(this, GantiTanggalLahirActivity::class.java))
        }

    }
}