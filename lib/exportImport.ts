import * as DocumentPicker from 'expo-document-picker';
import { Directory, File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import type { SQLiteDatabase } from 'expo-sqlite';

import {
  listAllTransactionsWithItems,
  listItems,
  replaceAllData,
  upsertItemByBarcode,
} from '../db/queries';
import { csvRowsToObjects, parseCsv, toCsv } from './csv';
import type { Item, Transaction } from '../types';

const EXPORT_DIR = new Directory(Paths.cache, 'inventory-exports');

function ensureExportDir() {
  if (!EXPORT_DIR.exists) {
    EXPORT_DIR.create({ intermediates: true });
  }
}

function timestampForFilename() {
  return new Date().toISOString().replace(/[:.]/g, '-');
}

async function shareFile(file: File) {
  if (await Sharing.isAvailableAsync()) {
    await Sharing.shareAsync(file.uri);
  }
}

export async function exportItemsCsv(db: SQLiteDatabase): Promise<string> {
  const items = await listItems(db);
  const csv = toCsv(
    ['barcode', 'name', 'hsn_code', 'unit'],
    items.map((i) => [i.barcode, i.name, i.hsn_code, i.unit])
  );
  ensureExportDir();
  const file = new File(EXPORT_DIR, `items-${timestampForFilename()}.csv`);
  file.create({ overwrite: true });
  file.write(csv);
  await shareFile(file);
  return file.uri;
}

export async function exportTransactionsCsv(db: SQLiteDatabase): Promise<string> {
  const rows = await listAllTransactionsWithItems(db);
  const csv = toCsv(
    ['timestamp', 'direction', 'barcode', 'item_name', 'hsn_code', 'quantity', 'note'],
    rows.map((r) => [r.timestamp, r.direction, r.barcode, r.item_name, r.hsn_code, r.quantity, r.note])
  );
  ensureExportDir();
  const file = new File(EXPORT_DIR, `transactions-${timestampForFilename()}.csv`);
  file.create({ overwrite: true });
  file.write(csv);
  await shareFile(file);
  return file.uri;
}

export async function exportBackupJson(db: SQLiteDatabase): Promise<string> {
  const items = await listItems(db);
  const rows = await listAllTransactionsWithItems(db);
  const transactions: Transaction[] = rows.map((r) => ({
    id: r.id,
    item_id: r.item_id,
    direction: r.direction,
    quantity: r.quantity,
    timestamp: r.timestamp,
    note: r.note,
  }));
  const backup = { version: 1, exported_at: new Date().toISOString(), items, transactions };
  ensureExportDir();
  const file = new File(EXPORT_DIR, `inventory-backup-${timestampForFilename()}.json`);
  file.create({ overwrite: true });
  file.write(JSON.stringify(backup, null, 2));
  await shareFile(file);
  return file.uri;
}

export interface ImportItemsResult {
  created: number;
  updated: number;
  skipped: number;
}

export async function importItemsCsv(db: SQLiteDatabase): Promise<ImportItemsResult | null> {
  const picked = await DocumentPicker.getDocumentAsync({
    type: ['text/csv', 'text/comma-separated-values', 'public.comma-separated-values-text', '*/*'],
    copyToCacheDirectory: true,
  });
  if (picked.canceled || picked.assets.length === 0) return null;

  const file = new File(picked.assets[0].uri);
  const text = await file.text();
  const rows = csvRowsToObjects(parseCsv(text));

  const result: ImportItemsResult = { created: 0, updated: 0, skipped: 0 };
  const existingBarcodes = new Set((await listItems(db)).map((i) => i.barcode));

  for (const row of rows) {
    const barcode = row.barcode?.trim();
    const name = row.name?.trim();
    if (!barcode || !name) {
      result.skipped++;
      continue;
    }
    const alreadyExisted = existingBarcodes.has(barcode);
    await upsertItemByBarcode(db, {
      barcode,
      name,
      hsn_code: row.hsn_code || null,
      unit: row.unit || null,
    });
    if (alreadyExisted) {
      result.updated++;
    } else {
      result.created++;
      existingBarcodes.add(barcode);
    }
  }
  return result;
}

interface BackupFile {
  version: number;
  items: Item[];
  transactions: Transaction[];
}

export async function importBackupJson(db: SQLiteDatabase): Promise<boolean> {
  const picked = await DocumentPicker.getDocumentAsync({
    type: ['application/json', '*/*'],
    copyToCacheDirectory: true,
  });
  if (picked.canceled || picked.assets.length === 0) return false;

  const file = new File(picked.assets[0].uri);
  const text = await file.text();
  const parsed: BackupFile = JSON.parse(text);
  if (!Array.isArray(parsed.items) || !Array.isArray(parsed.transactions)) {
    throw new Error('Invalid backup file: missing items/transactions arrays');
  }
  await replaceAllData(db, { items: parsed.items, transactions: parsed.transactions });
  return true;
}
