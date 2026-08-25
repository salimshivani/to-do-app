import Barcode from 'react-native-barcode-svg';
import { useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';
import { useSQLiteContext } from 'expo-sqlite';
import { useCallback, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import ViewShot, { type ViewShotRef } from 'react-native-view-shot';

import { getItemById, markLabelPrinted } from '../../db/queries';
import {
  isBluetoothPrintingSupported,
  listPairedPrinters,
  printRawToDevice,
  requestBluetoothPermissions,
  type PrinterDevice,
} from '../../lib/print/bluetooth';
import { generateLabelPdf, shareLabelPdf } from '../../lib/print/pdf';
import { buildTsplLabel } from '../../lib/print/tspl';
import type { Item } from '../../types';

const DEFAULT_WIDTH_MM = '40';
const DEFAULT_HEIGHT_MM = '25';

export default function PrintLabel() {
  const { itemId } = useLocalSearchParams<{ itemId: string }>();
  const db = useSQLiteContext();
  const router = useRouter();

  const [item, setItem] = useState<Item | null>(null);
  const [widthMm, setWidthMm] = useState(DEFAULT_WIDTH_MM);
  const [heightMm, setHeightMm] = useState(DEFAULT_HEIGHT_MM);
  const [pickerVisible, setPickerVisible] = useState(false);
  const [printers, setPrinters] = useState<PrinterDevice[]>([]);
  const [loadingPrinters, setLoadingPrinters] = useState(false);
  const [printing, setPrinting] = useState(false);
  const [generatingTestPdf, setGeneratingTestPdf] = useState(false);
  const viewShotRef = useRef<ViewShotRef>(null);

  useFocusEffect(
    useCallback(() => {
      getItemById(db, Number(itemId)).then(setItem);
    }, [db, itemId])
  );

  const parsedWidth = Number(widthMm);
  const parsedHeight = Number(heightMm);
  const dimensionsValid = parsedWidth > 0 && parsedHeight > 0;

  const openPrinterPicker = async () => {
    if (!isBluetoothPrintingSupported()) {
      Alert.alert(
        'Not supported on this platform',
        'Bluetooth label printing is only available on Android — most thermal label printers only support classic Bluetooth on Android.'
      );
      return;
    }
    const granted = await requestBluetoothPermissions();
    if (!granted) {
      Alert.alert('Permission required', 'Bluetooth permission is needed to find your printer.');
      return;
    }
    setLoadingPrinters(true);
    setPickerVisible(true);
    try {
      const devices = await listPairedPrinters();
      setPrinters(devices);
    } catch (e) {
      Alert.alert('Error', e instanceof Error ? e.message : 'Failed to list paired devices');
    } finally {
      setLoadingPrinters(false);
    }
  };

  const printToDevice = async (printer: PrinterDevice) => {
    if (!item || !dimensionsValid) return;
    setPickerVisible(false);
    setPrinting(true);
    try {
      const tspl = buildTsplLabel({
        widthMm: parsedWidth,
        heightMm: parsedHeight,
        barcodeValue: item.barcode,
        itemName: item.name,
        hsnCode: item.hsn_code,
        unit: item.unit,
      });
      await printRawToDevice(printer.address, tspl);
      await markLabelPrinted(db, item.id);
      setItem(await getItemById(db, item.id));
      Alert.alert('Sent to printer', `Label sent to ${printer.name}.`);
    } catch (e) {
      Alert.alert('Print failed', e instanceof Error ? e.message : 'Could not send label to printer');
    } finally {
      setPrinting(false);
    }
  };

  const testPrint = async () => {
    if (!dimensionsValid || !viewShotRef.current) return;
    setGeneratingTestPdf(true);
    try {
      const base64 = await viewShotRef.current.capture();
      const uri = await generateLabelPdf(base64, parsedWidth, parsedHeight);
      await shareLabelPdf(uri);
    } catch (e) {
      Alert.alert('Preview failed', e instanceof Error ? e.message : 'Could not generate the PDF preview');
    } finally {
      setGeneratingTestPdf(false);
    }
  };

  if (!item) return null;

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.itemName}>{item.name}</Text>
      <Text style={styles.itemMeta}>
        {item.barcode}
        {item.barcode_source === 'generated' ? ' · generated' : ''}
      </Text>
      {item.label_printed_at ? (
        <Text style={styles.printedNote}>Last printed {new Date(item.label_printed_at).toLocaleString()}</Text>
      ) : null}

      <View style={styles.previewCard}>
        <ViewShot ref={viewShotRef} options={{ format: 'png', quality: 1, result: 'base64' }}>
          <View
            style={[
              styles.stickerOutline,
              dimensionsValid
                ? { aspectRatio: parsedWidth / parsedHeight }
                : { aspectRatio: 1.6 },
            ]}
          >
            <Barcode value={item.barcode || ' '} format="CODE128" maxWidth={260} height={70} />
            <Text style={styles.previewName} numberOfLines={1}>
              {item.name}
            </Text>
            {item.hsn_code ? <Text style={styles.previewMeta}>HSN {item.hsn_code}</Text> : null}
          </View>
        </ViewShot>
      </View>

      <Text style={styles.sectionTitle}>Sticker size</Text>
      <View style={styles.sizeRow}>
        <View style={{ flex: 1 }}>
          <Text style={styles.fieldLabel}>Width (mm)</Text>
          <TextInput style={styles.input} keyboardType="numeric" value={widthMm} onChangeText={setWidthMm} />
        </View>
        <View style={{ flex: 1 }}>
          <Text style={styles.fieldLabel}>Height (mm)</Text>
          <TextInput style={styles.input} keyboardType="numeric" value={heightMm} onChangeText={setHeightMm} />
        </View>
      </View>
      {!dimensionsValid ? <Text style={styles.errorText}>Enter a valid width and height.</Text> : null}

      <Pressable
        style={[styles.testPrintButton, (!dimensionsValid || generatingTestPdf) && styles.disabled]}
        disabled={!dimensionsValid || generatingTestPdf}
        onPress={testPrint}
      >
        <Text style={styles.testPrintButtonText}>
          {generatingTestPdf ? 'Generating…' : 'Test Print (PDF Preview)'}
        </Text>
      </Pressable>
      <Text style={styles.hint}>
        No printer needed — generates a PDF at the exact sticker size so you can check the layout before printing for real.
      </Text>

      <Pressable
        style={[styles.primaryButton, (!dimensionsValid || printing) && styles.disabled]}
        disabled={!dimensionsValid || printing}
        onPress={openPrinterPicker}
      >
        <Text style={styles.primaryButtonText}>{printing ? 'Printing…' : 'Print via Bluetooth'}</Text>
      </Pressable>

      {Platform.OS !== 'android' ? (
        <Text style={styles.hint}>Bluetooth label printing requires the Android build of this app.</Text>
      ) : null}

      <Modal visible={pickerVisible} animationType="slide" onRequestClose={() => setPickerVisible(false)}>
        <View style={styles.modalContainer}>
          <Text style={styles.modalTitle}>Select a paired printer</Text>
          {loadingPrinters ? (
            <ActivityIndicator style={{ marginTop: 24 }} />
          ) : (
            <FlatList
              data={printers}
              keyExtractor={(d) => d.address}
              ListEmptyComponent={
                <Text style={styles.hint}>
                  No paired Bluetooth devices found. Pair your label printer in Android Bluetooth settings first.
                </Text>
              }
              renderItem={({ item: printer }) => (
                <Pressable style={styles.printerRow} onPress={() => printToDevice(printer)}>
                  <Text style={styles.printerName}>{printer.name}</Text>
                  <Text style={styles.printerAddress}>{printer.address}</Text>
                </Pressable>
              )}
            />
          )}
          <Pressable style={styles.secondaryButton} onPress={() => setPickerVisible(false)}>
            <Text style={styles.secondaryButtonText}>Cancel</Text>
          </Pressable>
        </View>
      </Modal>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 20, gap: 4, paddingBottom: 60 },
  itemName: { fontSize: 20, fontWeight: '700', color: '#111827' },
  itemMeta: { fontSize: 13, color: '#6b7280', marginTop: 2 },
  printedNote: { fontSize: 12, color: '#16a34a', marginTop: 4 },
  previewCard: { alignItems: 'center', marginTop: 16 },
  stickerOutline: {
    borderWidth: 2,
    borderColor: '#111827',
    borderStyle: 'dashed',
    borderRadius: 8,
    padding: 12,
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    maxWidth: 320,
    backgroundColor: '#fff',
    gap: 4,
  },
  previewName: { fontSize: 13, fontWeight: '700', color: '#111827' },
  previewMeta: { fontSize: 11, color: '#6b7280' },
  sectionTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#6b7280',
    textTransform: 'uppercase',
    marginTop: 20,
    marginBottom: 4,
  },
  sizeRow: { flexDirection: 'row', gap: 12 },
  fieldLabel: { fontSize: 13, color: '#374151', marginBottom: 4, fontWeight: '600' },
  input: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
  },
  errorText: { color: '#dc2626', fontSize: 12, marginTop: 6 },
  testPrintButton: {
    backgroundColor: '#fff',
    borderWidth: 1.5,
    borderColor: '#7c3aed',
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 20,
  },
  testPrintButtonText: { color: '#7c3aed', fontWeight: '700', fontSize: 16 },
  primaryButton: {
    backgroundColor: '#1d4ed8',
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 20,
  },
  primaryButtonText: { color: '#fff', fontWeight: '700', fontSize: 16 },
  disabled: { opacity: 0.6 },
  hint: { fontSize: 12, color: '#9ca3af', marginTop: 12, textAlign: 'center' },
  modalContainer: { flex: 1, padding: 20, paddingTop: 60 },
  modalTitle: { fontSize: 18, fontWeight: '700', marginBottom: 16, color: '#111827' },
  printerRow: { paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  printerName: { fontSize: 16, fontWeight: '600', color: '#111827' },
  printerAddress: { fontSize: 12, color: '#6b7280', marginTop: 2 },
  secondaryButton: { alignItems: 'center', paddingVertical: 16, marginTop: 12 },
  secondaryButtonText: { color: '#6b7280', fontWeight: '600' },
});
