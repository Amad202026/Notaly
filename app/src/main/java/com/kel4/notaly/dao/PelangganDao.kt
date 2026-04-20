package com.kel4.notaly.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kel4.notaly.model.Pelanggan

@Dao
interface PelangganDao {
    @Insert
    suspend fun tambahPelanggan(pelanggan: Pelanggan)

    @Update
    suspend fun ubahPelanggan(pelanggan: Pelanggan)

    @Delete
    suspend fun hapusPelanggan(pelanggan: Pelanggan)

    @Query("SELECT * FROM pelanggan")
    suspend fun ambilSemuaPelanggan(): List<Pelanggan>

    @Query("SELECT * FROM pelanggan WHERE Nama_Pelanggan LIKE '%' || :keyword || '%'")
    suspend fun cariPelanggan(keyword: String): List<Pelanggan>
}