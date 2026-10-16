package com.salimshivani.inventoryscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BillDao {
    @Insert
    suspend fun insert(bill: Bill): Long

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Bill?

    @Query("SELECT * FROM bills WHERE account_id = :accountId ORDER BY bill_date DESC, id DESC")
    suspend fun listForAccount(accountId: Long): List<Bill>

    @Query(
        "SELECT * FROM bills WHERE account_id = :accountId AND direction = :direction AND status = '$BILL_STATUS_UNPAID' " +
            "ORDER BY bill_date"
    )
    suspend fun listUnpaidForAccountAndDirection(accountId: Long, direction: String): List<Bill>

    @Query(
        """
        SELECT b.*, a.name AS account_name, a.mobile_number AS account_mobile
        FROM bills b
        JOIN accounts a ON a.id = b.account_id
        ORDER BY b.bill_date DESC, b.id DESC
        """
    )
    suspend fun listAllWithAccount(): List<BillWithAccount>

    @Query("UPDATE bills SET status = '$BILL_STATUS_PAID', payment_id = :paymentId WHERE id IN (:billIds)")
    suspend fun markPaid(billIds: List<Long>, paymentId: Long)

    @Query("DELETE FROM bills")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bills: List<Bill>)
}
