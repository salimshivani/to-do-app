import { useRouter } from 'expo-router';
import { ScrollView, StyleSheet } from 'react-native';

import { MenuButton } from '../../components/MenuButton';

export default function ReportsHub() {
  const router = useRouter();
  return (
    <ScrollView contentContainerStyle={styles.container}>
      <MenuButton
        title="Datewise Report"
        subtitle="Inward / outward totals per day"
        onPress={() => router.push('/reports/datewise')}
      />
      <MenuButton
        title="Itemwise Report"
        subtitle="Stock movement and balance per item"
        onPress={() => router.push('/reports/itemwise')}
      />
      <MenuButton
        title="HSN Code Report"
        subtitle="Stock movement grouped by HSN code"
        onPress={() => router.push('/reports/hsncode')}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 16, gap: 10 },
});
