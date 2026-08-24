# Inventory Barcode Scanner

A mobile-only, offline-first inventory management app built with **Expo (React Native)**. Scan item barcodes to log inward and outward stock movements, view reports, and back up your data — no backend server required.

## Tech stack

- **React Native + Expo (Router)** — cross-platform mobile app (iOS/Android), file-based navigation
- **expo-camera** — barcode scanning (EAN-13, UPC-A, Code128, QR, and more)
- **expo-sqlite** — local relational database, fully on-device, works offline
- **expo-file-system / expo-sharing / expo-document-picker** — CSV and JSON import/export
- **react-native-barcode-svg** — renders a Code128 barcode as scalable SVG for on-screen preview/printing
- **react-native-bluetooth-classic** — sends raw TSPL commands to a Bluetooth thermal label printer (Android only)

## Data model

- `items` — barcode, name, HSN code, unit, `barcode_source` (`scanned`/`generated`), `label_printed_at`
- `transactions` — item, direction (`inward`/`outward`), quantity, timestamp, note

Reports are computed as SQL queries/aggregations over `transactions` joined to `items`.

## Features

- Scan a barcode to record inward or outward stock; unrecognized barcodes prompt you to create the item on the spot
- **No barcode on the item?** Generate an internal one at inward-entry time (`Scan → This item has no barcode — generate one`, or the same option on the manual "Add Item" form) and print a sticker for it — see below
- Item master list with search, add, edit, delete
- Reports: **datewise**, **itemwise**, and **HSN-code-wise**, each with an optional date range filter
- Import/export: item master CSV (bulk add/update), transaction log CSV, and a full JSON backup/restore

## Printing barcode stickers for items without one

When an item has no physical barcode, the app generates an internal one (`INT-000123`, Code128) instead of a scanned value. That item is then tracked as needing a printed label — see it any time under **Pending Labels** on the home screen — and can be printed from there, from the item's edit screen, or right after creating it.

The print screen renders the barcode as SVG and lets you set the sticker's **width/height in millimeters** — this is what makes label size dynamic, since it's just two numbers fed into the print template rather than a fixed image. Printing sends raw [TSPL](https://en.wikipedia.org/wiki/Thermal_printer) commands (`lib/print/tspl.ts`) over classic Bluetooth (`lib/print/bluetooth.ts`) to a **paired** thermal label printer — pair it in Android's Bluetooth settings first, then pick it from the in-app device list when you print.

**Requirements and caveats:**
- **Android only.** Bluetooth *label* printers are almost universally classic-Bluetooth (SPP) devices with no iOS support; the print screen shows a message instead of a device picker on iOS.
- **Needs a TSPL-compatible printer** — most generic "Bluetooth barcode label printer" listings (Xprinter, TSC-compatible clones, etc.) qualify. Proprietary-protocol consumer printers (e.g. Niimbot) are not supported by this raw-TSPL approach.
- **Needs the dev-client/native build**, not Expo Go — classic Bluetooth isn't available there. Use the same `expo prebuild` + `expo run:android` flow described below.
- **Untested with real hardware.** This was built and typechecked but not exercised against an actual printer — expect to debug the TSPL template's exact coordinates/margins against your specific printer model. `react-native-bluetooth-classic` is also flagged by `expo-doctor` as not validated against React Native's New Architecture (on by default in this Expo SDK); if it misbehaves at runtime, try setting `"newArchEnabled": false` in `app.json` as a fallback before switching libraries.

## Running the app

```bash
npm install
npm run android   # or: npm run ios / npm start
```

Data lives entirely on the device in a local SQLite database — there is no backend to configure. Since everything is stored on-device only, use **Import / Export → Export Full Backup (JSON)** periodically if you want to guard against data loss or move data to another device.

> `expo-router`'s optional `@expo/ui` dependency pulls in a `react-dom` peer requirement that conflicts with the React version this Expo SDK pins. The committed `.npmrc` (`legacy-peer-deps=true`) resolves this for every `npm install` in this project — you shouldn't need to pass extra flags yourself.

## Building a real Android app locally (no EAS / no login)

`expo-camera` and `expo-sqlite` need native code, so **Expo Go** can run this app for quick testing, but a real installable app (APK, or an AAB for the Play Store) needs a native build. This project can build fully locally against your own Android SDK — no Expo account or cloud service required.

**Prerequisites:** Android Studio (or just the command-line SDK tools) installed, with `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) pointing at the SDK and `platform-tools` on your `PATH`. JDK 17+.

1. **Generate the native Android project** (reads `app.json`'s plugins — camera permission, SQLite, router — and wires them in automatically):
   ```bash
   npx expo prebuild --platform android
   ```
   This creates an `android/` folder (gitignored here — it's a generated artifact, safe to regenerate any time by re-running this command).

2. **Fastest path — build, install, and launch on a connected device or emulator in one step:**
   ```bash
   npx expo run:android
   ```
   This produces a debug build (auto-signed with a debug key) — fine for testing on your own device, not for distributing to others.

   Or do it manually:
   ```bash
   cd android
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **A signed release build** (needed to share the APK with others, or to publish to the Play Store) requires your own signing key:
   ```bash
   keytool -genkeypair -v -keystore inventory-release.keystore -alias inventory -keyalg RSA -keysize 2048 -validity 10000
   ```
   Then add a `signingConfigs.release` block to `android/app/build.gradle` pointing at that keystore (store the passwords in `~/.gradle/gradle.properties`, *not* in the repo — that file lives outside `android/`, so it survives re-running `prebuild`). Then:
   ```bash
   cd android
   ./gradlew assembleRelease   # → app/build/outputs/apk/release/app-release.apk
   ./gradlew bundleRelease     # → app/build/outputs/bundle/release/app-release.aab (for Play Store)
   ```

Since `android/` is regenerated by `prebuild`, any manual edits to files inside it (like the signing config) get wiped if you run `prebuild` again — reapply that edit afterward, or drop `/android` from `.gitignore` and commit the generated project once you start customizing it.

Before a release/Play Store build, also open `app.json` and set `android.package` to a package name you actually own the naming rights to (it currently defaults to `com.salimshivani.inventoryscanner`) — it can't be changed after publishing.

### Cloud build alternative (EAS)

If you'd rather not maintain a local Android SDK, `eas.json` already has `development`/`preview`/`production` build profiles set up for [EAS Build](https://docs.expo.dev/build/introduction/), Expo's cloud build service (`npx eas-cli@latest login`, `init`, then `build --platform android --profile preview`). Don't install `eas-cli` as a project dependency — its `react-dom` peer requirement conflicts with the React version this Expo SDK pins; always invoke it via `npx eas-cli@latest ...` instead.

## Web support

Web is not currently configured. `expo-camera`'s barcode scanning and `expo-sqlite` both have real limitations in the browser (camera scanning support is inconsistent across browsers, and SQLite-on-web is alpha and needs special server headers), so it isn't a drop-in target for this app — see the [Expo docs](https://docs.expo.dev/versions/v57.0.0/) for `expo-camera` and `expo-sqlite` before relying on it. Ask if you'd like this wired up.
