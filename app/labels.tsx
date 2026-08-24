import { useFocusEffect, useRouter } from 'expo-router';
import { useSQLiteContext } from 'expo-sqlite';
import { useCallback, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';

import { listItemsPendingLabels } from '../db/queries';
import type { Item } from '../types';

export default function PendingLabels() {
  const db = useSQLiteContext();
  const router = useRouter();
  const [items, setItems] = useState<Item[]>([]);

  useFocusEffect(
    useCallback(() => {
      listItemsPendingLabels(db).then(setItems);
    }, [db])
  );

  return (
    <FlatList
      contentContainerStyle={styles.container}
      data={items}
      keyExtractor={(item) => String(item.id)}
      ListEmptyComponent={
        <Text style={styles.empty}>No pending labels. Items get listed here when a barcode is generated for them during inward entry and hasn't been printed yet.</Text>
      }
      renderItem={({ item }) => (
        <Pressable style={styles.row} onPress={() => router.push(`/print/${item.id}`)}>
          <View style={{ flex: 1 }}>
            <Text style={styles.name}>{item.name}</Text>
            <Text style={styles.meta}>{item.barcode}</Text>
          </View>
          <Text style={styles.printLink}>Print →</Text>
        </Pressable>
      )}
    />
  );
}

const styles = StyleSheet.create({
  container: { padding: 16, flexGrow: 1 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  name: { fontSize: 16, fontWeight: '600', color: '#111827' },
  meta: { fontSize: 13, color: '#6b7280', marginTop: 2 },
  printLink: { color: '#1d4ed8', fontWeight: '700' },
  empty: { textAlign: 'center', color: '#9ca3af', marginTop: 40, paddingHorizontal: 24 },
});
