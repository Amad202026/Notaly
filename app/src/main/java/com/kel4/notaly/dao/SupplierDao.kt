package com.kel4.notaly.dao

import com.kel4.notaly.model.Supplier
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SupplierDao {
    @Insert
    suspend fun tambahSupplier(supplier: Supplier)

    @Query("SELECT * FROM supplier")
    suspend fun ambilSemuaSupplier(): List<Supplier>

    @Delete
    suspend fun hapusSupplier(supplier: Supplier)
}