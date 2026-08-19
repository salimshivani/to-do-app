import { CameraView, useCameraPermissions, type BarcodeScanningResult } from 'expo-camera';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useSQLiteContext } from 'expo-sqlite';
import { useState } from 'react';
import {
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { createItem, getItemByBarcode, recordTransaction } from '../db/queries';
import type { Direction, Item } from '../types';

const SCANNED_BARCODE_TYPES = [
  'ean13',
  'ean8',
  'upc_a',
  'upc_e',
  'code128',
  'code39',
  'code93',
  'codabar',
  'itf14',
  'qr',
];

export default function ScanScreen() {
  const { direction: directionParam } = useLocalSearchParams<{ direction: string }>();
  const direction: Direction = directionParam === 'outward' ? 'outward' : 'inward';
  const router = useRouter();
  const db = useSQLiteContext();
  const [permission, requestPermission] = useCameraPermissions();

  const [scannedBarcode, setScannedBarcode] = useState<string | null>(null);
  const [matchedItem, setMatchedItem] = useState<Item | null>(null);
  const [lookupDone, setLookupDone] = useState(false);
  const [quantity, setQuantity] = useState('1');
  const [note, setNote] = useState('');
  const [newName, setNewName] = useState('');
  const [newHsn, setNewHsn] = useState('');
  const [newUnit, setNewUnit] = useState('');
  const [saving, setSaving] = useState(false);

  const resetScan = () => {
    setScannedBarcode(null);
    setMatchedItem(null);
    setLookupDone(false);
    setQuantity('1');
    setNote('');
    setNewName('');
    setNewHsn('');
    setNewUnit('');
  };

  const handleBarcodeScanned = async (result: BarcodeScanningResult) => {
    if (scannedBarcode) return; // already handling a scan
    setScannedBarcode(result.data);
    const item = await getItemByBarcode(db, result.data);
    setMatchedItem(item);
    setLookupDone(true);
  };

  const confirmTransaction = async (itemId: number) => {
    const qty = Number(quantity);
    if (!qty || qty <= 0) {
      Alert.alert('Invalid quantity', 'Enter a quantity greater than zero.');
      return;
    }
    setSaving(true);
    try {
      await recordTransaction(db, { item_id: itemId, direction, quantity: qty, note });
      Alert.alert(
        direction === 'inward' ? 'Inward recorded' : 'Outward recorded',
        `${qty} unit(s) logged.`,
        [{ text: 'Scan next', onPress: resetScan }]
      );
    } catch (e) {
      Alert.alert('Error', e instanceof Error ? e.message : 'Failed to save transaction');
    } finally {
      setSaving(false);
    }
  };

  const createAndConfirm = async () => {
    if (!scannedBarcode) return;
    if (!newName.trim()) {
      Alert.alert('Item name required', 'Enter a name for this new item.');
      return;
    }
    const qty = Number(quantity);
    if (!qty || qty <= 0) {
      Alert.alert('Invalid quantity', 'Enter a quantity greater than zero.');
      return;
    }
    setSaving(true);
    try {
      const item = await createItem(db, {
        barcode: scannedBarcode,
        name: newName,
        hsn_code: newHsn || null,
        unit: newUnit || null,
      });
      await recordTransaction(db, { item_id: item.id, direction, quantity: qty, note });
      Alert.alert(
        direction === 'inward' ? 'Item created & inward recorded' : 'Item created & outward recorded',
        `${qty} unit(s) logged for ${item.name}.`,
        [{ text: 'Scan next', onPress: resetScan }]
      );
    } catch (e) {
      Alert.alert('Error', e instanceof Error ? e.message : 'Failed to save item');
    } finally {
      setSaving(false);
    }
  };

  if (!permission) {
    return <View style={styles.center} />;
  }

  if (!permission.granted) {
    return (
      <View style={styles.center}>
        <Text style={styles.permissionText}>Camera access is needed to scan barcodes.</Text>
        <Pressable style={styles.primaryButton} onPress={requestPermission}>
          <Text style={styles.primaryButtonText}>Grant Camera Permission</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={[styles.banner, { backgroundColor: direction === 'inward' ? '#16a34a' : '#dc2626' }]}>
        <Text style={styles.bannerText}>
          {direction === 'inward' ? 'INWARD' : 'OUTWARD'} — point camera at barcode
        </Text>
      </View>

      {!scannedBarcode ? (
        <CameraView
          style={styles.camera}
          facing="back"
          barcodeScannerSettings={{ barcodeTypes: SCANNED_BARCODE_TYPES as never }}
          onBarcodeScanned={handleBarcodeScanned}
        />
      ) : (
        <KeyboardAvoidingView
          style={styles.formWrap}
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        >
          <Text style={styles.barcodeLabel}>Barcode: {scannedBarcode}</Text>

          {!lookupDone ? (
            <Text>Looking up item…</Text>
          ) : matchedItem ? (
            <View style={styles.form}>
              <Text style={styles.itemName}>{matchedItem.name}</Text>
              {matchedItem.hsn_code ? <Text style={styles.itemMeta}>HSN: {matchedItem.hsn_code}</Text> : null}

              <Text style={styles.fieldLabel}>Quantity</Text>
              <TextInput
                style={styles.input}
                keyboardType="numeric"
                value={quantity}
                onChangeText={setQuantity}
              />
              <Text style={styles.fieldLabel}>Note (optional)</Text>
              <TextInput style={styles.input} value={note} onChangeText={setNote} placeholder="e.g. PO number" />

              <Pressable
                style={[styles.primaryButton, saving && styles.disabled]}
                disabled={saving}
                onPress={() => confirmTransaction(matchedItem.id)}
              >
                <Text style={styles.primaryButtonText}>{saving ? 'Saving…' : 'Confirm'}</Text>
              </Pressable>
              <Pressable style={styles.secondaryButton} onPress={resetScan}>
                <Text style={styles.secondaryButtonText}>Cancel</Text>
              </Pressable>
            </View>
          ) : (
            <View style={styles.form}>
              <Text style={styles.itemMeta}>No item found for this barcode. Create it:</Text>

              <Text style={styles.fieldLabel}>Item name</Text>
              <TextInput style={styles.input} value={newName} onChangeText={setNewName} placeholder="Item name" />
              <Text style={styles.fieldLabel}>HSN code (optional)</Text>
              <TextInput style={styles.input} value={newHsn} onChangeText={setNewHsn} placeholder="HSN code" />
              <Text style={styles.fieldLabel}>Unit (optional)</Text>
              <TextInput style={styles.input} value={newUnit} onChangeText={setNewUnit} placeholder="pcs, kg, box…" />
              <Text style={styles.fieldLabel}>Quantity</Text>
              <TextInput
                style={styles.input}
                keyboardType="numeric"
                value={quantity}
                onChangeText={setQuantity}
              />

              <Pressable style={[styles.primaryButton, saving && styles.disabled]} disabled={saving} onPress={createAndConfirm}>
                <Text style={styles.primaryButtonText}>{saving ? 'Saving…' : 'Create Item & Confirm'}</Text>
              </Pressable>
              <Pressable style={styles.secondaryButton} onPress={resetScan}>
                <Text style={styles.secondaryButtonText}>Cancel</Text>
              </Pressable>
            </View>
          )}
        </KeyboardAvoidingView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#000' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24, gap: 16 },
  camera: { flex: 1 },
  banner: { paddingVertical: 10, alignItems: 'center' },
  bannerText: { color: '#fff', fontWeight: '700', letterSpacing: 1 },
  formWrap: { flex: 1, backgroundColor: '#fff', padding: 20 },
  form: { gap: 4, marginTop: 8 },
  barcodeLabel: { fontSize: 13, color: '#6b7280', marginBottom: 8 },
  itemName: { fontSize: 20, fontWeight: '700', color: '#111827' },
  itemMeta: { fontSize: 14, color: '#374151', marginBottom: 8 },
  fieldLabel: { fontSize: 13, color: '#374151', marginTop: 10, marginBottom: 4, fontWeight: '600' },
  input: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
  },
  primaryButton: {
    backgroundColor: '#1d4ed8',
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 18,
  },
  primaryButtonText: { color: '#fff', fontWeight: '700', fontSize: 16 },
  secondaryButton: { alignItems: 'center', paddingVertical: 12 },
  secondaryButtonText: { color: '#6b7280', fontWeight: '600' },
  permissionText: { textAlign: 'center', fontSize: 15, color: '#374151' },
  disabled: { opacity: 0.6 },
});
