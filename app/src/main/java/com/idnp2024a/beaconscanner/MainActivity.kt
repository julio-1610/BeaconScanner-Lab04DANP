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

    private val PERMISSION_REQUEST_COARSE_LOCATION = 1000
    private var btManager: BluetoothManager? = null
    private var btScanner: BluetoothLeScanner? = null

    private var btPermission = false

    private val kalmanFilter = KalmanFilter(processNoise = 1e-5, measurementNoise = 1e-1, estimateError = 1.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpBluetoothManager()

        findViewById<Button>(R.id.button).setOnClickListener {
            if (checkPermissions()) {
                btScanner?.startScan(leScanCallback)
            } else {
                requestPermissions()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val hasBluetoothScanPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
        val hasBluetoothConnectPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return hasBluetoothScanPermission && hasBluetoothConnectPermission && hasLocationPermission
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_COARSE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                btScanner?.startScan(leScanCallback)
            } else {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Functionality limited")
                builder.setMessage("Since location and Bluetooth access have not been granted, this app will not be able to discover BLE beacons")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.show()
            }
        }
    }

    private fun setUpBluetoothManager() {
        btManager = getSystemService(BluetoothManager::class.java)
        val btAdapter = btManager?.adapter
        btScanner = btAdapter?.bluetoothLeScanner

        if (btAdapter != null && !btAdapter.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            btActivityResultLauncher.launch(enableIntent)
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                handleScanResult(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d("MainActivity", "Scan failed with error: $errorCode")
        }
    }

    private fun handleScanResult(result: ScanResult) {
        Log.d("MainActivity", "RSSI: ${result.rssi}")
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val filteredRssi = kalmanFilter.filter(result.rssi.toDouble())
        Log.d("MainActivity", "Filtered RSSI: $filteredRssi, Device: ${result.device.name}")
    }

    private val btActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            btScanner?.startScan(leScanCallback)
        }
    }
}
