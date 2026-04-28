package com.kel4.notaly.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kel4.notaly.model.Pengiriman

@Dao
interface PengirimanDao {

    // ── FUNGSI ASLI (tidak diubah) ──────────────────────────────────────────

    @Insert
    suspend fun buatPengiriman(pengiriman: Pengiriman)

    @Query("DELETE FROM pengiriman")  // ← was: DELETE FROM barang
    suspend fun hapusSemua()

    @Update
    suspend fun updateStatusPengiriman(pengiriman: Pengiriman)

    @Query("SELECT * FROM pengiriman WHERE ID_Transaksi = :idTransaksi")
    suspend fun cekPengirimanTransaksi(idTransaksi: String): Pengiriman?

    @Query("SELECT * FROM pengiriman WHERE Status_Kirim = 'Diproses'")
    suspend fun ambilPengirimanMenunggu(): List<Pengiriman>

    // ── FUNGSI TAMBAHAN UNTUK CRUD ──────────────────────────────────────────

    @Query("SELECT * FROM pengiriman ORDER BY ID_Pengiriman DESC")
    suspend fun getAllPengiriman(): List<Pengiriman>

    @Query("SELECT * FROM pengiriman WHERE ID_Pengiriman = :id")
    suspend fun getPengirimanById(id: Int): Pengiriman?

    @Query("DELETE FROM pengiriman WHERE ID_Pengiriman = :id")
    suspend fun deletePengirimanById(id: Int)
}