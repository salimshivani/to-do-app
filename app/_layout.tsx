import { Stack } from 'expo-router';
import { SQLiteProvider } from 'expo-sqlite';
import { Suspense } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { migrateDbIfNeeded } from '../db/schema';

function Loading() {
  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
      <ActivityIndicator size="large" />
    </View>
  );
}

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <Suspense fallback={<Loading />}>
        <SQLiteProvider databaseName="inventory.db" onInit={migrateDbIfNeeded} useSuspense>
          <Stack
            screenOptions={{
              headerStyle: { backgroundColor: '#1d4ed8' },
              headerTintColor: '#fff',
              headerTitleStyle: { fontWeight: '600' },
            }}
          >
            <Stack.Screen name="index" options={{ title: 'Inventory' }} />
            <Stack.Screen name="scan" options={{ title: 'Scan Barcode' }} />
            <Stack.Screen name="items/index" options={{ title: 'Items' }} />
            <Stack.Screen name="items/new" options={{ title: 'Add Item' }} />
            <Stack.Screen name="items/[id]" options={{ title: 'Edit Item' }} />
            <Stack.Screen name="reports/index" options={{ title: 'Reports' }} />
            <Stack.Screen name="reports/datewise" options={{ title: 'Datewise Report' }} />
            <Stack.Screen name="reports/itemwise" options={{ title: 'Itemwise Report' }} />
            <Stack.Screen name="reports/hsncode" options={{ title: 'HSN Code Report' }} />
            <Stack.Screen name="data" options={{ title: 'Import / Export' }} />
          </Stack>
        </SQLiteProvider>
      </Suspense>
    </SafeAreaProvider>
  );
}
