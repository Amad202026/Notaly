package com.kel4.notaly.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "pengiriman",
    foreignKeys = [
        ForeignKey(
            entity = TransaksiPenjualan::class,
            parentColumns = ["ID_Transaksi"],
            childColumns = ["ID_Transaksi"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class Pengiriman(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID_Pengiriman") val idPengiriman: Int = 0,
    @ColumnInfo(name = "ID_Transaksi") val idTransaksi: String?,
    @ColumnInfo(name = "Nama_Ekspedisi") val namaEkspedisi: String?,
    @ColumnInfo(name = "No_Resi") val noResi: String?,
    @ColumnInfo(name = "Alamat_Lengkap") val alamatLengkap: String?,
    @ColumnInfo(name = "Biaya_Kirim") val biayaKirim: Double?,
    @ColumnInfo(name = "Status_Kirim") val statusKirim: String?
)