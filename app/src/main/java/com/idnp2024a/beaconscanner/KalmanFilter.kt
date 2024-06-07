package com.idnp2024a.beaconscanner

import android.util.Log

class KalmanFilter(
    private val processNoise: Double,
    private val measurementNoise: Double,
    private val estimateError: Double
) {
    private var estimate: Double = 0.0
    private var error: Double = estimateError

    fun filter(value: Double): Double {
        // Predict
        error += processNoise

        // Update
        val kalmanGain = error / (error + measurementNoise)
        estimate += kalmanGain * (value - estimate)
        error *= (1 - kalmanGain)

        Log.d("KalmanFilter", "Value: $value, Estimate: $estimate, Error: $error") // LOG DE VALORES Y ESTIMACIONES

        return estimate
    }
}
