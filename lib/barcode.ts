import type { SQLiteDatabase } from 'expo-sqlite';

// Internally generated barcodes use this prefix so they're easy to tell apart
// from real, scanned manufacturer barcodes at a glance (in lists, reports, exports).
export const GENERATED_BARCODE_PREFIX = 'INT-';

// Sequential rather than random so printed labels stay short and easy to read/type as a fallback.
export async function generateInternalBarcode(db: SQLiteDatabase): Promise<string> {
  const row = await db.getFirstAsync<{ max_seq: number | null }>(
    `SELECT MAX(CAST(SUBSTR(barcode, ?) AS INTEGER)) AS max_seq
     FROM items
     WHERE barcode_source = 'generated' AND barcode LIKE ? `,
    GENERATED_BARCODE_PREFIX.length + 1,
    `${GENERATED_BARCODE_PREFIX}%`
  );
  const next = (row?.max_seq ?? 0) + 1;
  return `${GENERATED_BARCODE_PREFIX}${String(next).padStart(6, '0')}`;
}
