import { useFocusEffect } from 'expo-router';
import { useSQLiteContext } from 'expo-sqlite';
import { useCallback, useState } from 'react';
import { FlatList, StyleSheet, Text, View } from 'react-native';

import { DateRangeFilter } from '../../components/DateRangeFilter';
import { getItemwiseReport } from '../../db/queries';
import type { ItemwiseRow } from '../../types';

export default function ItemwiseReport() {
  const db = useSQLiteContext();
  const [rows, setRows] = useState<ItemwiseRow[]>([]);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const load = useCallback(() => {
    getItemwiseReport(db, { from: from || undefined, to: to || undefined }).then(setRows);
  }, [db, from, to]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load])
  );

  return (
    <View style={styles.container}>
      <DateRangeFilter
        from={from}
        to={to}
        onFromChange={setFrom}
        onToChange={setTo}
        onClear={() => {
          setFrom('');
          setTo('');
        }}
      />
      <FlatList
        data={rows}
        keyExtractor={(r) => String(r.item_id)}
        contentContainerStyle={{ padding: 16, paddingTop: 4 }}
        ListEmptyComponent={<Text style={styles.empty}>No items yet.</Text>}
        renderItem={({ item }) => (
          <View style={styles.card}>
            <Text style={styles.name}>{item.item_name}</Text>
            <Text style={styles.meta}>
              {item.barcode}
              {item.hsn_code ? ` · HSN ${item.hsn_code}` : ''}
            </Text>
            <View style={styles.statsRow}>
              <Text style={styles.inward}>IN {item.inward_total}</Text>
              <Text style={styles.outward}>OUT {item.outward_total}</Text>
              <Text style={styles.net}>Net {item.net_stock}</Text>
            </View>
          </View>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  card: { paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  name: { fontSize: 15, fontWeight: '700', color: '#111827' },
  meta: { fontSize: 12, color: '#6b7280', marginTop: 2 },
  statsRow: { flexDirection: 'row', gap: 16, marginTop: 6 },
  inward: { color: '#16a34a', fontWeight: '700', fontSize: 13 },
  outward: { color: '#dc2626', fontWeight: '700', fontSize: 13 },
  net: { color: '#1d4ed8', fontWeight: '700', fontSize: 13 },
  empty: { textAlign: 'center', color: '#9ca3af', marginTop: 40 },
});
