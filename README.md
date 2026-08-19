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

## Building a real Android app (EAS Build)

`expo-camera` and `expo-sqlite` need native code, so **Expo Go** can run this app for quick testing, but a real installable app (APK, or an AAB for the Play Store) requires [EAS Build](https://docs.expo.dev/build/introduction/) — Expo's free-tier-available cloud build service. Build profiles are already defined in `eas.json` (`development`, `preview`, `production`).

1. **Install the CLI and log in** (one-time):
   ```bash
   npx eas-cli@latest login
   ```
   Uses/creates a free Expo account.

2. **Link this project to EAS** (one-time — generates a project ID and writes it into `app.json`):
   ```bash
   npx eas-cli@latest init
   ```

3. **Build an installable APK** (for sharing/sideloading, no Play Store needed):
   ```bash
   npx eas-cli@latest build --platform android --profile preview
   ```
   EAS builds in the cloud and gives you a download link (and a QR code) for the `.apk` when it finishes.

4. **Build a production AAB** (for Play Store submission):
   ```bash
   npx eas-cli@latest build --platform android --profile production
   ```

5. **Submit to the Play Store** (once you have a Play Console developer account):
   ```bash
   npx eas-cli@latest submit --platform android
   ```

Before step 4/5, open `app.json` and set `android.package` to a package name you actually own the naming rights to (it currently defaults to `com.salimshivani.inventoryscanner`) — it can't be changed after the app is published to the Play Store.

### Local build without EAS (alternative)

If you'd rather build locally with Android Studio instead of using the cloud service:
```bash
npx expo prebuild --platform android
cd android && ./gradlew assembleRelease   # or bundleRelease for an .aab
```
This requires the Android SDK/NDK installed locally and generates the native `android/` project (gitignored here, since it's normally regenerated on demand).

## Web support

Web is not currently configured. `expo-camera`'s barcode scanning and `expo-sqlite` both have real limitations in the browser (camera scanning support is inconsistent across browsers, and SQLite-on-web is alpha and needs special server headers), so it isn't a drop-in target for this app — see the [Expo docs](https://docs.expo.dev/versions/v57.0.0/) for `expo-camera` and `expo-sqlite` before relying on it. Ask if you'd like this wired up.
