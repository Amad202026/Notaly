package com.kel4.notaly.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pelanggan")
data class Pelanggan(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID_Pelanggan") val idPelanggan: Int = 0,

    @ColumnInfo(name = "Nama_Pelanggan") val namaPelanggan: String,
    @ColumnInfo(name = "No_WA") val noWa: String?,
    @ColumnInfo(name = "Asal_Daerah") val asalDaerah: String?,
    @ColumnInfo(name = "Kategori_Pelanggan") val kategoriPelanggan: String? // 'Umum', 'Grosir', 'Member'
)