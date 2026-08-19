import { useSQLiteContext } from 'expo-sqlite';
import { useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text } from 'react-native';

import { MenuButton } from '../components/MenuButton';
import {
  exportBackupJson,
  exportItemsCsv,
  exportTransactionsCsv,
  importBackupJson,
  importItemsCsv,
} from '../lib/exportImport';

function confirmReplaceAllData(): Promise<boolean> {
  return new Promise((resolve) => {
    Alert.alert('Replace all data?', 'This overwrites everything currently on this device.', [
      { text: 'Cancel', style: 'cancel', onPress: () => resolve(false) },
      { text: 'Restore', style: 'destructive', onPress: () => resolve(true) },
    ]);
  });
}

export default function DataScreen() {
  const db = useSQLiteContext();
  const [busy, setBusy] = useState(false);

  const run = async (label: string, action: () => Promise<unknown>) => {
    if (busy) return;
    setBusy(true);
    try {
      await action();
    } catch (e) {
      Alert.alert(`${label} failed`, e instanceof Error ? e.message : 'Unknown error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.sectionTitle}>Export</Text>
      <MenuButton
        title="Export Items (CSV)"
        subtitle="Item master data: barcode, name, HSN, unit"
        onPress={() => run('Export items', () => exportItemsCsv(db))}
      />
      <MenuButton
        title="Export Transactions (CSV)"
        subtitle="Full inward/outward log for spreadsheets"
        onPress={() => run('Export transactions', () => exportTransactionsCsv(db))}
      />
      <MenuButton
        title="Export Full Backup (JSON)"
        subtitle="Everything, for restore on this or another device"
        color="#7c3aed"
        onPress={() => run('Export backup', () => exportBackupJson(db))}
      />

      <Text style={styles.sectionTitle}>Import</Text>
      <MenuButton
        title="Import Items (CSV)"
        subtitle="Columns: barcode, name, hsn_code, unit — updates existing barcodes"
        color="#16a34a"
        onPress={() =>
          run('Import items', async () => {
            const result = await importItemsCsv(db);
            if (result) {
              Alert.alert(
                'Import complete',
                `Created ${result.created}, updated ${result.updated}, skipped ${result.skipped}.`
              );
            }
          })
        }
      />
      <MenuButton
        title="Restore Full Backup (JSON)"
        subtitle="Replaces ALL current items and transactions"
        color="#dc2626"
        onPress={async () => {
          const confirmed = await confirmReplaceAllData();
          if (!confirmed) return;
          run('Restore backup', async () => {
            const ok = await importBackupJson(db);
            if (ok) Alert.alert('Restore complete', 'All data has been replaced.');
          });
        }}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 16, gap: 10 },
  sectionTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#6b7280',
    textTransform: 'uppercase',
    marginTop: 12,
    marginBottom: 2,
  },
});
