package com.salimshivani.inventoryscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE mobile_number = :mobile LIMIT 1")
    suspend fun getByMobile(mobile: String): Account?

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Account?

    @Query("SELECT * FROM accounts ORDER BY name COLLATE NOCASE")
    suspend fun listAll(): List<Account>

    @Insert
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<Account>)
}
