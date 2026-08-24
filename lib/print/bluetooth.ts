import { PermissionsAndroid, Platform } from 'react-native';
import RNBluetoothClassic, { type BluetoothDevice } from 'react-native-bluetooth-classic';

export interface PrinterDevice {
  address: string;
  name: string;
}

// Bluetooth *label* printers are near-universally Android-only devices (classic SPP,
// no MFi certification for iOS External Accessory), so this feature only targets Android.
export function isBluetoothPrintingSupported(): boolean {
  return Platform.OS === 'android';
}

export async function requestBluetoothPermissions(): Promise<boolean> {
  if (Platform.OS !== 'android') return true;

  if (Platform.Version >= 31) {
    const results = await PermissionsAndroid.requestMultiple([
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
    ]);
    return Object.values(results).every((r) => r === PermissionsAndroid.RESULTS.GRANTED);
  }

  const result = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION
  );
  return result === PermissionsAndroid.RESULTS.GRANTED;
}

export async function listPairedPrinters(): Promise<PrinterDevice[]> {
  const devices = await RNBluetoothClassic.getBondedDevices();
  return devices.map((d) => ({ address: d.address, name: d.name || d.address }));
}

export async function printRawToDevice(address: string, data: string): Promise<void> {
  let device: BluetoothDevice;
  const alreadyConnected = await RNBluetoothClassic.isDeviceConnected(address);
  if (alreadyConnected) {
    device = await RNBluetoothClassic.getConnectedDevice(address);
  } else {
    device = await RNBluetoothClassic.connectToDevice(address);
  }
  try {
    await device.write(data);
  } finally {
    await device.disconnect().catch(() => {});
  }
}
