package com.salimshivani.inventoryscanner.print

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class PrinterDevice(val address: String, val name: String)

private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

fun bluetoothConnectPermission(): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) android.Manifest.permission.BLUETOOTH_CONNECT else null

fun hasBluetoothConnectPermission(context: Context): Boolean {
    val permission = bluetoothConnectPermission() ?: return true
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun bluetoothAdapter(context: Context) =
    (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

@SuppressLint("MissingPermission") // caller verifies hasBluetoothConnectPermission first
suspend fun listPairedPrinters(context: Context): List<PrinterDevice> = withContext(Dispatchers.IO) {
    val adapter = bluetoothAdapter(context) ?: return@withContext emptyList()
    adapter.bondedDevices.map { PrinterDevice(it.address, it.name ?: it.address) }
}

@SuppressLint("MissingPermission") // caller verifies hasBluetoothConnectPermission first
suspend fun printRawToDevice(context: Context, address: String, data: String) = withContext(Dispatchers.IO) {
    val adapter = bluetoothAdapter(context)
        ?: throw IllegalStateException("Bluetooth is not available on this device")
    val device = adapter.getRemoteDevice(address)
    var socket: BluetoothSocket? = null
    try {
        socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
        adapter.cancelDiscovery()
        socket.connect()
        socket.outputStream.write(data.toByteArray(Charsets.US_ASCII))
        socket.outputStream.flush()
    } finally {
        socket?.close()
    }
}
