package com.danp.beacon_position_scanner.services

sealed class ResultServiceBeacon {
    data class Success(val x: Double, val y: Double, val mensaje: String?) : ResultServiceBeacon()
    data class Error(val errorCode: Int, val errorMessage: String) : ResultServiceBeacon()
}
