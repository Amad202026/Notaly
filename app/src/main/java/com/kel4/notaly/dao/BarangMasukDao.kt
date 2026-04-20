package com.kel4.notaly.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kel4.notaly.model.BarangMasuk

@Dao
interface BarangMasukDao {
    @Insert
    suspend fun catatRestok(barangMasuk: BarangMasuk)

    @Query("SELECT * FROM barang_masuk ORDER BY Tanggal_Masuk DESC")
    suspend fun riwayatBarangMasuk(): List<BarangMasuk>

    @Query("SELECT * FROM barang_masuk WHERE ID_Barang = :idBarang")
    suspend fun riwayatRestokSatuBarang(idBarang: String): List<BarangMasuk>
}