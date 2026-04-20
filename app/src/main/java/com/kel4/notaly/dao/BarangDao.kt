package com.kel4.notaly.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kel4.notaly.model.Barang

@Dao
interface BarangDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun tambahBarang(barang: Barang)

    @Update
    suspend fun ubahBarang(barang: Barang)

    @Delete
    suspend fun hapusBarang(barang: Barang)

    @Query("SELECT * FROM barang")
    suspend fun ambilSemuaBarang(): List<Barang>

    @Query("SELECT * FROM barang WHERE ID_Barang = :id")
    suspend fun cariBarangBerdasarkanId(id: String): Barang?

    @Query("UPDATE barang SET Stok = Stok + :jumlah WHERE ID_Barang = :id")
    suspend fun tambahStok(id: String, jumlah: Int)
}