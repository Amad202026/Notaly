package com.kel4.notaly.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "barang_masuk",
    foreignKeys = [
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["ID_Supplier"],
            childColumns = ["ID_Supplier"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Barang::class,
            parentColumns = ["ID_Barang"],
            childColumns = ["ID_Barang"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class BarangMasuk(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID_Restok") val idRestok: Int = 0,
    @ColumnInfo(name = "ID_Supplier") val idSupplier: Int,
    @ColumnInfo(name = "ID_Barang") val idBarang: String,
    @ColumnInfo(name = "Tanggal_Masuk") val tanggalMasuk: String,
    @ColumnInfo(name = "Qty_Masuk") val qtyMasuk: Int,
    @ColumnInfo(name = "Harga_Beli") val hargaBeli: Double
)

data class DetailRestokLengkap(
    val idRestok: Int,
    val namaBarang: String,
    val kategori: String,
    val namaSupplier: String,
    val asalSupplier: String,
    val totalQty: Int,
    val tanggalMasuk: String,
    val hargaBeli: Double
)
