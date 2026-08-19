import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

export function DateRangeFilter({
  from,
  to,
  onFromChange,
  onToChange,
  onClear,
}: {
  from: string;
  to: string;
  onFromChange: (v: string) => void;
  onToChange: (v: string) => void;
  onClear: () => void;
}) {
  return (
    <View style={styles.row}>
      <TextInput
        style={styles.input}
        placeholder="From (YYYY-MM-DD)"
        value={from}
        onChangeText={onFromChange}
        autoCapitalize="none"
      />
      <TextInput
        style={styles.input}
        placeholder="To (YYYY-MM-DD)"
        value={to}
        onChangeText={onToChange}
        autoCapitalize="none"
      />
      <Pressable style={styles.clear} onPress={onClear}>
        <Text style={styles.clearText}>Clear</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: 8, padding: 16, paddingBottom: 8, alignItems: 'center' },
  input: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 8,
    fontSize: 13,
  },
  clear: { paddingHorizontal: 10, paddingVertical: 8 },
  clearText: { color: '#1d4ed8', fontWeight: '600' },
});
