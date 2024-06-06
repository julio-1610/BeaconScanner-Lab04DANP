package com.idnp2024a.beaconscanner.permissions

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PermissionManager private constructor(private val activity: AppCompatActivity) {
    private val requiredPermissions = mutableListOf<Permission>()
    private var rationale: String? = null
    private var callback: (Boolean) -> Unit = {}
    private var detailedCallback: (Map<Permission, Boolean>) -> Unit = {}

    private val permissionCheck = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        sendResultAndCleanUp(grantResults)
    }

    companion object {
        fun from(activity: AppCompatActivity) = PermissionManager(activity)
    }

    fun rationale(description: String): PermissionManager {
        rationale = description
        return this
    }

    fun request(vararg permission: Permission): PermissionManager {
        requiredPermissions.addAll(permission)
        return this
    }

    fun checkPermission(callback: (Boolean) -> Unit) {
        this.callback = callback
        handlePermissionRequest()
    }

    private fun handlePermissionRequest() {
        activity.let {
            when {
                areAllPermissionsGranted(it) -> sendPositiveResult()
                shouldShowPermissionRationale(it) -> displayRationale(it)
                else -> requestPermissions()
            }
        }
    }

    private fun displayRationale(activity: AppCompatActivity) {
        AlertDialog.Builder(activity.applicationContext)
            .setTitle("Permiso")
            .setMessage(rationale ?: "fragment.getString(R.string.dialog_permission_default_message)")
            .setCancelable(false)
            .setPositiveButton("Aceptar") { _, _ ->
                requestPermissions()
            }
            .show()
    }

    private fun sendPositiveResult() {
        sendResultAndCleanUp(getPermissionList().associate { it to true } )
    }

    private fun sendResultAndCleanUp(grantResults: Map<String, Boolean>) {
        callback(grantResults.all { it.value })
        detailedCallback(grantResults.mapKeys { Permission.from(it.key) })
        cleanUp()
    }

    private fun cleanUp() {
        requiredPermissions.clear()
        rationale = null
        callback = {}
        detailedCallback = {}
    }

    private fun requestPermissions() {
        permissionCheck?.launch(getPermissionList())
    }

    private fun areAllPermissionsGranted(activity: AppCompatActivity) =
        requiredPermissions.all {
        it.isGranted(activity)
    }

    private fun shouldShowPermissionRationale(activity: AppCompatActivity) =
        requiredPermissions.any { it.requiresRationale(activity) }

    private fun getPermissionList() =
        requiredPermissions.flatMap { it.permissions.toList() }.toTypedArray()

    private fun Permission.isGranted(activity: AppCompatActivity) =
        permissions.all { hasPermission(activity, it) }

    private fun Permission.requiresRationale(activity: AppCompatActivity) =
        permissions.any { activity.shouldShowRequestPermissionRationale(it) }

    private fun hasPermission(fragment: AppCompatActivity, permission: String) =
        ContextCompat.checkSelfPermission(
            fragment.applicationContext, permission
        ) == PackageManager.PERMISSION_GRANTED

}

sealed class Permission(vararg val permissions: String) {
    object Location : Permission(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION)
    object Bluetooth : Permission(BLUETOOTH, BLUETOOTH_ADMIN, BLUETOOTH_CONNECT, BLUETOOTH_SCAN)

    companion object {
        fun from(permission: String) = when (permission) {
            ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION -> Location
            BLUETOOTH, BLUETOOTH_ADMIN, BLUETOOTH_CONNECT, BLUETOOTH_SCAN -> Bluetooth
            else -> throw IllegalArgumentException("Unknown permission: $permission")
        }
    }

}