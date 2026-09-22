package com.salimshivani.inventoryscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Transaction?

    @Query(
        """
        SELECT t.id AS id, t.item_id AS item_id, t.direction AS direction, t.quantity AS quantity,
               t.timestamp AS timestamp, t.note AS note, a.name AS account_name
        FROM transactions t
        LEFT JOIN accounts a ON a.id = t.account_id
        WHERE t.item_id = :itemId
        ORDER BY t.timestamp DESC, t.id DESC
        LIMIT :limit
        """
    )
    suspend fun listForItem(itemId: Long, limit: Int = 100): List<TransactionWithAccount>

    @Query(
        """
        SELECT t.*, i.barcode AS barcode, i.name AS item_name, i.hsn_code AS hsn_code,
               a.name AS account_name, a.mobile_number AS account_mobile
        FROM transactions t
        JOIN items i ON i.id = t.item_id
        LEFT JOIN accounts a ON a.id = t.account_id
        ORDER BY t.timestamp DESC, t.id DESC
        """
    )
    suspend fun listAllWithItems(): List<TransactionWithItem>

    @Query(
        """
        SELECT date(t.timestamp) AS date, t.direction AS direction,
               SUM(t.quantity) AS total_quantity, COUNT(*) AS transaction_count
        FROM transactions t
        WHERE (:from IS NULL OR date(t.timestamp) >= date(:from))
          AND (:to IS NULL OR date(t.timestamp) <= date(:to))
        GROUP BY date(t.timestamp), t.direction
        ORDER BY date DESC, direction
        """
    )
    suspend fun datewiseReport(from: String?, to: String?): List<DatewiseRow>

    @Query(
        """
        SELECT i.id AS item_id, i.barcode AS barcode, i.name AS item_name, i.hsn_code AS hsn_code,
               COALESCE(SUM(CASE WHEN t.direction = 'inward' THEN t.quantity ELSE 0 END), 0) AS inward_total,
               COALESCE(SUM(CASE WHEN t.direction = 'outward' THEN t.quantity ELSE 0 END), 0) AS outward_total,
               COALESCE(SUM(CASE WHEN t.direction = 'inward' THEN t.quantity ELSE -t.quantity END), 0) AS net_stock
        FROM items i
        LEFT JOIN transactions t ON t.item_id = i.id
          AND (:from IS NULL OR date(t.timestamp) >= date(:from))
          AND (:to IS NULL OR date(t.timestamp) <= date(:to))
        GROUP BY i.id
        ORDER BY i.name COLLATE NOCASE
        """
    )
    suspend fun itemwiseReport(from: String?, to: String?): List<ItemwiseRow>

    @Query(
        """
        SELECT COALESCE(i.hsn_code, '(none)') AS hsn_code,
               COALESCE(SUM(CASE WHEN t.direction = 'inward' THEN t.quantity ELSE 0 END), 0) AS inward_total,
               COALESCE(SUM(CASE WHEN t.direction = 'outward' THEN t.quantity ELSE 0 END), 0) AS outward_total,
               COALESCE(SUM(CASE WHEN t.direction = 'inward' THEN t.quantity ELSE -t.quantity END), 0) AS net_stock,
               COUNT(DISTINCT i.id) AS item_count
        FROM items i
        LEFT JOIN transactions t ON t.item_id = i.id
          AND (:from IS NULL OR date(t.timestamp) >= date(:from))
          AND (:to IS NULL OR date(t.timestamp) <= date(:to))
        GROUP BY hsn_code
        ORDER BY hsn_code COLLATE NOCASE
        """
    )
    suspend fun hsnwiseReport(from: String?, to: String?): List<HsnwiseRow>

    @Query(
        """
        SELECT COALESCE(a.id, -1) AS account_id,
               COALESCE(a.name, '(no customer)') AS account_name,
               a.mobile_number AS mobile_number,
               COALESCE(SUM(CASE WHEN t.direction = 'inward' THEN t.quantity ELSE 0 END), 0) AS inward_total,
               COALESCE(SUM(CASE WHEN t.direction = 'outward' THEN t.quantity ELSE 0 END), 0) AS outward_total,
               COALESCE(SUM(CASE WHEN t.direction = 'inward' THEN t.quantity ELSE -t.quantity END), 0) AS net_stock,
               COUNT(*) AS transaction_count
        FROM transactions t
        LEFT JOIN accounts a ON a.id = t.account_id
        WHERE (:from IS NULL OR date(t.timestamp) >= date(:from))
          AND (:to IS NULL OR date(t.timestamp) <= date(:to))
        GROUP BY account_id
        ORDER BY account_name COLLATE NOCASE
        """
    )
    suspend fun accountwiseReport(from: String?, to: String?): List<AccountwiseRow>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)
}
