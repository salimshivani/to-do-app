import type { SQLiteDatabase } from 'expo-sqlite';
import type {
  BarcodeSource,
  DatewiseRow,
  Direction,
  HsnwiseRow,
  Item,
  ItemwiseRow,
  Transaction,
  TransactionWithItem,
} from '../types';

export async function getItemByBarcode(db: SQLiteDatabase, barcode: string): Promise<Item | null> {
  const row = await db.getFirstAsync<Item>('SELECT * FROM items WHERE barcode = ?', barcode);
  return row ?? null;
}

export async function getItemById(db: SQLiteDatabase, id: number): Promise<Item | null> {
  const row = await db.getFirstAsync<Item>('SELECT * FROM items WHERE id = ?', id);
  return row ?? null;
}

export async function listItems(db: SQLiteDatabase): Promise<Item[]> {
  return db.getAllAsync<Item>('SELECT * FROM items ORDER BY name COLLATE NOCASE');
}

export async function createItem(
  db: SQLiteDatabase,
  input: {
    barcode: string;
    name: string;
    hsn_code?: string | null;
    unit?: string | null;
    barcode_source?: BarcodeSource;
  }
): Promise<Item> {
  const result = await db.runAsync(
    'INSERT INTO items (barcode, name, hsn_code, unit, barcode_source) VALUES (?, ?, ?, ?, ?)',
    input.barcode.trim(),
    input.name.trim(),
    input.hsn_code?.trim() || null,
    input.unit?.trim() || null,
    input.barcode_source ?? 'scanned'
  );
  const created = await getItemById(db, result.lastInsertRowId);
  if (!created) throw new Error('Failed to create item');
  return created;
}

export async function updateItem(
  db: SQLiteDatabase,
  id: number,
  input: { name: string; hsn_code?: string | null; unit?: string | null }
): Promise<void> {
  await db.runAsync(
    'UPDATE items SET name = ?, hsn_code = ?, unit = ? WHERE id = ?',
    input.name.trim(),
    input.hsn_code?.trim() || null,
    input.unit?.trim() || null,
    id
  );
}

// Insert-or-update by barcode, used by both the scan flow and CSV import.
export async function upsertItemByBarcode(
  db: SQLiteDatabase,
  input: { barcode: string; name: string; hsn_code?: string | null; unit?: string | null }
): Promise<Item> {
  const existing = await getItemByBarcode(db, input.barcode);
  if (existing) {
    await updateItem(db, existing.id, input);
    const updated = await getItemById(db, existing.id);
    if (!updated) throw new Error('Failed to update item');
    return updated;
  }
  return createItem(db, input);
}

export async function listItemsPendingLabels(db: SQLiteDatabase): Promise<Item[]> {
  return db.getAllAsync<Item>(
    `SELECT * FROM items WHERE barcode_source = 'generated' AND label_printed_at IS NULL
     ORDER BY created_at DESC`
  );
}

export async function markLabelPrinted(db: SQLiteDatabase, id: number): Promise<void> {
  await db.runAsync("UPDATE items SET label_printed_at = datetime('now') WHERE id = ?", id);
}

export async function deleteItem(db: SQLiteDatabase, id: number): Promise<void> {
  await db.runAsync('DELETE FROM items WHERE id = ?', id);
}

export async function recordTransaction(
  db: SQLiteDatabase,
  input: { item_id: number; direction: Direction; quantity: number; note?: string | null }
): Promise<Transaction> {
  const result = await db.runAsync(
    'INSERT INTO transactions (item_id, direction, quantity, note) VALUES (?, ?, ?, ?)',
    input.item_id,
    input.direction,
    input.quantity,
    input.note?.trim() || null
  );
  const created = await db.getFirstAsync<Transaction>(
    'SELECT * FROM transactions WHERE id = ?',
    result.lastInsertRowId
  );
  if (!created) throw new Error('Failed to record transaction');
  return created;
}

export async function listTransactionsWithItems(
  db: SQLiteDatabase,
  limit = 50
): Promise<TransactionWithItem[]> {
  return db.getAllAsync<TransactionWithItem>(
    `SELECT t.*, i.barcode AS barcode, i.name AS item_name, i.hsn_code AS hsn_code
     FROM transactions t
     JOIN items i ON i.id = t.item_id
     ORDER BY t.timestamp DESC, t.id DESC
     LIMIT ?`,
    limit
  );
}

export async function listTransactionsForItem(
  db: SQLiteDatabase,
  itemId: number,
  limit = 100
): Promise<Transaction[]> {
  return db.getAllAsync<Transaction>(
    'SELECT * FROM transactions WHERE item_id = ? ORDER BY timestamp DESC, id DESC LIMIT ?',
    itemId,
    limit
  );
}

export async function listAllTransactionsWithItems(
  db: SQLiteDatabase
): Promise<TransactionWithItem[]> {
  return db.getAllAsync<TransactionWithItem>(
    `SELECT t.*, i.barcode AS barcode, i.name AS item_name, i.hsn_code AS hsn_code
     FROM transactions t
     JOIN items i ON i.id = t.item_id
     ORDER BY t.timestamp DESC, t.id DESC`
  );
}

export async function getDatewiseReport(
  db: SQLiteDatabase,
  input: { from?: string; to?: string } = {}
): Promise<DatewiseRow[]> {
  const conditions: string[] = [];
  const params: (string | number)[] = [];
  if (input.from) {
    conditions.push('date(t.timestamp) >= date(?)');
    params.push(input.from);
  }
  if (input.to) {
    conditions.push('date(t.timestamp) <= date(?)');
    params.push(input.to);
  }
  const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
  return db.getAllAsync<DatewiseRow>(
    `SELECT date(t.timestamp) AS date, t.direction AS direction,
            SUM(t.quantity) AS total_quantity, COUNT(*) AS transaction_count
     FROM transactions t
     ${where}
     GROUP BY date(t.timestamp), t.direction
     ORDER BY date DESC, direction`,
    ...params
  );
}

export async function getItemwiseReport(
  db: SQLiteDatabase,
  input: { from?: string; to?: string } = {}
): Promise<ItemwiseRow[]> {
  const conditions: string[] = [];
  const params: (string | number)[] = [];
  if (input.from) {
    conditions.push('date(t.timestamp) >= date(?)');
    params.push(input.from);
  }
  if (input.to) {
    conditions.push('date(t.timestamp) <= date(?)');
    params.push(input.to);
  }
  const where = conditions.length ? `AND ${conditions.join(' AND ')}` : '';
  return db.getAllAsync<ItemwiseRow>(
    `SELECT i.id AS item_id, i.barcode AS barcode, i.name AS item_name, i.hsn_code AS hsn_code,
            COALESCE(SUM(CASE WHEN t.direction = 'inward' THEN t.quantity ELSE 0 END), 0) AS inward_total,
            COALESCE(SUM(CASE WHEN t.direction = 'outward' THEN t.quantity ELSE 0 END), 0) AS outward_total,
            COALESCE(SUM(CASE WHEN t.direction = 'inward' THEN t.quantity ELSE -t.quantity END), 0) AS net_stock
     FROM items i
     LEFT JOIN transactions t ON t.item_id = i.id ${where}
     GROUP BY i.id
     ORDER BY i.name COLLATE NOCASE`,
    ...params
  );
}

export async function getHsnwiseReport(
  db: SQLiteDatabase,
  input: { from?: string; to?: string } = {}
): Promise<HsnwiseRow[]> {
  const conditions: string[] = [];
  const params: (string | number)[] = [];
  if (input.from) {
    conditions.push('date(t.timestamp) >= date(?)');
    params.push(input.from);
  }
  if (input.to) {
    conditions.push('date(t.timestamp) <= date(?)');
    params.push(input.to);
  }
  const where = conditions.length ? `AND ${conditions.join(' AND ')}` : '';
  return db.getAllAsync<HsnwiseRow>(
    `SELECT COALESCE(i.hsn_code, '(none)') AS hsn_code,
            COALESCE(SUM(CASE WHEN t.direction = 'inward' THEN t.quantity ELSE 0 END), 0) AS inward_total,
            COALESCE(SUM(CASE WHEN t.direction = 'outward' THEN t.quantity ELSE 0 END), 0) AS outward_total,
            COALESCE(SUM(CASE WHEN t.direction = 'inward' THEN t.quantity ELSE -t.quantity END), 0) AS net_stock,
            COUNT(DISTINCT i.id) AS item_count
     FROM items i
     LEFT JOIN transactions t ON t.item_id = i.id ${where}
     GROUP BY hsn_code
     ORDER BY hsn_code COLLATE NOCASE`,
    ...params
  );
}

export async function replaceAllData(
  db: SQLiteDatabase,
  data: { items: Item[]; transactions: Transaction[] }
): Promise<void> {
  await db.withTransactionAsync(async () => {
    await db.execAsync('DELETE FROM transactions; DELETE FROM items;');
    for (const item of data.items) {
      await db.runAsync(
        `INSERT INTO items (id, barcode, name, hsn_code, unit, created_at, barcode_source, label_printed_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
        item.id,
        item.barcode,
        item.name,
        item.hsn_code,
        item.unit,
        item.created_at,
        item.barcode_source ?? 'scanned',
        item.label_printed_at ?? null
      );
    }
    for (const t of data.transactions) {
      await db.runAsync(
        'INSERT INTO transactions (id, item_id, direction, quantity, timestamp, note) VALUES (?, ?, ?, ?, ?, ?)',
        t.id,
        t.item_id,
        t.direction,
        t.quantity,
        t.timestamp,
        t.note
      );
    }
  });
}
