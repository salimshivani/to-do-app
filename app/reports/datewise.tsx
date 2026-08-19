import { useFocusEffect } from 'expo-router';
import { useSQLiteContext } from 'expo-sqlite';
import { useCallback, useState } from 'react';
import { FlatList, StyleSheet, Text, View } from 'react-native';

import { DateRangeFilter } from '../../components/DateRangeFilter';
import { getDatewiseReport } from '../../db/queries';
import type { DatewiseRow } from '../../types';

export default function DatewiseReport() {
  const db = useSQLiteContext();
  const [rows, setRows] = useState<DatewiseRow[]>([]);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const load = useCallback(() => {
    getDatewiseReport(db, { from: from || undefined, to: to || undefined }).then(setRows);
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
        keyExtractor={(r) => `${r.date}-${r.direction}`}
        contentContainerStyle={{ padding: 16, paddingTop: 4 }}
        ListEmptyComponent={<Text style={styles.empty}>No transactions in this range.</Text>}
        renderItem={({ item }) => (
          <View style={styles.row}>
            <Text style={styles.date}>{item.date}</Text>
            <Text style={[styles.direction, { color: item.direction === 'inward' ? '#16a34a' : '#dc2626' }]}>
              {item.direction === 'inward' ? 'IN' : 'OUT'}
            </Text>
            <Text style={styles.qty}>{item.total_quantity}</Text>
            <Text style={styles.count}>{item.transaction_count} txn</Text>
          </View>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
    gap: 10,
  },
  date: { flex: 1.2, fontSize: 14, fontWeight: '600', color: '#111827' },
  direction: { width: 40, fontWeight: '800' },
  qty: { width: 70, fontSize: 15, fontWeight: '700', textAlign: 'right', color: '#111827' },
  count: { width: 60, fontSize: 12, color: '#6b7280', textAlign: 'right' },
  empty: { textAlign: 'center', color: '#9ca3af', marginTop: 40 },
});
