package com.idnp2024a.beaconscanner

import android.util.Log

class MovingAverageFilter(private val windowSize: Int) {

    private val values = mutableListOf<Int>()

    fun filter(value: Int): Double {
        values.add(value)
        if (values.size > windowSize) {
            values.removeAt(0)
        }
        val sum = values.sum()
        Log.d("MovingAverageFilter", "Values: $values, Sum: $sum, Average: ${sum.toDouble() / values.size}") // LOG DE VALORES Y PROMEDIO

        return sum.toDouble() / values.size
    }
}
