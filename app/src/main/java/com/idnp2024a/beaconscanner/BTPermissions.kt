package com.idnp2024a.beaconscanner.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.idnp2024a.beaconscanner.MainActivityBLE

class BTPermissions(private val activity: MainActivityBLE) {
    private val permissions = arrayOf(
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.BLUETOOTH_ADVERTISE
    )

    private val permissionsLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val deniedPermissions = result.filterValues { !it }.keys
            if (deniedPermissions.isNotEmpty()) {
                askForPermissions(deniedPermissions.toList())
            } else {
                Log.d("BTPermissions", "All permissions are granted!")
            }
        }

    fun check() {
        askForPermissions(permissions.toList())
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun askForPermissions(permissionsList: List<String>) {
        val newPermissions = permissionsList.filter { !hasPermission(activity, it) }
        if (newPermissions.isNotEmpty()) {
            permissionsLauncher.launch(newPermissions.toTypedArray())
        } else {
            Log.d("BTPermissions", "All permissions are already granted!")
        }
    }
}
