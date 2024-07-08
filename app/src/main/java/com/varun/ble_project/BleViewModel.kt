package com.varun.ble_project

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Device(
    val name: String,
    val uuid: String,
    val rssi: String
)

class BleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<List<Device>>(emptyList())
    val uiState = _uiState.asStateFlow()

    fun addDevice(device: Device) {
        viewModelScope.launch {
            _uiState.emit(_uiState.value + device)
        }
    }

    fun onRequestPermissionsError(context: Context) {
        Log.d("BLE_ViewModel","onRequestPermissionsError")
        Toast.makeText(context,"permission denial scenario",Toast.LENGTH_SHORT).show()
    }

}