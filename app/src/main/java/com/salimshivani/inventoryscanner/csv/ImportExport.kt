package com.salimshivani.inventoryscanner.csv

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.salimshivani.inventoryscanner.data.Account
import com.salimshivani.inventoryscanner.data.AccountDao
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.data.BARCODE_SOURCE_SCANNED
import com.salimshivani.inventoryscanner.data.Bill
import com.salimshivani.inventoryscanner.data.BillDao
import com.salimshivani.inventoryscanner.data.BILL_STATUS_UNPAID
import com.salimshivani.inventoryscanner.data.Item
import com.salimshivani.inventoryscanner.data.ItemDao
import com.salimshivani.inventoryscanner.data.Payment
import com.salimshivani.inventoryscanner.data.PaymentDao
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

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (has(key) && !isNull(key)) getLong(key) else null

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) getDouble(key) else null

suspend fun exportItemsCsv(context: Context, itemDao: ItemDao): Uri {
    val items = itemDao.listAll()
    val csv = toCsv(
        listOf(
            "barcode", "name", "hsn_code", "unit", "price", "purchase_price", "mrp", "gst_percent",
            "unit2_name", "unit2_ratio", "unit2_price", "unit2_purchase_price",
            "unit3_name", "unit3_ratio", "unit3_price", "unit3_purchase_price"
        ),
        items.map {
            listOf(
                it.barcode, it.name, it.hsnCode, it.unit, it.price, it.purchasePrice, it.mrp, it.gstPercent,
                it.unit2Name, it.unit2Ratio, it.unit2Price, it.unit2PurchasePrice,
                it.unit3Name, it.unit3Ratio, it.unit3Price, it.unit3PurchasePrice
            )
        }
    )
    val file = File(exportsDir(context), "items-${timestampForFilename()}.csv")
    file.writeText(csv)
    return shareableUri(context, file)
}

suspend fun exportTransactionsCsv(context: Context, transactionDao: TransactionDao): Uri {
    val rows = transactionDao.listAllWithItems()
    val csv = toCsv(
        listOf("timestamp", "direction", "barcode", "item_name", "hsn_code", "quantity", "unit_label", "note", "customer_name", "customer_mobile"),
        rows.map {
            listOf(
                it.timestamp, it.direction, it.barcode, it.item_name, it.hsn_code, it.quantity, it.unit_label, it.note,
                it.account_name, it.account_mobile
            )
        }
    )
    val file = File(exportsDir(context), "transactions-${timestampForFilename()}.csv")
    file.writeText(csv)
    return shareableUri(context, file)
}

suspend fun exportBackupJson(
    context: Context,
    itemDao: ItemDao,
    transactionDao: TransactionDao,
    accountDao: AccountDao,
    billDao: BillDao,
    paymentDao: PaymentDao
): Uri {
    val items = itemDao.listAll()
    val accounts = accountDao.listAll()
    val transactionRows = transactionDao.listAllWithItems()
    val bills = billDao.listAllWithAccount()
    val payments = accounts.flatMap { paymentDao.listForAccount(it.id) }

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
                put("price", item.price)
                put("purchase_price", item.purchasePrice)
                put("mrp", item.mrp)
                put("gst_percent", item.gstPercent)
                put("unit2_name", item.unit2Name)
                put("unit2_ratio", item.unit2Ratio ?: JSONObject.NULL)
                put("unit2_price", item.unit2Price ?: JSONObject.NULL)
                put("unit2_purchase_price", item.unit2PurchasePrice ?: JSONObject.NULL)
                put("unit3_name", item.unit3Name)
                put("unit3_ratio", item.unit3Ratio ?: JSONObject.NULL)
                put("unit3_price", item.unit3Price ?: JSONObject.NULL)
                put("unit3_purchase_price", item.unit3PurchasePrice ?: JSONObject.NULL)
            }
        )
    }
    val accountsJson = JSONArray()
    accounts.forEach { account ->
        accountsJson.put(
            JSONObject().apply {
                put("id", account.id)
                put("mobile_number", account.mobileNumber)
                put("name", account.name)
                put("address", account.address)
                put("created_at", account.createdAt)
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
                put("account_id", t.account_id ?: JSONObject.NULL)
                put("bill_id", t.bill_id ?: JSONObject.NULL)
                put("rate", t.rate)
                put("gst_percent", t.gst_percent)
                put("tax_amount", t.tax_amount)
                put("line_total", t.line_total)
                put("unit_label", t.unit_label)
                put("quantity_base", t.quantity_base)
            }
        )
    }
    val paymentsJson = JSONArray()
    payments.forEach { p ->
        paymentsJson.put(
            JSONObject().apply {
                put("id", p.id)
                put("account_id", p.accountId)
                put("direction", p.direction)
                put("amount", p.amount)
                put("payment_date", p.paymentDate)
                put("note", p.note)
            }
        )
    }
    val billsJson = JSONArray()
    bills.forEach { b ->
        billsJson.put(
            JSONObject().apply {
                put("id", b.id)
                put("account_id", b.account_id)
                put("direction", b.direction)
                put("total_amount", b.total_amount)
                put("status", b.status)
                put("bill_date", b.bill_date)
                put("payment_id", b.payment_id ?: JSONObject.NULL)
                put("subtotal", b.subtotal)
                put("tax_amount", b.tax_amount)
            }
        )
    }
    val backup = JSONObject().apply {
        put("version", 6)
        put("exported_at", Instant.now().toString())
        put("items", itemsJson)
        put("accounts", accountsJson)
        put("transactions", transactionsJson)
        put("payments", paymentsJson)
        put("bills", billsJson)
    }
    val file = File(exportsDir(context), "inventory-backup-${timestampForFilename()}.bkp")
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
        val price = row["price"]?.trim()?.toDoubleOrNull() ?: 0.0
        val purchasePrice = row["purchase_price"]?.trim()?.toDoubleOrNull() ?: 0.0
        val mrp = row["mrp"]?.trim()?.toDoubleOrNull() ?: 0.0
        val gstPercent = row["gst_percent"]?.trim()?.toDoubleOrNull() ?: 0.0
        val unit2Name = row["unit2_name"]?.trim()?.ifEmpty { null }
        val unit2Ratio = row["unit2_ratio"]?.trim()?.toDoubleOrNull()
        val unit2Price = row["unit2_price"]?.trim()?.toDoubleOrNull()
        val unit2PurchasePrice = row["unit2_purchase_price"]?.trim()?.toDoubleOrNull()
        val unit3Name = row["unit3_name"]?.trim()?.ifEmpty { null }
        val unit3Ratio = row["unit3_ratio"]?.trim()?.toDoubleOrNull()
        val unit3Price = row["unit3_price"]?.trim()?.toDoubleOrNull()
        val unit3PurchasePrice = row["unit3_purchase_price"]?.trim()?.toDoubleOrNull()
        val existing = itemDao.getByBarcode(barcode)
        if (existing != null) {
            itemDao.update(
                existing.copy(
                    name = name, hsnCode = hsnCode, unit = unit, price = price,
                    purchasePrice = purchasePrice, mrp = mrp, gstPercent = gstPercent,
                    unit2Name = unit2Name, unit2Ratio = unit2Ratio,
                    unit2Price = unit2Price, unit2PurchasePrice = unit2PurchasePrice,
                    unit3Name = unit3Name, unit3Ratio = unit3Ratio,
                    unit3Price = unit3Price, unit3PurchasePrice = unit3PurchasePrice
                )
            )
            updated++
        } else {
            itemDao.insert(
                Item(
                    barcode = barcode,
                    name = name,
                    hsnCode = hsnCode,
                    unit = unit,
                    createdAt = Instant.now().toString(),
                    barcodeSource = BARCODE_SOURCE_SCANNED,
                    price = price,
                    purchasePrice = purchasePrice,
                    mrp = mrp,
                    gstPercent = gstPercent,
                    unit2Name = unit2Name,
                    unit2Ratio = unit2Ratio,
                    unit2Price = unit2Price,
                    unit2PurchasePrice = unit2PurchasePrice,
                    unit3Name = unit3Name,
                    unit3Ratio = unit3Ratio,
                    unit3Price = unit3Price,
                    unit3PurchasePrice = unit3PurchasePrice
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
    val accountsJson = root.optJSONArray("accounts") ?: JSONArray()
    val paymentsJson = root.optJSONArray("payments") ?: JSONArray()
    val billsJson = root.optJSONArray("bills") ?: JSONArray()

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
            labelPrintedAt = o.optString("label_printed_at").ifEmpty { null },
            price = if (o.has("price")) o.getDouble("price") else 0.0,
            purchasePrice = if (o.has("purchase_price")) o.getDouble("purchase_price") else 0.0,
            mrp = if (o.has("mrp")) o.getDouble("mrp") else 0.0,
            gstPercent = if (o.has("gst_percent")) o.getDouble("gst_percent") else 0.0,
            unit2Name = o.optString("unit2_name").ifEmpty { null },
            unit2Ratio = o.optDoubleOrNull("unit2_ratio"),
            unit2Price = o.optDoubleOrNull("unit2_price"),
            unit2PurchasePrice = o.optDoubleOrNull("unit2_purchase_price"),
            unit3Name = o.optString("unit3_name").ifEmpty { null },
            unit3Ratio = o.optDoubleOrNull("unit3_ratio"),
            unit3Price = o.optDoubleOrNull("unit3_price"),
            unit3PurchasePrice = o.optDoubleOrNull("unit3_purchase_price")
        )
    }
    val accounts = (0 until accountsJson.length()).map { i ->
        val o = accountsJson.getJSONObject(i)
        Account(
            id = o.getLong("id"),
            mobileNumber = o.optString("mobile_number").ifEmpty { null },
            name = o.getString("name"),
            address = o.optString("address").ifEmpty { null },
            createdAt = o.getString("created_at")
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
            note = o.optString("note").ifEmpty { null },
            accountId = o.optLongOrNull("account_id"),
            billId = o.optLongOrNull("bill_id"),
            rate = if (o.has("rate")) o.getDouble("rate") else 0.0,
            gstPercent = if (o.has("gst_percent")) o.getDouble("gst_percent") else 0.0,
            taxAmount = if (o.has("tax_amount")) o.getDouble("tax_amount") else 0.0,
            lineTotal = if (o.has("line_total")) o.getDouble("line_total") else 0.0,
            unitLabel = o.optString("unit_label").ifEmpty { null },
            quantityBase = if (o.has("quantity_base")) o.getDouble("quantity_base") else o.getDouble("quantity")
        )
    }
    val payments = (0 until paymentsJson.length()).map { i ->
        val o = paymentsJson.getJSONObject(i)
        Payment(
            id = o.getLong("id"),
            accountId = o.getLong("account_id"),
            direction = o.getString("direction"),
            amount = o.getDouble("amount"),
            paymentDate = o.getString("payment_date"),
            note = o.optString("note").ifEmpty { null }
        )
    }
    val bills = (0 until billsJson.length()).map { i ->
        val o = billsJson.getJSONObject(i)
        Bill(
            id = o.getLong("id"),
            accountId = o.getLong("account_id"),
            direction = o.getString("direction"),
            totalAmount = o.getDouble("total_amount"),
            status = o.optString("status", BILL_STATUS_UNPAID),
            billDate = o.getString("bill_date"),
            paymentId = o.optLongOrNull("payment_id"),
            subtotal = if (o.has("subtotal")) o.getDouble("subtotal") else 0.0,
            taxAmount = if (o.has("tax_amount")) o.getDouble("tax_amount") else 0.0
        )
    }
    db.replaceAllData(items, transactions, accounts, bills, payments)
}
