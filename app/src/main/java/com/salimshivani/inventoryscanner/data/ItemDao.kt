package com.salimshivani.inventoryscanner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): Item?

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Item?

    @Query("SELECT * FROM items ORDER BY name COLLATE NOCASE")
    suspend fun listAll(): List<Item>

    @Query(
        "SELECT * FROM items WHERE barcode_source = '$BARCODE_SOURCE_GENERATED' AND label_printed_at IS NULL " +
            "ORDER BY created_at DESC"
    )
    suspend fun listPendingLabels(): List<Item>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("UPDATE items SET label_printed_at = :printedAt WHERE id = :id")
    suspend fun markLabelPrinted(id: Long, printedAt: String)

    @Query("SELECT MAX(CAST(SUBSTR(barcode, :prefixLength) AS INTEGER)) FROM items " +
        "WHERE barcode_source = '$BARCODE_SOURCE_GENERATED' AND barcode LIKE :likePattern")
    suspend fun maxGeneratedBarcodeSequence(prefixLength: Int, likePattern: String): Int?

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Item>)
}
