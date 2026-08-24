import { useRouter } from 'expo-router';
import { useSQLiteContext } from 'expo-sqlite';
import { useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import { createItem, getItemByBarcode } from '../../db/queries';
import { generateInternalBarcode } from '../../lib/barcode';

export default function NewItem() {
  const db = useSQLiteContext();
  const router = useRouter();
  const [barcode, setBarcode] = useState('');
  const [isGenerated, setIsGenerated] = useState(false);
  const [name, setName] = useState('');
  const [hsnCode, setHsnCode] = useState('');
  const [unit, setUnit] = useState('');
  const [saving, setSaving] = useState(false);

  const handleGenerateBarcode = async () => {
    const generated = await generateInternalBarcode(db);
    setBarcode(generated);
    setIsGenerated(true);
  };

  const save = async () => {
    if (!barcode.trim() || !name.trim()) {
      Alert.alert('Missing fields', 'Barcode and name are required.');
      return;
    }
    setSaving(true);
    try {
      const existing = await getItemByBarcode(db, barcode.trim());
      if (existing) {
        Alert.alert('Duplicate barcode', 'An item with this barcode already exists.');
        return;
      }
      const item = await createItem(db, {
        barcode: barcode.trim(),
        name,
        hsn_code: hsnCode || null,
        unit: unit || null,
        barcode_source: isGenerated ? 'generated' : 'scanned',
      });
      if (isGenerated) {
        Alert.alert('Item created', 'A barcode was generated for this item — print a sticker for it now?', [
          { text: 'Later', style: 'cancel', onPress: () => router.back() },
          { text: 'Print Label', onPress: () => router.replace(`/print/${item.id}`) },
        ]);
      } else {
        router.back();
      }
    } catch (e) {
      Alert.alert('Error', e instanceof Error ? e.message : 'Failed to save item');
    } finally {
      setSaving(false);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.labelRow}>
        <Text style={styles.label}>Barcode</Text>
        <Pressable onPress={handleGenerateBarcode}>
          <Text style={styles.generateLink}>No barcode? Generate one</Text>
        </Pressable>
      </View>
      <TextInput
        style={styles.input}
        value={barcode}
        onChangeText={(v) => {
          setBarcode(v);
          setIsGenerated(false);
        }}
        placeholder="Scan or type barcode"
        autoCapitalize="none"
      />
      <Text style={styles.label}>Name</Text>
      <TextInput style={styles.input} value={name} onChangeText={setName} placeholder="Item name" />
      <Text style={styles.label}>HSN Code (optional)</Text>
      <TextInput style={styles.input} value={hsnCode} onChangeText={setHsnCode} placeholder="HSN code" />
      <Text style={styles.label}>Unit (optional)</Text>
      <TextInput style={styles.input} value={unit} onChangeText={setUnit} placeholder="pcs, kg, box…" />

      <Pressable style={[styles.button, saving && styles.disabled]} disabled={saving} onPress={save}>
        <Text style={styles.buttonText}>{saving ? 'Saving…' : 'Save Item'}</Text>
      </Pressable>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 20, gap: 4 },
  labelRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 12,
    marginBottom: 4,
  },
  label: { fontSize: 13, fontWeight: '600', color: '#374151' },
  generateLink: { fontSize: 13, fontWeight: '600', color: '#1d4ed8' },
  input: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
  },
  button: {
    backgroundColor: '#1d4ed8',
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 24,
  },
  buttonText: { color: '#fff', fontWeight: '700', fontSize: 16 },
  disabled: { opacity: 0.6 },
});
