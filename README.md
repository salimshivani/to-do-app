# Inventory Barcode Scanner

A mobile-only, offline-first inventory management app built with **Expo (React Native)**. Scan item barcodes to log inward and outward stock movements, view reports, and back up your data — no backend server required.

## Tech stack

- **React Native + Expo (Router)** — cross-platform mobile app (iOS/Android), file-based navigation
- **expo-camera** — barcode scanning (EAN-13, UPC-A, Code128, QR, and more)
- **expo-sqlite** — local relational database, fully on-device, works offline
- **expo-file-system / expo-sharing / expo-document-picker** — CSV and JSON import/export

## Data model

- `items` — barcode, name, HSN code, unit
- `transactions` — item, direction (`inward`/`outward`), quantity, timestamp, note

Reports are computed as SQL queries/aggregations over `transactions` joined to `items`.

## Features

- Scan a barcode to record inward or outward stock; unrecognized barcodes prompt you to create the item on the spot
- Item master list with search, add, edit, delete
- Reports: **datewise**, **itemwise**, and **HSN-code-wise**, each with an optional date range filter
- Import/export: item master CSV (bulk add/update), transaction log CSV, and a full JSON backup/restore

## Running the app

```bash
npm install
npm run android   # or: npm run ios / npm start
```

Data lives entirely on the device in a local SQLite database — there is no backend to configure. Since everything is stored on-device only, use **Import / Export → Export Full Backup (JSON)** periodically if you want to guard against data loss or move data to another device.
