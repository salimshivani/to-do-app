import { useFocusEffect, useRouter } from 'expo-router';
import { useSQLiteContext } from 'expo-sqlite';
import { useCallback, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { MenuButton } from '../components/MenuButton';
import { getItemwiseReport } from '../db/queries';

export default function Home() {
  const router = useRouter();
  const db = useSQLiteContext();
  const [stats, setStats] = useState({ itemCount: 0, netStock: 0 });

  useFocusEffect(
    useCallback(() => {
      let cancelled = false;
      getItemwiseReport(db).then((rows) => {
        if (cancelled) return;
        setStats({
          itemCount: rows.length,
          netStock: rows.reduce((sum, r) => sum + r.net_stock, 0),
        });
      });
      return () => {
        cancelled = true;
      };
    }, [db])
  );

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.statsRow}>
        <View style={styles.statCard}>
          <Text style={styles.statValue}>{stats.itemCount}</Text>
          <Text style={styles.statLabel}>Items</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statValue}>{stats.netStock}</Text>
          <Text style={styles.statLabel}>Net Stock</Text>
        </View>
      </View>

      <Text style={styles.sectionTitle}>Scan</Text>
      <MenuButton
        title="Scan Inward"
        subtitle="Receive stock into inventory"
        color="#16a34a"
        onPress={() => router.push({ pathname: '/scan', params: { direction: 'inward' } })}
      />
      <MenuButton
        title="Scan Outward"
        subtitle="Dispatch stock out of inventory"
        color="#dc2626"
        onPress={() => router.push({ pathname: '/scan', params: { direction: 'outward' } })}
      />

      <Text style={styles.sectionTitle}>Manage</Text>
      <MenuButton title="Items" subtitle="View and edit item master data" onPress={() => router.push('/items')} />
      <MenuButton title="Reports" subtitle="Datewise, itemwise, HSN-wise" onPress={() => router.push('/reports')} />
      <MenuButton
        title="Pending Labels"
        subtitle="Generated barcodes waiting to be printed"
        color="#7c3aed"
        onPress={() => router.push('/labels')}
      />
      <MenuButton title="Import / Export" subtitle="Backup, restore, CSV" onPress={() => router.push('/data')} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 16,
    gap: 10,
  },
  statsRow: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 8,
  },
  statCard: {
    flex: 1,
    backgroundColor: '#eff6ff',
    borderRadius: 12,
    paddingVertical: 18,
    alignItems: 'center',
  },
  statValue: {
    fontSize: 26,
    fontWeight: '700',
    color: '#1d4ed8',
  },
  statLabel: {
    fontSize: 13,
    color: '#374151',
    marginTop: 2,
  },
  sectionTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#6b7280',
    textTransform: 'uppercase',
    marginTop: 12,
    marginBottom: 2,
  },
});
