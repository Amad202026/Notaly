package com.kel4.notaly.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplier")
data class Supplier(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID_Supplier") val idSupplier: Int = 0,
    @ColumnInfo(name = "Nama_Supplier") val namaSupplier: String,
    @ColumnInfo(name = "Kategori_Suplai") val kategoriSuplai: String?,
    @ColumnInfo(name = "Asal_Daerah") val asalDaerah: String?,
    @ColumnInfo(name = "No_WA") val noWa: String?
)