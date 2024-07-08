package com.varun.ble_project

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.varun.ble_project.ui.theme.BLE_ProjectTheme

const val REQUEST_ENABLE_LOCATION = 2
const val REQUEST_ENABLE_BT = 1

class MainActivity1 : ComponentActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BluetoothManager::class.java) as? BluetoothManager)?.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val packageManager = this.packageManager
            val bluetoothAv = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
            val bluetoothLeAv = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            val bluetoothManager = getSystemService(BluetoothManager::class.java)
            val bluetoothAdapter = bluetoothManager?.adapter
            bluetoothAdapter?.let {
                BLE_ProjectTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        DemoBLE(
                            modifier = Modifier.padding(innerPadding),
                            bluetoothAvailable = bluetoothAv.toString(),
                            bluetoothLeAvailable = bluetoothLeAv.toString(),
                            bluetoothAdapter = bluetoothAdapter,
                            vm = BleViewModel()
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (bluetoothAdapter?.isEnabled == true) {
            requestBluetoothPermissions()
        } else {
            enableBluetooth()
            enableLocation()
        }
    }

    private fun enableBluetooth() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT // Use BLUETOOTH_CONNECT for enabling Bluetooth
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_ENABLE_BT
            )
        } else {
            // Permission already granted, proceed with enabling Bluetooth
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private fun enableLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_ENABLE_LOCATION // Define this request code constant
            )
        } else {
            // Permission already granted, you can now use location features
            // ... your code to utilize location here ...
//            val enableLocIntent = Intent(LocationManager.EXTRA_LOCATION_ENABLED)
//            startActivityForResult(enableLocIntent, REQUEST_ENABLE_LOCATION)
        }
    }



    private fun requestBluetoothPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_ENABLE_BT
            )
        } else {
            // Permissions already granted, trigger scan in DemoBLE
            // You'll need a reference to your DemoBLE Composable here
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            requestBluetoothPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_ENABLE_BT &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions granted, trigger scan in DemoBLE
            // Assuming you have a reference to your DemoBLE Composable, call its scanLeDevice function
            // For example:
            // findComposable<DemoBLE>()?.scanLeDevice()
            // (You'll need to figure out how to get the reference based on your Composable structure)
        }
    }
}


@Composable
fun DemoBLE(
    modifier: Modifier,
    bluetoothAvailable: String,
    bluetoothLeAvailable: String,
    bluetoothAdapter: BluetoothAdapter,
    vm: BleViewModel
) {
    val handler = Handler()

    // Stops scanning after 10 seconds.
    val SCAN_PERIOD: Long = 30000
    val deviceList by vm.uiState.collectAsState()
    var scanning by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

    val scanCallback: ScanCallback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d("BLE_ViewModel", "Scan CallBack")
            with(result.device) {
                Log.d(
                    "Result",
                    "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address, RSSI: ${result.rssi}"
                )
                vm.addDevice( // Update ViewModel with discovered device
                    Device(
                        name ?: "Unknown",
                        uuids?.joinToString() ?: "N/A",
                        result.rssi.toString()
                    )
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("onScanFailed", "BLE Scan failed with error code: $errorCode")
        }
    }

    fun scanLeDevice() {
        if (!scanning && bluetoothAdapter.isEnabled) {
            Log.d("BLE_ViewModel", "scanLeDevice")
            scanning = true
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // You shouldn't need to request permissions here again,
                // as it's already handled in MainActivity
                return
            }
            bluetoothLeScanner.startScan(scanCallback)
            Log.d("BLE_ViewModel", "Scan Started")

            // Optionally stop scanning after SCAN_PERIOD:
            // handler.postDelayed({
            //     scanning = false
            //     bluetoothLeScanner.stopScan(scanCallback)
            //     Log.d("BLE_ViewModel", "Scan Stopped")
            // }, SCAN_PERIOD)

        } else {
            scanning = false
            bluetoothLeScanner.stopScan(scanCallback)
            Log.d("BLE_ViewModel", "Scan Stopped")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(0.5f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Button(
                onClick = { scanLeDevice() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(text = if (scanning) "Stop Scan" else "Start Scan")
            }
        }

        // Uncomment to display the list of devices
        items(deviceList) { item ->
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Name: ${item.name}")
                Text("UUID: ${item.uuid}")
                Text("RSSI: ${item.rssi}")
            }
        }
    }
}
