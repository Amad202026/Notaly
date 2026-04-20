package com.kel4.notaly.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barang")
data class Barang(
    @PrimaryKey(autoGenerate = false) // false karena tipenya TEXT
    @ColumnInfo(name = "ID_Barang") val idBarang: String,
    @ColumnInfo(name = "Nama_Barang") val namaBarang: String,
    @ColumnInfo(name = "Kategori") val kategori: String,
    @ColumnInfo(name = "Harga_Modal") val hargaModal: Int,
    @ColumnInfo(name = "Harga_Jual") val hargaJual: Int,
    @ColumnInfo(name = "Stok") val stok: Int,

    // Tipe String nullable (?) karena bisa kosong atau berisi 'Normal' / 'Cacat/Obral'
    @ColumnInfo(name = "Status_Kondisi") val statusKondisi: String?
)