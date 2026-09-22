package com.salimshivani.inventoryscanner.ui

import org.json.JSONArray
import org.json.JSONObject

// Carries the scanned-but-not-yet-saved items from ScanActivity to AccountCaptureActivity,
// where they're all committed together once the customer is captured.
data class PendingLineItem(
    val itemId: Long,
    val itemName: String,
    val quantity: Double,
    val note: String?
)

fun List<PendingLineItem>.toJson(): String {
    val array = JSONArray()
    forEach { line ->
        array.put(
            JSONObject().apply {
                put("itemId", line.itemId)
                put("itemName", line.itemName)
                put("quantity", line.quantity)
                put("note", line.note)
            }
        )
    }
    return array.toString()
}

fun parsePendingLineItems(json: String): List<PendingLineItem> {
    val array = JSONArray(json)
    return (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        PendingLineItem(
            itemId = o.getLong("itemId"),
            itemName = o.getString("itemName"),
            quantity = o.getDouble("quantity"),
            note = o.optString("note").ifEmpty { null }
        )
    }
}
