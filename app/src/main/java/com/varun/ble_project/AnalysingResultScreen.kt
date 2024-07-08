package com.varun.ble_project

import TrilaterationRequest
import TrilaterationResponse
import TrilaterationService
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.round

@Preview(showBackground = true)
@Composable
fun AnalysingResultScreen(modifier: Modifier = Modifier) {
    var isLongestRssi by remember { mutableStateOf(true) }
    var calculate by remember { mutableStateOf(false) }
    var isDropExpanded by remember { mutableStateOf(false) }
    var peripheralSelected by remember {
        mutableIntStateOf(0)
    }
    var tfv by remember {
        mutableStateOf("")
    }
    var bestRssi by remember {
        mutableIntStateOf(if (isLongestRssi) 1 else -100)
    }
    var bestRssiP1 by remember {
        mutableIntStateOf(0)
    }
    var bestRssiP2 by remember {
        mutableIntStateOf(0)
    }
    var bestRssiP3 by remember {
        mutableIntStateOf(0)
    }
    var errorFactorString by remember {
        mutableStateOf("")
    }
    var errorFactor by remember {
        mutableDoubleStateOf(0.0)
    }
    var environmentFactorString by remember {
        mutableStateOf("2")
    }
    var environmentFactor by remember {
        mutableIntStateOf(2)
    }

    if (arrayRssi_P1.isNotEmpty() && arrayRssi_P2.isNotEmpty() && arrayRssi_P3.isNotEmpty()) {
        arrayRssi_P1.forEach {
            if (if (isLongestRssi) (it < bestRssi) else (it > bestRssi)) {
                bestRssi = it
            }
            bestRssiP1 = bestRssi
        }
        bestRssi = if (isLongestRssi) 1 else -100
        arrayRssi_P2.forEach {
            if (if (isLongestRssi) (it < bestRssi) else (it > bestRssi)) {
                bestRssi = it
            }
            bestRssiP2 = bestRssi
        }
        bestRssi = if (isLongestRssi) 1 else -100
        arrayRssi_P3.forEach {
            if (if (isLongestRssi) (it < bestRssi) else (it > bestRssi)) {
                bestRssi = it
            }
            bestRssiP3 = bestRssi
        }
        bestRssi = if (isLongestRssi) 1 else -100
    }

    var valX by remember {
        mutableStateOf("0")
    }

    var valY by remember {
        mutableStateOf("0")
    }

    var valZ by remember {
        mutableStateOf("0")
    }
    var cP1x by remember {
        mutableIntStateOf(0)
    }
    var cP1y by remember {
        mutableIntStateOf(0)
    }
    var cP1z by remember {
        mutableIntStateOf(0)
    }
    var cP2x by remember {
        mutableIntStateOf(0)
    }
    var cP2y by remember {
        mutableIntStateOf(0)
    }
    var cP2z by remember {
        mutableIntStateOf(0)
    }
    var cP3x by remember {
        mutableIntStateOf(0)
    }
    var cP3y by remember {
        mutableIntStateOf(0)
    }
    var cP3z by remember {
        mutableIntStateOf(0)
    }
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "With closest Rssi value",
            )
            Checkbox(
                checked = !isLongestRssi, onCheckedChange = { isLongestRssi = !isLongestRssi }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = tfv,
                    placeholder = { Text(text = " Select Peripheral") },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "",
                            modifier = Modifier
                                .clickable {
                                    isDropExpanded = true
                                }
                                .border(2.dp, Color.Black)
                        )
                    },
                    modifier = Modifier
                        .clickable {
                            isDropExpanded = true
                        }
                        .padding(vertical = 4.dp)
                        .border(2.dp, Color.Black)
                )
                DropdownMenu(
                    expanded = isDropExpanded,
                    onDismissRequest = { isDropExpanded = !isDropExpanded },
                    offset = DpOffset(x = 200.dp, y = (-200).dp)
                ) {
                    DropdownMenuItem(text = { Text(text = "Peripheral_1") }, onClick = {
                        peripheralSelected = 1
                        tfv = "Peripheral_1"
                        isDropExpanded = !isDropExpanded
                    })
                    DropdownMenuItem(text = { Text(text = "Peripheral_2") }, onClick = {
                        peripheralSelected = 2
                        tfv = "Peripheral_2"
                        isDropExpanded = !isDropExpanded
                    })
                    DropdownMenuItem(text = { Text(text = "Peripheral_3") }, onClick = {
                        peripheralSelected = 3
                        tfv = "Peripheral_3"
                        isDropExpanded = !isDropExpanded
                    })
                }

                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(value = errorFactorString, onValueChange = {
                        errorFactorString = it
                        errorFactor =
                            if (errorFactorString == "") 0.0 else errorFactorString.toDouble()
                    },
                        placeholder = {
                            Text(text = "Error Factor in Distance")
                        },
                        modifier = Modifier.weight(1f),

                    )
                    OutlinedTextField(
                        value = environmentFactorString,
                        onValueChange = {
                            environmentFactorString = it
                            environmentFactor =
                                if (errorFactorString == "") 0 else environmentFactorString.toInt()
                        },
                        placeholder = {
                            Text(text = "Environment Factor")
                        },
                        modifier = Modifier.weight(1f),

                        )
                }
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {

                    OutlinedTextField(
                        value = valX,
                        onValueChange = {
                            valX = it
                        },
                        label = {
                            Text(text = "value of x")
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = peripheralSelected != 0
                    )
                    OutlinedTextField(
                        value = valY,
                        onValueChange = {
                            valY = it

                        },
                        label = {
                            Text(text = "value of y")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = peripheralSelected != 0

                    )
                    OutlinedTextField(
                        value = valZ,
                        onValueChange = {
                            valZ = it
                        },
                        label = {
                            Text(text = "value of z")
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = peripheralSelected != 0

                    )


                }
                when (peripheralSelected) {
                    1 -> {
                        cP1x = if (valX != "") valX.toInt() else 0
                        cP1y = if (valY != "") valY.toInt() else 0
                        cP1z = if (valZ != "") valZ.toInt() else 0
                    }

                    2 -> {
                        cP2x = if (valX != "") valX.toInt() else 0
                        cP2y = if (valY != "") valY.toInt() else 0
                        cP2z = if (valZ != "") valZ.toInt() else 0
                    }

                    3 -> {
                        cP3x = if (valX != "") valX.toInt() else 0
                        cP3y = if (valY != "") valY.toInt() else 0
                        cP3z = if (valZ != "") valZ.toInt() else 0
                    }


                }
                Text(text = "Peripheral_1 x:${cP1x} y:${cP1y} z:${cP1z}")
                Text(text = "Peripheral_2 x:${cP2x} y:${cP2y} z:${cP2z}")
                Text(text = "Peripheral_3 x:${cP3x} y:${cP3y} z:${cP3z}")
            }
        }
        val distanceP1 = calculateDistance(
            bestRssiP1, environmentFactor
        )
        val distanceP2 = calculateDistance(
            bestRssiP2, environmentFactor
        )
        val distanceP3 = calculateDistance(
            bestRssiP3, environmentFactor
        )
        Text(
            text = "${
                if (isLongestRssi) {
                    "Largest"
                } else {
                    "Smallest"
                }
            } Rssi for P1 : $bestRssiP1,\nDistance from P1 : ${distanceP1.round(2) - errorFactor}m.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
        Text(
            text = "${
                if (isLongestRssi) {
                    "Largest"
                } else {
                    "Smallest"
                }
            } Rssi for P2 : $bestRssiP2,\nDistance from P2 : ${distanceP2.round(2) - errorFactor}m.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
        Text(
            text = "${
                if (isLongestRssi) {
                    "Largest"
                } else {
                    "Smallest"
                }
            } Rssi for P3 : $bestRssiP3,\nDistance from P3 : ${distanceP3.round(2) - errorFactor}m.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                val service =
                    TrilaterationService("https://api-omega-pied-50.vercel.app/") // Replace with your API URL
                val requestData = TrilaterationRequest(
                    listOf(listOf(2.0, 4.0, 1.0), listOf(8.0, 2.0, 3.0), listOf(5.0, 7.0, 5.0)),
                    listOf(6.0, 4.0, 7.0)
                )
                var estimatedLocation: TrilaterationResponse? = null
                estimatedLocation = service.getEstimatedLocation(requestData)
                if (estimatedLocation != null) {
                    Log.d(
                        "Trilateration",
                        "Estimated Location: (x, y, z) = (${estimatedLocation.x}," +
                                " ${estimatedLocation.y}, ${estimatedLocation.z})"
                    )
                } else {
                    Log.e("Trilateration", "Error fetching estimated location")
                }
            }

            calculate != calculate
        }) {
            Text(text = "Calculate Coordinated")
        }

    }
}

fun calculateDistance(rssi: Int, environmentFactor: Int): Double {
    val txPower = +8 // Replace with actual transmission power of your beacons (dBm)
    val pathLoss = txPower - rssi

//    val environmentFactor = 6  // Assuming environment factor (n) = 5 for indoor

    // Path loss equation considering environment factor
    val distance = 10.0.pow((pathLoss - 40.0) / (10.0 * environmentFactor))
    return distance
}

fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) {
        multiplier *= 10
    }
    return round(this * multiplier) / multiplier

}


