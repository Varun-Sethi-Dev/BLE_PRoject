package com.varun.ble_project

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.collection.mutableIntListOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


data class BLEDevice(
    val name: String,
    val rssi: String
)

private const val PERMISSION_REQUEST_CODE = 1
var rssi_P1 = 1;
var rssi_P2 = 1;
var rssi_P3 = 1;
var arrayRssi_P1 = mutableIntListOf()
var arrayRssi_P2 = mutableIntListOf()
var arrayRssi_P3 = mutableIntListOf()
var showList = mutableStateOf(true)

class MainActivity : ComponentActivity() {
    private var isScanning = mutableStateOf(false)
    private val list = mutableStateListOf<BLEDevice>()
    private val mainScope = CoroutineScope(Dispatchers.Default)
    private val serviceUuid_Peripheral_1: ParcelUuid =
        ParcelUuid.fromString("a4478767-5f86-4ba5-9959-9256f0ee9e30")

    private val serviceUuid_Peripheral_2: ParcelUuid =
        ParcelUuid.fromString("1d3469e7-009c-41a1-a70d-6d62fe9fa042")
    private val serviceUuid_Peripheral_3: ParcelUuid =
        ParcelUuid.fromString("1166f120-dfe2-4794-a69a-515a9b923f33")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent() {
            MaterialTheme {
                val sDKVersion = Build.VERSION.SDK_INT
//                val nav = rememberNavController()
//                NavHost(nav, startDestination = "MainActivity"){
//                    composable("MainActivity"){
//
//                    }
//                }
                val focusManager = LocalFocusManager.current
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                focusManager.clearFocus()
                            },
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current device sdk =  $sDKVersion",
                            modifier = Modifier.padding(12.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .align(Alignment.CenterHorizontally),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (isScanning.value) {
                                        stopBleScan()

                                    } else {
                                        startBleScan()
                                    }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 24.dp),

                                ) {
                                Text(text = if (!isScanning.value) "Start Scan" else "Stop Scan")
                            }
                            Button(
                                onClick = {
                                    if (list.isNotEmpty()) {
                                        list.clear()
                                        arrayRssi_P1.clear()
                                        arrayRssi_P2.clear()
                                        arrayRssi_P3.clear()
                                    }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 24.dp),

                                ) {
                                Text(text = "Clear All Values")
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .align(Alignment.CenterHorizontally),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    showList.value = true
                                },
                                modifier = Modifier
                                    .padding(horizontal = 24.dp),

                                ) {
                                Text(text = "Go Back")
                            }
                            Button(
                                onClick = {
                                    showList.value = false
                                },
                                modifier = Modifier
                                    .padding(horizontal = 24.dp),

                                ) {
                                Text(text = "Show Results")
                            }
                        }
                        val lazyListState = rememberLazyListState()

                        AnimatedVisibility(visible = showList.value) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                state = lazyListState
                            ) {
                                item { Text(text = "Peripheral_1") }
                                items(list) { bleItem ->
                                    if (bleItem.name == "Peripheral_1") {
                                        Text(
                                            text = "Name: ${bleItem.name}, rssi: ${bleItem.rssi}",
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                        LaunchedEffect(list.size) {
                                            if (list.isNotEmpty()) {
                                                lazyListState.animateScrollToItem(list.lastIndex)
                                            }
                                        }
                                    }

                                }
                                item { Text(text = "Peripheral_2") }

                                items(list) { bleItem ->
                                    if (bleItem.name == "Peripheral_2") {

                                        Text(
                                            text = "Name: ${bleItem.name}, rssi: ${bleItem.rssi}",
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                        LaunchedEffect(list.size) {
                                            if (list.isNotEmpty()) {
                                                lazyListState.animateScrollToItem(list.lastIndex)
                                            }
                                        }
                                    }
                                }
                                item { Text(text = "Peripheral_3") }

                                items(list) { bleItem ->
                                    if (bleItem.name == "Peripheral_3") {

                                        Text(
                                            text = "Name: ${bleItem.name}, rssi: ${bleItem.rssi}",
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                        LaunchedEffect(list.size) {
                                            if (list.isNotEmpty()) {
                                                lazyListState.animateScrollToItem(list.lastIndex)
                                            }
                                        }
                                    }
                                }

                            }
                        }
                        AnimatedVisibility(visible = !showList.value) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AnalysingResultScreen()
                            }
                        }

                    }

                }
            }
        }
    }


    private val scanSettings =
        ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

    private val scanCallback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            with(result.device) {

                if (arrayRssi_P1.size != 10 && name == "Peripheral_1") {
                    rssi_P1 = result.rssi
                    arrayRssi_P1.add(result.rssi)
                    Log.i(
                        "Peripheral_1",
                        "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address, rssi: ${result.rssi}"
                    )
                    list.add(
                        BLEDevice(
                            name = result.device.name.toString(),
                            rssi = result.rssi.toString()
                        )
                    )
                } else if (arrayRssi_P2.size != 10 && name == "Peripheral_2") {
                    rssi_P2 = result.rssi
                    arrayRssi_P2.add(result.rssi)
                    Log.i(
                        "Peripheral_2",
                        "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address, rssi: ${result.rssi}"
                    )
                    list.add(
                        BLEDevice(
                            name = result.device.name.toString(),
                            rssi = result.rssi.toString()
                        )
                    )
                } else if (arrayRssi_P3.size != 10 && name == "Peripheral_3") {
                    rssi_P3 = result.rssi
                    arrayRssi_P3.add(result.rssi)
                    Log.i(
                        "Peripheral_3",
                        "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address, rssi: ${result.rssi}"
                    )
                    list.add(
                        BLEDevice(
                            name = result.device.name.toString(),
                            rssi = result.rssi.toString()
                        )
                    )
                } else if (arrayRssi_P2.size + arrayRssi_P2.size + arrayRssi_P3.size == 30) {
                    stopBleScan()
                } else {
                    Log.d("MainActivity", "else")
                }


            }
        }
    }

    private var scanFilterP1: ScanFilter =
        ScanFilter.Builder().setServiceUuid(serviceUuid_Peripheral_1).build()
    private var scanFilterP2: ScanFilter =
        ScanFilter.Builder().setServiceUuid(serviceUuid_Peripheral_2).build()
    private var scanFilterP3: ScanFilter =
        ScanFilter.Builder().setServiceUuid(serviceUuid_Peripheral_3).build()


    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        if (!hasRequiredBluetoothPermissions()) {
            requestRelevantRuntimePermissions()
        } else { /* TODO: Actually perform scan */
            isScanning.value = true
            bleScanner.startScan(
                listOf(scanFilterP1, scanFilterP2, scanFilterP3),
                scanSettings,
                scanCallback
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning.value = false
    }

    private fun Activity.requestRelevantRuntimePermissions() {
        if (hasRequiredBluetoothPermissions()) {
            return
        }
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                requestLocationPermission()
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                requestBluetoothPermissions()
            }
        }
    }

    private fun requestLocationPermission() = runOnUiThread {
        AlertDialog.Builder(this).setTitle("Location permission required").setMessage(
            "Starting from Android M (6.0), the system requires apps to be granted " + "location access in order to scan for BLE devices."
        ).setCancelable(false).setPositiveButton(android.R.string.ok) { _, _ ->
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE
            )
        }.show()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestBluetoothPermissions() = runOnUiThread {
        AlertDialog.Builder(this).setTitle("Bluetooth permission required").setMessage(
            "Starting from Android 12, the system requires apps to be granted " + "Bluetooth access in order to scan for and connect to BLE devices."
        ).setCancelable(false).setPositiveButton(android.R.string.ok) { _, _ ->
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT
                ), PERMISSION_REQUEST_CODE
            )
        }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST_CODE) return
        val containsPermanentDenial = permissions.zip(grantResults.toTypedArray()).any {
            it.second == PackageManager.PERMISSION_DENIED && !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                it.first
            )
        }
        val containsDenial = grantResults.any { it == PackageManager.PERMISSION_DENIED }
        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        when {
            containsPermanentDenial -> {
                Log.d("MainActivity", "Permission Denied")
            }

            containsDenial -> {
                requestRelevantRuntimePermissions()
            }

            allGranted && hasRequiredBluetoothPermissions() -> {
                startBleScan()
            }

            else -> {
                Log.d("MainActivity", "Unexpected Error occurred")
                recreate()
            }
        }
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bluetoothEnablingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
        } else {
            promptEnableBluetooth()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }


    private fun promptEnableBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                bluetoothEnablingResult.launch(this)
            }
        }
    }

}


fun Context.hasPermission(permissionType: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this, permissionType
    ) == PackageManager.PERMISSION_GRANTED
}

fun Context.hasRequiredBluetoothPermissions(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hasPermission(Manifest.permission.BLUETOOTH_SCAN) && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}