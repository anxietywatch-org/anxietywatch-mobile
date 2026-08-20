package com.anxietywatch.mobile.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.anxietywatch.mobile.network.NetworkModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

data class PairedDevice(val name: String, val address: String, val device: BluetoothDevice)

private enum class ConnectState { IDLE, CONNECTING, SUCCESS, FAILED }

private fun isWatch(device: BluetoothDevice): Boolean {
    val name = device.name?.lowercase() ?: ""
    if (name.contains("watch") || name.contains("band") || name.contains("gear")) {
        return true
    }
    return device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.WEARABLE
}

private fun getBondedWatches(context: Context): List<PairedDevice> {
    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    if (!hasPermission) return emptyList()

    return try {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        val bonded = adapter.bondedDevices ?: return emptyList()
        val result = mutableListOf<PairedDevice>()
        for (device in bonded) {
            if (isWatch(device)) {
                result.add(PairedDevice(name = device.name ?: "Reloj sin nombre", address = device.address, device = device))
            }
        }
        result
    } catch (e: SecurityException) {
        emptyList()
    }
}

private suspend fun attemptConnection(context: Context, device: BluetoothDevice): Boolean {
    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    if (!hasPermission) return false

    val result = withTimeoutOrNull(6000) {
        suspendCancellableCoroutine<Boolean> { cont ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent?.action == BluetoothDevice.ACTION_UUID) {
                        val receivedDevice = if (Build.VERSION.SDK_INT >= 33) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        if (receivedDevice?.address == device.address && cont.isActive) {
                            cont.resumeWith(Result.success(true))
                        }
                    }
                }
            }
            val filter = IntentFilter(BluetoothDevice.ACTION_UUID)
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
            cont.invokeOnCancellation {
                try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
            }
            try {
                device.fetchUuidsWithSdp()
            } catch (e: SecurityException) {
                if (cont.isActive) cont.resumeWith(Result.success(false))
            }
        }
    }
    return result ?: false
}

@Composable
fun WatchLinkScreen(modifier: Modifier = Modifier, onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf(getBondedWatches(context)) }
    var selectedDevice by remember { mutableStateOf<PairedDevice?>(null) }
    var connectState by remember { mutableStateOf(ConnectState.IDLE) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.padding(top = 16.dp).size(88.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Watch,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }

        Text(text = "Vincula tu reloj", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        Text(
            text = "Selecciona tu Galaxy Watch7 de la lista de relojes ya emparejados con este teléfono.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        if (devices.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No encontramos relojes emparejados con este teléfono.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Primero empareja tu Galaxy Watch7 desde los ajustes de Bluetooth del sistema, luego regresa aquí.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Button(
                        onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Text("Abrir ajustes de Bluetooth")
                    }
                    TextButton(
                        onClick = { devices = getBondedWatches(context) },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text("Actualizar lista")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                for (item in devices) {
                    val isSelected = selectedDevice?.address == item.address
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clickable { selectedDevice = item },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = item.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RadioButton(selected = isSelected, onClick = { selectedDevice = item })
                        }
                    }
                }
            }
        }

        when (connectState) {
            ConnectState.CONNECTING -> {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = "Vinculando con ${selectedDevice?.name}...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
            ConnectState.SUCCESS -> {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    Text(text = "¡Conectado exitosamente!", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Button(onClick = onFinished, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Text("Continuar")
                    }
                }
            }
            ConnectState.FAILED -> {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
                    Text(
                        text = "No pudimos conectar con el reloj. Verifica que esté cerca y encendido.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Button(onClick = { connectState = ConnectState.IDLE }, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Text("Intentar de nuevo")
                    }
                }
            }
            ConnectState.IDLE -> {
                Button(
                    onClick = {
                        val chosen = selectedDevice ?: return@Button
                        connectState = ConnectState.CONNECTING
                        scope.launch {
                            val success = attemptConnection(context, chosen.device)
                            if (success) {
                                NetworkModule.getSessionManager().saveLinkedWatchAddress(chosen.address)
                                NetworkModule.getSessionManager().setWatchStepDone()
                                connectState = ConnectState.SUCCESS
                            } else {
                                connectState = ConnectState.FAILED
                            }
                        }
                    },
                    enabled = selectedDevice != null,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                ) {
                    Text("Vincular dispositivo")
                }
                TextButton(
                    onClick = {
                        NetworkModule.getSessionManager().setWatchStepDone()
                        onFinished()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Vincular después")
                }
            }
        }
    }
}