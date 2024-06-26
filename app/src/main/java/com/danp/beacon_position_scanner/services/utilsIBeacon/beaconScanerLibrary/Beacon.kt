package com.danp.beacon_position_scanner.services.utilsIBeacon.beaconScanerLibrary

import android.util.Log

class Beacon(
    val macAddress: String?,
    var manufacturer: String? = null,
    var type: BeaconType = BeaconType.ANY,
    var uuid: String? = null,
    var major: Int? = null,
    var minor: Int? = null,
    var namespace: String? = null,
    var instance: String? = null,
    var rssi: Int? = null,
    var txPower: Int? = null,
    var timestamp: Long = System.currentTimeMillis(), // Añadir el timestamp
    var distance: Double? = 0.0
) {
    val TAG = "Beacon"
    enum class BeaconType {
        IBEACON, EDDYSTONE_UID, ANY
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Beacon) return false
        if (uuid != other.uuid) return false
        return true
    }

//    private val movingAverageFilter = MovingAverageFilter(5)

    override fun hashCode(): Int {
        return uuid?.hashCode() ?: 0
    }
    override fun toString(): String {
        return "Beacon(macAddress=$macAddress, manufacturer=$manufacturer, type=$type, uuid=$uuid, major=$major, minor=$minor, rssi=$rssi, TxPower=$txPower)"
    }

    fun calculateDistance(txPower: Int, rssi: Int, N: Double): Double {
        val factor = (txPower - rssi) / (10 * N)
        val distance = Math.pow(10.0, factor)
        return distance;

    }
    fun calculateDistance(N: Double = 3.0): Double {
        val factor = (this.txPower!! - this.rssi!!) / (10 * N)
        val distance = Math.pow(10.0, factor)
        return distance;

    }


    fun calculateDistanceAverageFilter(txPower: Int, rssi: Int, N: Double, rssiHistory: MutableList<Int>): Double {
        // Calcular el promedio de los valores de RSSI en el historial y el nuevo valor de RSSI
        val allRssiValues = rssiHistory + rssi
        val averageRssi = allRssiValues.average()

        // Calcular el factor basado en txPower y el promedio de RSSI
        val factor = (txPower - averageRssi) / (10 * N)

        // Calcular la distancia usando la fórmula de atenuación de la señal
        val distance = Math.pow(10.0, factor)

        // Imprimir en el log el promedio de RSSI y la distancia calculada
        Log.d(TAG, "Using moving filter: averageRssi=$averageRssi, distance=$distance")

        this.distance = distance
        return distance
    }

}
