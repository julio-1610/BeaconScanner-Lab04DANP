package com.idnp2024a.beaconscanner.permissions

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.idnp2024a.beaconscanner.MainActivityBLE

class BTPermissions(private val activity: MainActivityBLE) {
    private val TAG = "BTPermissions"
    private lateinit var permissionsList: ArrayList<String>
    private lateinit var alertDialog: AlertDialog

    @RequiresApi(Build.VERSION_CODES.S)
    private val permissions = arrayOf(
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.BLUETOOTH_ADVERTISE,
    )

    init {
        permissionsList = ArrayList(permissions.asList())
    }

    fun checkPermissions() {
        val missingPermissions = permissionsList.filter { !hasPermission(activity, it) }
        if (missingPermissions.isNotEmpty()) {
            askForPermissions(ArrayList(missingPermissions))
        } else {
            Log.d(TAG, "All permissions are granted!")
        }
    }

    private var permissionsLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            handlePermissionsResult(results)
        }

    private fun handlePermissionsResult(results: Map<String, Boolean>) {
        val deniedPermissions = results.filterValues { !it }.keys
        val permanentlyDeniedPermissions = deniedPermissions.filter { !activity.shouldShowRequestPermissionRationale(it) }

        if (deniedPermissions.isNotEmpty()) {
            if (permanentlyDeniedPermissions.isNotEmpty()) {
                showPermissionDialog()
            } else {
                askForPermissions(ArrayList(deniedPermissions))
            }
        } else {
            Log.d(TAG, "All permissions are granted!")
        }
    }

    private fun hasPermission(context: Context, permissionStr: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permissionStr) == PackageManager.PERMISSION_GRANTED
    }

    private fun askForPermissions(permissionsList: ArrayList<String>) {
        if (permissionsList.isNotEmpty()) {
            permissionsLauncher.launch(permissionsList.toTypedArray())
        } else {
            showPermissionDialog()
        }
    }

    private fun showPermissionDialog() {
        if (!::alertDialog.isInitialized) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
            builder.setTitle("Permission required")
                .setMessage("Some permissions are needed to use this app without any problems.")
                .setPositiveButton("Settings") { dialog, _ ->
                    dialog.dismiss()
                    // Optionally, open the app settings screen
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", activity.packageName, null)
                    intent.data = uri
                    activity.startActivity(intent)
                }
            alertDialog = builder.create()
        }

        if (!alertDialog.isShowing) {
            alertDialog.show()
        }
    }
}
