package com.kel4.notaly.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kel4.notaly.model.TransaksiPenjualan

@Dao
interface TransaksiPenjualanDao {
    @Insert
    suspend fun buatTransaksi(transaksi: TransaksiPenjualan)

    @Update
    suspend fun ubahStatusTransaksi(transaksi: TransaksiPenjualan)

    @Query("DELETE FROM transaksi_penjualan")  // ← was: DELETE FROM barang
    suspend fun hapusSemua()

    @Query("SELECT * FROM transaksi_penjualan ORDER BY Tanggal_Transaksi DESC")
    suspend fun ambilSemuaTransaksi(): List<TransaksiPenjualan>

    @Query("SELECT SUM(Total_Belanja) FROM transaksi_penjualan WHERE Status_Pembayaran = 'Lunas'")
    suspend fun hitungTotalPendapatan(): Int?

    @Query("SELECT * FROM transaksi_penjualan WHERE id_Transaksi = :id LIMIT 1")
    suspend fun cariTransaksiById(id: String): TransaksiPenjualan?
}