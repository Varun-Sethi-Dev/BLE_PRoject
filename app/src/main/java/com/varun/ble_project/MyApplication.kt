package com.varun.ble_project

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.STATE_OFF
import android.bluetooth.BluetoothAdapter.STATE_ON
import android.bluetooth.BluetoothAdapter.STATE_TURNING_OFF
import android.bluetooth.BluetoothAdapter.STATE_TURNING_ON
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

class MyApplication : Application() {

    companion object {
        lateinit var bluetoothStateReceiver: BluetoothStateReceiver
    }

    override fun onCreate() {
        super.onCreate()
        bluetoothStateReceiver = BluetoothStateReceiver()
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)
    }
}

class BluetoothStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            // Update your UI or perform actions based on the new state
            when (newState) {
                STATE_ON -> Log.d("BLE@", "Bluetooth is ON")
                STATE_TURNING_ON -> Log.d("BLE@", "Bluetooth is being turned ON")
                STATE_OFF -> Log.d("BLE@", "Bluetooth is OFF")
                STATE_TURNING_OFF -> Log.d("BLE@", "Bluetooth is being turned OFF")
            }
        }
    }
}