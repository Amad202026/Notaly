package com.kel4.notaly.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "detail_penjualan",
    foreignKeys = [
        ForeignKey(
            entity = Barang::class,
            parentColumns = ["ID_Barang"],
            childColumns = ["ID_Barang"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransaksiPenjualan::class,
            parentColumns = ["ID_Transaksi"],
            childColumns = ["ID_Transaksi"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class DetailPenjualan(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID_Detail") val idDetail: Int = 0,
    @ColumnInfo(name = "ID_Transaksi") val idTransaksi: String,
    @ColumnInfo(name = "ID_Barang") val idBarang: String,
    @ColumnInfo(name = "Qty") val qty: Int,
    @ColumnInfo(name = "Harga_Satuan") val hargaSatuan: Int,
    @ColumnInfo(name = "Harga_Nego") val hargaNego: Int?,
    @ColumnInfo(name = "Subtotal") val subtotal: Int
)