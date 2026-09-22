package com.salimshivani.inventoryscanner.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts", indices = [Index("mobile_number", unique = true)])
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "mobile_number") val mobileNumber: String?,
    val name: String,
    val address: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String
)
