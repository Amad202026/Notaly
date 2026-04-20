package com.kel4.notaly.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kel4.notaly.model.Pengiriman

@Dao
interface PengirimanDao {
    @Insert
    suspend fun buatPengiriman(pengiriman: Pengiriman)

    @Update
    suspend fun updateStatusPengiriman(pengiriman: Pengiriman)

    @Query("SELECT * FROM pengiriman WHERE ID_Transaksi = :idTransaksi")
    suspend fun cekPengirimanTransaksi(idTransaksi: String): Pengiriman?

    @Query("SELECT * FROM pengiriman WHERE Status_Kirim = 'Diproses'")
    suspend fun ambilPengirimanMenunggu(): List<Pengiriman>
}