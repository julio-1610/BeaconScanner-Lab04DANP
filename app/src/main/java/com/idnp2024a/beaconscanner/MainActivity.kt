package com.idnp2024a.beaconscanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    val PERMISSION_REQUEST_COARSE_LOCATION = 1000
    val REQUEST_ENABLE_BT = 100
    var btManager: BluetoothManager? = null;
    var btScanner: BluetoothLeScanner? = null

    private var btPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        /*       ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                   val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                   v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                   insets
               }*/

        //setUpBluetoothManager()
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

        //checkForLocationPermission()
        setUpBluetoothManager()

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            btScanner!!.startScan(leScanCallback)
        }


    }

    private fun checkForLocationPermission() {
        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val builder = AlertDialog.Builder(applicationContext)
            builder.setTitle("This app needs location access")
            builder.setMessage("Please grant location access so this app can detect  peripherals.")
            builder.setPositiveButton(android.R.string.ok, null)
            builder.setOnDismissListener {
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                    PERMISSION_REQUEST_COARSE_LOCATION
                )
                /*                requestPermissions(
                                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                                    PERMISSION_REQUEST_COARSE_LOCATION
                                )*/
            }
            builder.show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_COARSE_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    println("coarse location permission granted")
                } else {
                    val builder = AlertDialog.Builder(applicationContext)
                    builder.setTitle("Functionality limited")
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover BLE beacons")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener { }
                    builder.show()
                }
                return
            }
        }
    }

    private fun setUpBluetoothManager() {
        //btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btManager = getSystemService(BluetoothManager::class.java) as BluetoothManager
        val btAdapter = btManager!!.adapter
        btScanner = btAdapter?.bluetoothLeScanner
        Log.d("MainActivity", "setUpBluetoothManager 1")
        if (btAdapter != null && !btAdapter!!.isEnabled) {
            Log.d("MainActivity", "setUpBluetoothManager 2")
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            btActivityResultLauncher.launch(enableIntent)
            //startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        }


        checkForLocationPermission()
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            //Result will be retrieved here

            Log.d("MainActivity", result.rssi.toString())
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.BLUETOOTH_CONNECT
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
            Log.d("MainActivity", "----------------")
            Log.d("MainActivity", result.device.name.toString())
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d("MainActivity", "Error" + errorCode.toString())
        }
    }

    /*    private fun onStartScannerButtonClick() {
            startButton.visibility = View.GONE
            stopButton.visibility = View.VISIBLE
            btScanner!!.startScan(leScanCallback)
        }

        private fun onStopScannerButtonClick() {
            stopButton.visibility = View.GONE
            startButton.visibility = View.VISIBLE
            btScanner!!.stopScan(leScanCallback)
        }*/

    private val blueToothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
            val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
            btPermission = true

            if (bluetoothAdapter?.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                btActivityResultLauncher.launch(enableBtIntent)
            } else {
                btScan()
            }
        } else {
            btPermission = false
        }
    }

    private val btActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            btScan()
        }
    }

    private fun btScan() = Toast.makeText(this, "BT接続できます", Toast.LENGTH_LONG).show()


}