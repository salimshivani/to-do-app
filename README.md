# Inventory Barcode Scanner

A mobile-only, offline-first inventory management app for Android. Scan item barcodes to log inward and outward stock movements, view reports, and back up your data — no backend server required.

## Tech stack

- **Kotlin + XML layouts / View Binding** — native Android, no cross-platform framework
- **Room** (over SQLite) — local relational database, fully on-device, works offline
- **CameraX + ML Kit Barcode Scanning** — barcode scanning (EAN-13, UPC-A, Code128, QR, and more)
- **ZXing (`core`)** — renders Code128 barcodes as bitmaps for on-screen preview, printing, and PDFs
- **Android's own Bluetooth (classic RFCOMM) API** — sends raw TSPL commands to a Bluetooth thermal label printer
- **Android's `PdfDocument` API** — renders a real-size PDF for a printer-free "test print" mode
- **Storage Access Framework + `FileProvider`** — CSV/JSON import and export

This app was originally built with Expo/React Native and later migrated to native Android — mainly to fix ballooning APK size (the RN build was 119MB fully universal; this one is **~23MB covering all four CPU architectures**) and to drop a dependency (`react-native-bluetooth-classic`) that wasn't validated against React Native's newer architecture.

## Data model

- `items` — barcode, name, HSN code, unit, purchase price, MRP, selling `price` per unit, GST %, `barcode_source` (`scanned`/`generated`), `label_printed_at`
- `transactions` — item, direction (`inward`/`outward`), quantity, timestamp, note, account, the `bill_id` it belongs to, and the rate/GST%/tax/line-total actually charged for that line (independent of the item master's current prices)
- `accounts` — customer/supplier master: mobile number (unique), name, address
- `bills` — one per scan batch: account, direction, subtotal, tax, total amount, status (`unpaid`/`paid`), date, and the `payment_id` that settled it (if paid)
- `payments` — a single payment-received/payment-made entry that can settle one or many bills at once

Reports (`TransactionDao`) are computed as SQL aggregations over `transactions` joined to `items`/`accounts`.

## Features

- Scan a barcode to record inward or outward stock; unrecognized barcodes prompt you to create the item on the spot
- **No barcode on the item?** Generate an internal one at inward-entry time (`Scan → This item has no barcode — generate one`, or the same option on the manual "Add Item" form) and print a sticker for it — see below
- **Loose/bulk items with no barcode on hand (grains, etc.)**: tap `Scan → Search item by name` to find an existing item by name instead of scanning, then enter any decimal quantity — e.g. buy in bulk as `100` (kg) inward and sell loose as `0.5`, `0.25`, `0.1`, or `2` (kg) outward, since quantity is a decimal field for every item
- Item master list with search, add, edit, delete, and purchase price / MRP / selling price / GST% per item
- Scanning (or searching) an item for inward/outward shows an editable **rate** and **GST%** (defaulted from the item master — purchase price for inward, selling price for outward) with a live Taxable/Tax/Total readout, so a one-off negotiated price doesn't require editing the item master
- After scanning a batch of items, capture the supplier/customer (mobile-number lookup auto-fills an existing account, otherwise creates one) and a bill — subtotal and tax are computed from the batch, and the grand total is pre-filled and editable
- **Accounts screen**: list of customers/suppliers with outstanding receivable/payable at a glance; tap one for its full bill and payment history
- **Billing & payments**: each scan batch produces one GST-itemized bill (payable on inward, receivable on outward). From an account's detail screen, select any number of its unpaid bills and record a single payment against them — one payment entry settles multiple bills at once, the same way jewellers' billing software reconciles a customer's running account
- **Print or share any bill as a PDF**: right after saving a bill, or any time from an account's bill list (the **PDF** button on each bill row), generate a GST-style invoice/purchase-bill PDF with full line-item and tax breakdown — sent either to the Android system print dialog (any registered printer) or the share sheet (WhatsApp, email, Drive, etc. — whatever's installed)
- Reports: **datewise**, **itemwise**, **HSN-code-wise**, and **customer/supplier-wise** (including outstanding dues), each with an optional date range filter and its own **PDF** button (next to "Clear") to print or share the currently filtered table
- Import/export: item master CSV (bulk add/update, including purchase price/MRP/GST), transaction log CSV, and a full backup/restore (items, transactions, accounts, bills, payments)

## Navigation

Every screen has a proper Android toolbar (app bar) with its title and a back arrow — the app previously ran with `NoActionBar` and relied only on the system back gesture/button, which made it unclear where you were or how to get back without it. `MainActivity` (the home screen) is the only one without a back arrow, since it's the root.

## Printing or sharing a bill / report as a PDF

Bills (`print/BillPdfGenerator.kt`) and reports (`print/ReportPdfGenerator.kt`) are rendered to a cached A4 PDF the same way label previews are (`android.graphics.pdf.PdfDocument`), then handed to one of two paths in `print/PdfPrintShare.kt`:
- **Print** goes through Android's system `PrintManager` — it works with any printer the device already has a print service for (Wi‑Fi/cloud printers, PDF-to-file, etc.), not just the Bluetooth thermal printer used for labels.
- **Share** builds a standard `ACTION_SEND` chooser for the PDF file — this surfaces whatever's installed (WhatsApp, Gmail/Outlook, Drive, Bluetooth, …) without the app needing to integrate with any of them individually.

## Printing barcode stickers for items without one

When an item has no physical barcode, the app generates an internal one (`INT-000123`, Code128) instead of a scanned value. That item is then tracked as needing a printed label — see it any time under **Pending Labels** on the home screen — and can be printed from there, from the item's edit screen, or right after creating it.

The print screen (`PrintLabelActivity`) renders the barcode as a bitmap and lets you set the sticker's **width/height in millimeters** — this is what makes label size dynamic, since it's just two numbers fed into the print template rather than a fixed image. Printing sends raw [TSPL](https://en.wikipedia.org/wiki/Thermal_printer) commands (`print/TsplBuilder.kt`) over classic Bluetooth (`print/BluetoothPrinter.kt`) to a **paired** thermal label printer — pair it in Android's Bluetooth settings first, then pick it from the in-app device list when you print.

### Test Print (PDF) — no printer required

Before you have a physical printer to test against (or any time you just want to sanity-check a layout), tap **Test Print (PDF Preview)** instead of "Print via Bluetooth". It skips Bluetooth entirely: it draws the on-screen sticker preview into a bitmap, lays it into a PDF page sized to the *exact* sticker dimensions you entered (`print/LabelPdfGenerator.kt`), and opens the share sheet so you can view, save, or send it. Viewing that PDF at 100% zoom shows the label at true physical size — useful for checking that text isn't clipped and the barcode fits before committing to a real print. This mode never touches the printer or marks the label as printed.

**Requirements and caveats:**
- **Needs a TSPL-compatible printer** — most generic "Bluetooth barcode label printer" listings (Xprinter, TSC-compatible clones, etc.) qualify. Proprietary-protocol consumer printers (e.g. Niimbot) are not supported by this raw-TSPL approach.
- **Untested with real hardware.** This was built and compiled successfully but not exercised against an actual printer — expect to debug the TSPL template's exact coordinates/margins against your specific printer model.

## Building the app locally

**Prerequisites:** Android Studio (or just the command-line SDK tools), with `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) pointing at the SDK and `platform-tools` on your `PATH`. JDK 17+.

1. Point Gradle at your SDK — create `local.properties` at the repo root (gitignored, machine-specific):
   ```
   sdk.dir=/path/to/your/Android/sdk
   ```
2. **Build and install on a connected device or emulator:**
   ```bash
   ./gradlew installDebug
   ```
   Or from Android Studio: open the repo root, let Gradle sync, then Run.
3. **A release build** (minified with R8, resource-shrunk):
   ```bash
   ./gradlew assembleRelease   # → app/build/outputs/apk/release/app-release.apk
   ```
   This is currently signed with the auto-generated debug key (`~/.android/debug.keystore`) purely for local-test convenience — it installs fine on a device but isn't valid for Play Store distribution. For real distribution, generate your own key and point `signingConfigs.release` in `app/build.gradle.kts` at it instead:
   ```bash
   keytool -genkeypair -v -keystore inventory-release.keystore -alias inventory -keyalg RSA -keysize 2048 -validity 10000
   ```
   (keep the keystore and its passwords out of the repo — e.g. in `~/.gradle/gradle.properties`, read into `build.gradle.kts` via `project.findProperty(...)`)

Also update `applicationId`/`namespace` in `app/build.gradle.kts` (currently `com.salimshivani.inventoryscanner`) to a package name you actually own the naming rights to before publishing — it can't be changed after release.

## Permissions

- `CAMERA` — barcode scanning
- `BLUETOOTH_CONNECT` (Android 12+) / `BLUETOOTH`, `BLUETOOTH_ADMIN` (Android ≤11) — talking to a paired label printer
- `INTERNET`, `ACCESS_NETWORK_STATE` — pulled in transitively by the ML Kit barcode-scanning library's manifest; the app itself makes no network calls and works fully offline

No storage/location permissions are needed — CSV/JSON export goes through `FileProvider` + share sheet, import through the system file picker (Storage Access Framework), and paired-device listing doesn't require location on this API surface.

Data lives entirely on the device in a local Room/SQLite database — there is no backend to configure. Since everything is stored on-device only, use **Import / Export → Export Full Backup** periodically if you want to guard against data loss or move data to another device. The exported file is JSON under the hood but saved with a `.bkp` extension and a generic MIME type — deliberately not labeled "JSON" in the UI or by file type, so a backup sitting in a Downloads folder doesn't advertise its format or structure to anyone who finds it.
