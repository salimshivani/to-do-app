import type { SQLiteDatabase } from 'expo-sqlite';

// Bump when the schema changes; migrateDbIfNeeded runs the matching upgrade path.
export const DB_VERSION = 1;

export async function migrateDbIfNeeded(db: SQLiteDatabase) {
  const row = await db.getFirstAsync<{ user_version: number }>('PRAGMA user_version');
  const currentVersion = row?.user_version ?? 0;

  if (currentVersion >= DB_VERSION) {
    return;
  }

  await db.execAsync('PRAGMA journal_mode = WAL');

  if (currentVersion < 1) {
    await db.execAsync(`
      CREATE TABLE IF NOT EXISTS items (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        barcode TEXT NOT NULL UNIQUE,
        name TEXT NOT NULL,
        hsn_code TEXT,
        unit TEXT,
        created_at TEXT NOT NULL DEFAULT (datetime('now'))
      );

      CREATE TABLE IF NOT EXISTS transactions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE,
        direction TEXT NOT NULL CHECK (direction IN ('inward', 'outward')),
        quantity REAL NOT NULL DEFAULT 1,
        timestamp TEXT NOT NULL DEFAULT (datetime('now')),
        note TEXT
      );

      CREATE INDEX IF NOT EXISTS idx_transactions_item_id ON transactions(item_id);
      CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp);
      CREATE INDEX IF NOT EXISTS idx_items_hsn_code ON items(hsn_code);
    `);
  }

  await db.execAsync(`PRAGMA user_version = ${DB_VERSION}`);
}
