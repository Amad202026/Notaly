package com.kel4.notaly.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kel4.notaly.model.Supplier

@Dao
interface SupplierDao {

    // ── Create ────────────────────────────────────────────────
    @Insert
    suspend fun tambahSupplier(supplier: Supplier)

    // ── Read ──────────────────────────────────────────────────
    @Query("SELECT * FROM supplier")
    suspend fun ambilSemuaSupplier(): List<Supplier>

    @Query("SELECT * FROM supplier WHERE ID_Supplier = :id")
    suspend fun ambilSupplierById(id: Int): Supplier?

    @Query("SELECT ID_Supplier FROM supplier ORDER BY ID_Supplier ASC")
    suspend fun ambilSemuaIdSupplier(): List<Int>

    // ── Update ────────────────────────────────────────────────
    @Update
    suspend fun updateSupplier(supplier: Supplier)

    // ── Delete ────────────────────────────────────────────────
    @Delete
    suspend fun hapusSupplier(supplier: Supplier)
}