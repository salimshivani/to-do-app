package com.salimshivani.inventoryscanner.csv

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.data.BARCODE_SOURCE_SCANNED
import com.salimshivani.inventoryscanner.data.Item
import com.salimshivani.inventoryscanner.data.ItemDao
import com.salimshivani.inventoryscanner.data.Transaction
import com.salimshivani.inventoryscanner.data.TransactionDao
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant

data class ImportItemsResult(val created: Int, val updated: Int, val skipped: Int)

private fun exportsDir(context: Context): File =
    File(context.cacheDir, "exports").apply { mkdirs() }

private fun shareableUri(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

private fun timestampForFilename(): String = Instant.now().toString().replace(Regex("[:.]"), "-")

suspend fun exportItemsCsv(context: Context, itemDao: ItemDao): Uri {
    val items = itemDao.listAll()
    val csv = toCsv(
        listOf("barcode", "name", "hsn_code", "unit"),
        items.map { listOf(it.barcode, it.name, it.hsnCode, it.unit) }
    )
    val file = File(exportsDir(context), "items-${timestampForFilename()}.csv")
    file.writeText(csv)
    return shareableUri(context, file)
}

suspend fun exportTransactionsCsv(context: Context, transactionDao: TransactionDao): Uri {
    val rows = transactionDao.listAllWithItems()
    val csv = toCsv(
        listOf("timestamp", "direction", "barcode", "item_name", "hsn_code", "quantity", "note"),
        rows.map { listOf(it.timestamp, it.direction, it.barcode, it.item_name, it.hsn_code, it.quantity, it.note) }
    )
    val file = File(exportsDir(context), "transactions-${timestampForFilename()}.csv")
    file.writeText(csv)
    return shareableUri(context, file)
}

suspend fun exportBackupJson(context: Context, itemDao: ItemDao, transactionDao: TransactionDao): Uri {
    val items = itemDao.listAll()
    val transactionRows = transactionDao.listAllWithItems()

    val itemsJson = JSONArray()
    items.forEach { item ->
        itemsJson.put(
            JSONObject().apply {
                put("id", item.id)
                put("barcode", item.barcode)
                put("name", item.name)
                put("hsn_code", item.hsnCode)
                put("unit", item.unit)
                put("created_at", item.createdAt)
                put("barcode_source", item.barcodeSource)
                put("label_printed_at", item.labelPrintedAt)
            }
        )
    }
    val transactionsJson = JSONArray()
    transactionRows.forEach { t ->
        transactionsJson.put(
            JSONObject().apply {
                put("id", t.id)
                put("item_id", t.item_id)
                put("direction", t.direction)
                put("quantity", t.quantity)
                put("timestamp", t.timestamp)
                put("note", t.note)
            }
        )
    }
    val backup = JSONObject().apply {
        put("version", 1)
        put("exported_at", Instant.now().toString())
        put("items", itemsJson)
        put("transactions", transactionsJson)
    }
    val file = File(exportsDir(context), "inventory-backup-${timestampForFilename()}.json")
    file.writeText(backup.toString(2))
    return shareableUri(context, file)
}

suspend fun importItemsCsv(context: Context, itemDao: ItemDao, uri: Uri): ImportItemsResult {
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        ?: throw IllegalStateException("Could not read file")
    val rows = csvRowsToObjects(parseCsv(text))

    var created = 0
    var updated = 0
    var skipped = 0
    val existingBarcodes = itemDao.listAll().map { it.barcode }.toMutableSet()

    for (row in rows) {
        val barcode = row["barcode"]?.trim().orEmpty()
        val name = row["name"]?.trim().orEmpty()
        if (barcode.isEmpty() || name.isEmpty()) {
            skipped++
            continue
        }
        val hsnCode = row["hsn_code"]?.trim()?.ifEmpty { null }
        val unit = row["unit"]?.trim()?.ifEmpty { null }
        val existing = itemDao.getByBarcode(barcode)
        if (existing != null) {
            itemDao.update(existing.copy(name = name, hsnCode = hsnCode, unit = unit))
            updated++
        } else {
            itemDao.insert(
                Item(
                    barcode = barcode,
                    name = name,
                    hsnCode = hsnCode,
                    unit = unit,
                    createdAt = Instant.now().toString(),
                    barcodeSource = BARCODE_SOURCE_SCANNED
                )
            )
            created++
            existingBarcodes.add(barcode)
        }
    }
    return ImportItemsResult(created, updated, skipped)
}

suspend fun importBackupJson(context: Context, db: AppDatabase, uri: Uri) {
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        ?: throw IllegalStateException("Could not read file")
    val root = JSONObject(text)
    val itemsJson = root.getJSONArray("items")
    val transactionsJson = root.getJSONArray("transactions")

    val items = (0 until itemsJson.length()).map { i ->
        val o = itemsJson.getJSONObject(i)
        Item(
            id = o.getLong("id"),
            barcode = o.getString("barcode"),
            name = o.getString("name"),
            hsnCode = o.optString("hsn_code").ifEmpty { null },
            unit = o.optString("unit").ifEmpty { null },
            createdAt = o.getString("created_at"),
            barcodeSource = o.optString("barcode_source", BARCODE_SOURCE_SCANNED),
            labelPrintedAt = o.optString("label_printed_at").ifEmpty { null }
        )
    }
    val transactions = (0 until transactionsJson.length()).map { i ->
        val o = transactionsJson.getJSONObject(i)
        Transaction(
            id = o.getLong("id"),
            itemId = o.getLong("item_id"),
            direction = o.getString("direction"),
            quantity = o.getDouble("quantity"),
            timestamp = o.getString("timestamp"),
            note = o.optString("note").ifEmpty { null }
        )
    }
    db.replaceAllData(items, transactions)
}
