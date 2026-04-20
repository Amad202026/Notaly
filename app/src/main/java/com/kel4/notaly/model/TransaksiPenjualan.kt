package com.kel4.notaly.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// KITA TAMBAHKAN ATURAN FOREIGN KEY DI SINI AGAR SAMA DENGAN DATABASE ASLI
@Entity(
    tableName = "transaksi_penjualan",
    foreignKeys = [
        ForeignKey(
            entity = Pelanggan::class,
            parentColumns = ["ID_Pelanggan"],
            childColumns = ["ID_Pelanggan"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class TransaksiPenjualan(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "ID_Transaksi") val idTransaksi: String,

    @ColumnInfo(name = "ID_Pelanggan") val idPelanggan: Int?,
    @ColumnInfo(name = "Tanggal_Transaksi") val tanggalTransaksi: String,
    @ColumnInfo(name = "Metode") val metode: String?,
    @ColumnInfo(name = "Status_Pembayaran") val statusPembayaran: String,
    @ColumnInfo(name = "Total_Belanja") val totalBelanja: Int,
    @ColumnInfo(name = "Total_Diskon") val totalDiskon: Int?
)