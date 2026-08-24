import { useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';
import { useSQLiteContext } from 'expo-sqlite';
import { useCallback, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import { deleteItem, getItemById, listTransactionsForItem, updateItem } from '../../db/queries';
import type { Item, Transaction } from '../../types';

export default function EditItem() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const itemId = Number(id);
  const db = useSQLiteContext();
  const router = useRouter();

  const [item, setItem] = useState<Item | null>(null);
  const [name, setName] = useState('');
  const [hsnCode, setHsnCode] = useState('');
  const [unit, setUnit] = useState('');
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [saving, setSaving] = useState(false);

  useFocusEffect(
    useCallback(() => {
      getItemById(db, itemId).then((row) => {
        if (!row) return;
        setItem(row);
        setName(row.name);
        setHsnCode(row.hsn_code ?? '');
        setUnit(row.unit ?? '');
      });
      listTransactionsForItem(db, itemId).then(setTransactions);
    }, [db, itemId])
  );

  const save = async () => {
    if (!name.trim()) {
      Alert.alert('Name required');
      return;
    }
    setSaving(true);
    try {
      await updateItem(db, itemId, { name, hsn_code: hsnCode || null, unit: unit || null });
      router.back();
    } finally {
      setSaving(false);
    }
  };

  const remove = () => {
    Alert.alert('Delete item', 'This also deletes its transaction history. Continue?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          await deleteItem(db, itemId);
          router.back();
        },
      },
    ]);
  };

  if (!item) return null;

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.label}>Barcode</Text>
      <Text style={styles.readonly}>{item.barcode}</Text>
      <Pressable style={styles.printLinkButton} onPress={() => router.push(`/print/${item.id}`)}>
        <Text style={styles.printLinkText}>Print Label</Text>
      </Pressable>

      <Text style={styles.label}>Name</Text>
      <TextInput style={styles.input} value={name} onChangeText={setName} />
      <Text style={styles.label}>HSN Code</Text>
      <TextInput style={styles.input} value={hsnCode} onChangeText={setHsnCode} />
      <Text style={styles.label}>Unit</Text>
      <TextInput style={styles.input} value={unit} onChangeText={setUnit} />

      <Pressable style={[styles.button, saving && styles.disabled]} disabled={saving} onPress={save}>
        <Text style={styles.buttonText}>{saving ? 'Saving…' : 'Save Changes'}</Text>
      </Pressable>
      <Pressable style={styles.deleteButton} onPress={remove}>
        <Text style={styles.deleteButtonText}>Delete Item</Text>
      </Pressable>

      <Text style={styles.sectionTitle}>Recent Transactions</Text>
      {transactions.length === 0 ? (
        <Text style={styles.empty}>No transactions yet.</Text>
      ) : (
        transactions.map((t) => (
          <View key={t.id} style={styles.txRow}>
            <Text style={[styles.txDirection, { color: t.direction === 'inward' ? '#16a34a' : '#dc2626' }]}>
              {t.direction === 'inward' ? 'IN' : 'OUT'}
            </Text>
            <View style={{ flex: 1 }}>
              <Text style={styles.txQty}>{t.quantity} unit(s)</Text>
              <Text style={styles.txMeta}>{new Date(t.timestamp).toLocaleString()}</Text>
              {t.note ? <Text style={styles.txMeta}>{t.note}</Text> : null}
            </View>
          </View>
        ))
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 20, gap: 4, paddingBottom: 60 },
  label: { fontSize: 13, fontWeight: '600', color: '#374151', marginTop: 12, marginBottom: 4 },
  readonly: { fontSize: 16, color: '#111827', paddingVertical: 8 },
  printLinkButton: { alignSelf: 'flex-start', marginBottom: 4 },
  printLinkText: { color: '#1d4ed8', fontWeight: '600', fontSize: 13 },
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
  deleteButton: { alignItems: 'center', paddingVertical: 14 },
  deleteButtonText: { color: '#dc2626', fontWeight: '600' },
  disabled: { opacity: 0.6 },
  sectionTitle: { fontSize: 14, fontWeight: '700', color: '#6b7280', textTransform: 'uppercase', marginTop: 16, marginBottom: 8 },
  empty: { color: '#9ca3af' },
  txRow: { flexDirection: 'row', gap: 12, paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  txDirection: { width: 40, fontWeight: '800' },
  txQty: { fontSize: 15, fontWeight: '600', color: '#111827' },
  txMeta: { fontSize: 12, color: '#6b7280' },
});
