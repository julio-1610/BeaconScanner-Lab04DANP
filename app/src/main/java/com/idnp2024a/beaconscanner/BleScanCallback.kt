package com.idnp2024a.beaconscanner

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult

class BleScanCallback(
    // funciones lambda
    // CONSTRUCTOR
    //private val onScanResultAction: (ScanResult?) -> Unit = {},//se recibe un resultado de escaneo individual
    private val onScanResultAction: (ScanResult?, Double) -> Unit = { _, _ -> },
    private val onBatchScanResultAction: (MutableList<ScanResult>?) -> Unit = {},//cuando se reciben resultados de escaneo en lote
    private val onScanFailedAction: (Int) -> Unit = {} //se ejecuta cuando el escaneo falla
) : ScanCallback() {//extiende
    private val movingAverageFilter = MovingAverageFilter(windowSize = 5)

    // Este método se llama cuando se encuentra un resultado de escaneo individual.
    override fun onScanResult(callbackType: Int, result: ScanResult?) {
        super.onScanResult(callbackType, result)
        result?.rssi?.let {
            val smoothedRSSI = movingAverageFilter.filter(it)
            onScanResultAction(result, smoothedRSSI)
        }
    }
    // Este método se llama cuando se reciben resultados de escaneo en lote.
    override fun onBatchScanResults(results: MutableList<ScanResult>?) {
        super.onBatchScanResults(results)
        onBatchScanResultAction(results)
    }

    override fun onScanFailed(errorCode: Int) {
        super.onScanFailed(errorCode)
        onScanFailedAction(errorCode)
    }

}