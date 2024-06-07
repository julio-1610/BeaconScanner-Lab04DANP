package com.idnp2024a.beaconscanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    // Se define el código de solicitud de permisos de ubicación.
    private val PERMISSION_REQUEST_COARSE_LOCATION = 1000
    // Se inicializan las variables relacionadas con Bluetooth.
    private var btManager: BluetoothManager? = null
    private var btScanner: BluetoothLeScanner? = null

    // Se establece un indicador de permiso Bluetooth.
    private var btPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Se inicializa el gestor de Bluetooth.
        setUpBluetoothManager()

        // Se establece el clic del botón para iniciar el escaneo Bluetooth.
        findViewById<Button>(R.id.button).setOnClickListener {
            startBluetoothScan()
        }
    }

    // Función para inicializar el gestor de Bluetooth.
    private fun setUpBluetoothManager() {
        // Se obtiene el gestor de Bluetooth del sistema.
        btManager = getSystemService(BluetoothManager::class.java)
        // Se obtiene el adaptador Bluetooth del gestor.
        val btAdapter = btManager?.adapter
        // Se obtiene el escáner Bluetooth del adaptador.
        btScanner = btAdapter?.bluetoothLeScanner

        // Si el adaptador Bluetooth no está habilitado, se solicita habilitarlo.
        if (btAdapter != null && !btAdapter.isEnabled) {
            requestEnableBluetooth()
        }

        // Se verifica si se tienen permisos de ubicación.
        checkForLocationPermission()
    }

    // Función para verificar y solicitar permisos de ubicación.
    private fun checkForLocationPermission() {
        // Si no se tienen permisos de ubicación, se solicitan.
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
        }
    }

    // Función para solicitar permisos de ubicación.
    private fun requestLocationPermission() {
        // Se muestra un diálogo para solicitar permisos de ubicación.
        val builder = AlertDialog.Builder(this)
        builder.setTitle("This app needs location access")
        builder.setMessage("Please grant location access so this app can detect peripherals.")
        builder.setPositiveButton(android.R.string.ok, null)
        builder.setOnDismissListener {
            // Se solicitan los permisos de ubicación al usuario.
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_COARSE_LOCATION
            )
        }
        builder.show()
    }

    // Función para solicitar habilitar Bluetooth.
    private fun requestEnableBluetooth() {
        val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        // Se utiliza un ActivityResultLauncher para manejar el resultado de la solicitud.
        btActivityResultLauncher.launch(enableIntent)
    }

    // Manejador de resultados de la solicitud de habilitar Bluetooth.
    private val btActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Si la solicitud fue exitosa, se inicia el escaneo Bluetooth.
        if (it.resultCode == RESULT_OK) {
            startBluetoothScan()
        }
    }

    // Función para iniciar el escaneo Bluetooth.
    private fun startBluetoothScan() {
        // Si se tienen permisos de ubicación, se inicia el escaneo.
        if (btPermission) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            btScanner?.startScan(leScanCallback)
        } else {
            // Si no se tienen permisos de ubicación, se muestra un mensaje.
            Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show()
        }
    }

    // Callback para manejar los resultados de los escaneos Bluetooth.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // Se muestra la información del dispositivo Bluetooth escaneado.
            Log.d("MainActivity", "RSSI: ${result.rssi}")

            // Aquí se accede al contexto de la actividad mediante la referencia a this@MainActivity
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Considerar solicitar los permisos faltantes aquí
                return
            }
            Log.d("MainActivity", "Device: ${result.device.name}")
        }

        override fun onScanFailed(errorCode: Int) {
            // Se muestra un mensaje si el escaneo falla.
            Log.d("MainActivity", "Scan failed with error: $errorCode")
        }
    }


    // Función para manejar los resultados de las solicitudes de permisos.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Si se solicitó permisos de ubicación, se actualiza el indicador de permisos.
        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                btPermission = true
            } else {
                // Si los permisos de ubicación no fueron otorgados, se muestra un mensaje.
                Toast.makeText(
                    this,
                    "Location permission not granted, functionality limited.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
