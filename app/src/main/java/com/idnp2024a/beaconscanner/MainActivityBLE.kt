package com.idnp2024a.beaconscanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.location.LocationManagerCompat
import com.idnp2024a.beaconscanner.permissions.BTPermissions
import com.idnp2024a.beaconscanner.permissions.Permission
import com.idnp2024a.beaconscanner.permissions.PermissionManager


class MainActivityBLE : AppCompatActivity() {

    private val TAG: String = "MainActivityBLE"
    private var alertDialog: AlertDialog? = null
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var btScanner: BluetoothLeScanner
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var txtMessage: TextView;
    private val permissionManager = PermissionManager.from(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_main_ble)
        /*        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }*/

        BTPermissions(this).check()
        initBluetooth()

        val btnAdversting = findViewById<Button>(R.id.btnAdversting)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        txtMessage = findViewById(R.id.txtMessage)


        val bleScanCallback = BleScanCallback(
            onScanResultAction,
            onBatchScanResultAction,
            onScanFailedAction
        )

        btnAdversting.setOnClickListener {
//            val iBeaconEmissor=IBeaconEmissor(applicationContext)
//            iBeaconEmissor.emissor()
        }

        btnStart.setOnClickListener {
            if (isLocationEnabled()) {
                bluetoothScanStart(bleScanCallback)
            } else {
                showPermissionDialog()
            }
        }

        btnStop.setOnClickListener {
            bluetoothScanStop(bleScanCallback)
        }

    }

    fun initBluetooth() {

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager!!.adapter

        if (bluetoothAdapter != null) {
            btScanner = bluetoothAdapter.bluetoothLeScanner
        } else {
            Log.d(TAG, "BluetoothAdapter is null")
        }
    }

    private fun bluetoothScanStart(bleScanCallback: BleScanCallback) {
        Log.d(TAG, "btScan ...1")
        if (btScanner != null) {
            Log.d(TAG, "btScan ...2")
            permissionManager
                .request(Permission.Location)
                .rationale("Bluetooth permission is needed")
                .checkPermission { isgranted ->
                    if (isgranted) {
                        btScanner!!.startScan(bleScanCallback)
                    } else {
                        Log.d(TAG, "Alert you don't have Bluetooth permission")
                    }

                }

        } else {
            Log.d(TAG, "btScanner is null")
        }

    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }


    @SuppressLint("MissingPermission")
    private fun bluetoothScanStop(bleScanCallback: BleScanCallback) {
        Log.d(TAG, "btScan ...1")
        if (btScanner != null) {
            Log.d(TAG, "btScan ...2")
            btScanner!!.stopScan(bleScanCallback)

        } else {
            Log.d(TAG, "btScanner is null")
        }

    }

    @SuppressLint("MissingPermission")
    val onScanResultAction: (ScanResult?) -> Unit = { result ->
        val scanRecord = result?.scanRecord
        val beacon = Beacon(result?.device?.address)
        beacon.manufacturer = result?.device?.name
        beacon.rssi = result?.rssi
        if (scanRecord != null && beacon.manufacturer == "ESP32 Beacon") {
            scanRecord?.bytes?.let { decodeiBeacon(it, beacon.rssi) }

            /*
            Log.d(TAG, "ScanRecord:" + scanRecord?.bytes?.let { Utils.toHexString(it) })
            val iBeaconManufactureData =
                scanRecord.getManufacturerSpecificData(0X004c)// fake Apple 0x004C LSB (ENDIAN_CHANGE_U16!)

            if (iBeaconManufactureData != null && iBeaconManufactureData.size >= 23) {
                Log.d(TAG, "ManufacturerSpecificData:" + Utils.toHexString(iBeaconManufactureData))
                val iBeaconUUID = Utils.toHexString(iBeaconManufactureData.copyOfRange(2, 18))
                val major = Integer.parseInt(
                    Utils.toHexString(iBeaconManufactureData.copyOfRange(18, 20)),
                    16
                )
                val minor = Integer.parseInt(
                    Utils.toHexString(iBeaconManufactureData.copyOfRange(20, 22)),
                    16
                )
                val txPower = Integer.parseInt(
                    Utils.toHexString(iBeaconManufactureData.copyOfRange(22, 23)),
                    16
                )

                beacon.type = Beacon.beaconType.iBeacon
                beacon.uuid = iBeaconUUID
                beacon.major = major
                beacon.minor = minor

                var beacon_manufacturer = beacon.manufacturer
                var beacon_rssi = beacon.rssi
                var beacon_type = beacon.type
                Log.e(
                    TAG,
                    "manufacturer:$beacon_manufacturer rssi:$beacon_rssi type:$beacon_type iBeaconUUID:$iBeaconUUID major:$major minor:$minor txPower:$txPower"
                )
            }
            */
        }

    }

    val onBatchScanResultAction: (MutableList<ScanResult>?) -> Unit = {
        if (it != null) {
            Log.d(TAG, "BatchScanResult " + it.toString())
        }
    }

    val onScanFailedAction: (Int) -> Unit = {
        Log.d(TAG, "ScanFailed " + it.toString())
    }

    fun decodeiBeacon(data: ByteArray, rssi: Int?) {
        val data_len = Integer.parseInt(Utils.toHexString(data.copyOfRange(0, 1)), 16)
        val data_type = Integer.parseInt(Utils.toHexString(data.copyOfRange(1, 2)), 16)
        val LE_flag = Integer.parseInt(Utils.toHexString(data.copyOfRange(2, 3)), 16)
        val len = Integer.parseInt(Utils.toHexString(data.copyOfRange(3, 4)), 16)
        val type = Integer.parseInt(Utils.toHexString(data.copyOfRange(4, 5)), 16)
        val company = Utils.toHexString(data.copyOfRange(5, 7))
        val subtype = Integer.parseInt(Utils.toHexString(data.copyOfRange(7, 8)), 16)
        val subtypelen = Integer.parseInt(Utils.toHexString(data.copyOfRange(8, 9)), 16)
        val iBeaconUUID = Utils.toHexString(data.copyOfRange(9, 25))
        val major = Integer.parseInt(Utils.toHexString(data.copyOfRange(25, 27)), 16)
        val minor = Integer.parseInt(Utils.toHexString(data.copyOfRange(27, 29)), 16)
        val txPower = Integer.parseInt(Utils.toHexString(data.copyOfRange(29, 30)), 16)

        //var factor = (-40 - rssi!!)/(10*2.0)
        var factor = (-1 * txPower - rssi!!) / (10 * 4.0)
        var distance = Math.pow(10.0, factor)
        //var rssi2= movingAverage.next(rssi)

        var display = "TxPower:$txPower \nRSSI:$rssi \nRSSI2:rssi2 \nDistance:$distance"
        txtMessage.setText(display)

        Log.d(
            TAG,
            "DECODE data_len:$data_len data_type:$data_type LE_flag:$LE_flag len:$len type:$type subtype:$subtype subtype_len:$subtypelen company:$company UUID:$iBeaconUUID major:$major minor:$minor txPower:$txPower"
        )


    }

    private fun showPermissionDialog() {

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Alerta")
            .setMessage("El servicio de localizacion no esta activo")
            .setPositiveButton("Close") { dialog, which ->
                dialog.dismiss()
            }

        if (alertDialog == null) {
            alertDialog = builder.create()
        }

        if (!alertDialog!!.isShowing()) {
            alertDialog!!.show()
        }
    }

}