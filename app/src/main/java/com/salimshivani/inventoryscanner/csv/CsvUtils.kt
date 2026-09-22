package com.salimshivani.inventoryscanner.csv

// Minimal RFC4180-ish CSV encode/decode — no external dependency needed for our flat row shapes.

fun toCsv(headers: List<String>, rows: List<List<Any?>>): String {
    fun escapeCell(cell: Any?): String {
        val str = cell?.toString() ?: ""
        return if (Regex("[\",\n\r]").containsMatchIn(str)) {
            "\"" + str.replace("\"", "\"\"") + "\""
        } else str
    }
    val lines = mutableListOf(headers.joinToString(",") { escapeCell(it) })
    for (row in rows) {
        lines.add(row.joinToString(",") { escapeCell(it) })
    }
    return lines.joinToString("\r\n")
}

fun parseCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false

    fun pushField() {
        row.add(field.toString())
        field.clear()
    }

    fun pushRow() {
        pushField()
        rows.add(row)
        row = mutableListOf()
    }

    var i = 0
    while (i < text.length) {
        val char = text[i]
        if (inQuotes) {
            if (char == '"') {
                if (i + 1 < text.length && text[i + 1] == '"') {
                    field.append('"')
                    i++
                } else {
                    inQuotes = false
                }
            } else {
                field.append(char)
            }
        } else {
            when (char) {
                '"' -> inQuotes = true
                ',' -> pushField()
                '\n' -> pushRow()
                '\r' -> { /* ignore, \n handles the row break */ }
                else -> field.append(char)
            }
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) {
        pushRow()
    }
    return rows.filter { !(it.size == 1 && it[0].isEmpty()) }
}

fun csvRowsToObjects(rows: List<List<String>>): List<Map<String, String>> {
    if (rows.isEmpty()) return emptyList()
    val header = rows.first().map { it.trim().lowercase() }
    return rows.drop(1).map { r ->
        header.mapIndexed { idx, key -> key to (r.getOrNull(idx)?.trim() ?: "") }.toMap()
    }
}
