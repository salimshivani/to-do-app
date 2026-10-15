package com.salimshivani.inventoryscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(payment: Payment): Long

    @Query("SELECT * FROM payments WHERE account_id = :accountId ORDER BY payment_date DESC, id DESC")
    suspend fun listForAccount(accountId: Long): List<Payment>

    @Query("DELETE FROM payments")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<Payment>)
}
