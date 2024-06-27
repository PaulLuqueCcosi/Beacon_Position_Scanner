package com.danp.beacon_position_scanner.services

import Point
import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.danp.beacon_position_scanner.services.utilsIBeacon.beaconScanerLibrary.Beacon
import com.danp.beacon_position_scanner.services.utilsIBeacon.beaconScanerLibrary.BeaconParser
import com.danp.beacon_position_scanner.services.utilsIBeacon.beaconScanerLibrary.BleScanCallback
import dagger.hilt.android.AndroidEntryPoint
import trilateration
import javax.inject.Inject

@AndroidEntryPoint
class BeaconScannerService : Service() {

    @Inject
    lateinit var context: Context // Contexto del servicio
    private val TAG = "BeaconScannerService"
    private val binder = MyBinder()

    private var newXPosition: Double? = null
    private var newYPosition: Double? = null
    private var nearestBeacon: Beacon? = null

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var btScanner: BluetoothLeScanner
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val UUDI = "2f234454cf6d4a0fadf2f4911ba9ffa6"

    // HashMap para almacenar los últimos 5 valores de RSSI por beacon
    private val beaconRssiMap = HashMap<String, MutableList<Int>>()

    // Lista para almacenar todos los beacons escaneados
    private val recentBeacons = mutableListOf<Beacon>()

    private var scanCallBack: BleScanCallback? = null;

    // Handler y Runnable para limpiar la lista de beacons recientes
    private val handler = Handler(Looper.getMainLooper())

    private val beaconCleanerRunnable = object : Runnable {
        override fun run() {
            cleanRecentBeaconsList()

            handler.postDelayed(this, 500) // Repetir cada segundo
        }
    }

    private fun cleanRecentBeaconsList() {
        // Limitar el tamaño de la lista de recientes (ejemplo: últimos 2 segundos)
        val currentTime = System.currentTimeMillis()
        recentBeacons.removeIf { currentTime - it.timestamp > 2 * 1000 }
    }

    // Metodo publico del servicio
    fun getPosition(): ResultServiceBeacon {
        handleBeacons(recentBeacons)
        return if (newXPosition != null && newYPosition != null) {
            val x = newXPosition!!
            val y = newYPosition!!
            newXPosition = null
            newYPosition = null
            ResultServiceBeacon.Success(x, y)
        } else {
            ResultServiceBeacon.Error(
                1,
                "No se encontraron suficientes beacons para determinar la posición"
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OnCreate BeaconScannerService")
        Log.d(TAG, "Check Permission")

        context = applicationContext // Asignar el contexto de la aplicación
        initBluetooth()
        scanCallBack = createBleScanCallback();
        startScannerBeacons(scanCallBack!!)
        handler.post(beaconCleanerRunnable) // Iniciar el Runnable

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy BeaconScannerService")
        if (scanCallBack != null) {
            bluetoothScanStop(scanCallBack!!)
        }

        handler.removeCallbacks(beaconCleanerRunnable) // Detener el Runnabl
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand BeaconScannerService")
    }
    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }
    private fun initBluetooth() {
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter != null) {
            btScanner = bluetoothAdapter.bluetoothLeScanner
        } else {
            Log.d(TAG, "BluetoothAdapter is null")
        }
    }

    private fun startScannerBeacons(scanCallBack: BleScanCallback) {
        if (btScanner == null)
            return

        Log.i(TAG, "Start scanning");
        if (!isLocationEnabled() || !isBluetoothEnabled()) {
            Log.d(
                TAG,
                "Servicios no activados - La localizacion y el Bluetooth tienen que estar activos"
            );
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    this,
                    "Servicios no activados - La localización y el Bluetooth tienen que estar activos",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        val scanFilter = ScanFilter.Builder()
            .setManufacturerData(0x004C, byteArrayOf(0x02, 0x15)) // Ejemplo para iBeacon
            .build()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            return
        }
        btScanner.startScan(listOf(scanFilter), scanSettings, scanCallBack)

    }


    private fun isLocationEnabled(): Boolean {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            android.location.LocationManager.NETWORK_PROVIDER
        )
    }

    private fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }

    private fun bluetoothScanStop(bleScanCallback: BleScanCallback) {
        Log.d(TAG, "Stopping Bluetooth scan...")
        if (btScanner != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            btScanner.stopScan(bleScanCallback)
            Log.d(TAG, "Stopp Bluetooth")

        } else {
            Log.d(TAG, "BluetoothLeScanner is null or permission not granted")
        }
    }


    private fun handleBeacons(beacons: Collection<Beacon>) {
        val beaconCerca = getNearestBeacons(beacons.toList(), 3)

        if (beaconCerca.size < 3) {
            Log.d("LOGGER", "No se registran suficientes beacons")
            newXPosition = 1.1
            newYPosition = 1.1
        } else {
            Log.d(TAG, "LISTA: $beaconCerca")

            // Extract points and distances from the beacons
            val P1 = Point(beaconCerca[0].major!!.toDouble(), beaconCerca[0].minor!!.toDouble())
            val P2 = Point(beaconCerca[1].major!!.toDouble(), beaconCerca[1].minor!!.toDouble())
            val P3 = Point(beaconCerca[2].major!!.toDouble(), beaconCerca[2].minor!!.toDouble())

            val R1 = beaconCerca[0].distance!! * 100; // cm.
            val R2 = beaconCerca[1].distance!! * 100;
            val R3 = beaconCerca[2].distance!! * 100;

            Log.d("LOGGER", "Puntos: (${P1}, ${R1}) - (${P2}, ${R2}) - (${P3}, ${R3}) ")
            // Perform trilateration
            Log.d("LOGGER", "Distancias: ${R1}, ${R2}, ${R3}")
            val receptorPosition = trilateration(P1, P2, P3, R1, R2, R3)
            Log.e("LOGGER", "Trilateracion: (${receptorPosition} ")

            if (receptorPosition != null) {
                newXPosition = receptorPosition.x
                newYPosition = receptorPosition.y
            } else {
                Log.d("LOGGER", "Error en la Trilateracion ")

                newXPosition = 800.0
                newYPosition = 800.0
            }
        }
    }


    private fun getNearestBeacons(beacons: List<Beacon>, number: Int = 1): List<Beacon> {
//         List to store beacons with their calculated distances
        Log.d("LOGGER", "Lista dde beacons ${beacons}")
        val beaconDistanceList = mutableListOf<Pair<Beacon, Double>>()

        for (beacon in beacons) {
            val beaconId = "${beacon.major}-${beacon.minor}"

            val distance = beacon.calculateDistanceAverageFilter(
                beacon.txPower!!,
                beacon.rssi!!,
                4.0,
                beaconRssiMap.get(beaconId)!!
            )
            Log.i(
                "LOGGER",
                "Distancia beacon (${beacon.major}, ${beacon.minor}) -> ${distance} -> ${beacon.rssi}"
            )
            if (distance != null) {
                beaconDistanceList.add(beacon to distance)
            }
        }
        Log.d(
            "LOGGER",
            "Lista actualizada : (${beaconDistanceList.size}) ${beaconDistanceList.toString()}"
        )
        // Sort the list by distance and return the closest 'number' beacons
        val newFilterList = beaconDistanceList.sortedBy { it.second }.take(number).map { it.first }
        Log.d("LOGGER", "Lista actualizada filtrada :(${newFilterList.size}) ${newFilterList}")
        newFilterList.map { it ->
            (
                    Log.e("LOGGER", "B :(${it.major}, ${it.minor}) ->  ${it.distance}")
                    )
        }
        return newFilterList
    }

    private fun createBleScanCallback(): BleScanCallback {
        return BleScanCallback(
            onScanResultAction,
            onBatchScanResultAction,
            onScanFailedAction
        )
    }

    private val onScanResultAction: (ScanResult?) -> Unit = { result ->
        Log.d(TAG, "onScanResultAction -> Se detecto un Beacon")

        val scanRecord = result?.scanRecord
        val beacon = Beacon(result?.device?.address).apply {
            manufacturer = result?.device?.name
            rssi = result?.rssi
        }
        val rssiThreshold = -170 // Ejemplo: Ignorar señales con RSSI menor a -100 dBm

        // Verificar si el RSSI es mayor al umbral
        if (beacon.rssi != null && beacon.rssi!! >= rssiThreshold) {
            scanRecord?.bytes?.let {
                val parsedBeacon = BeaconParser.parseIBeacon(it, beacon.rssi)
                // CHECK uuid BEACON

                if (parsedBeacon.uuid == UUDI) {
                    Log.d("LOGGER", "Beacon : ${parsedBeacon} detectado")

                    // Clave para identificar el beacon por su ID mayor y menor
                    val beaconId = "${parsedBeacon.major}-${parsedBeacon.minor}"

                    // Obtener o inicializar la lista de RSSI para este beacon
                    val rssiList = beaconRssiMap.getOrPut(beaconId) { mutableListOf() }

                    // Agregar el nuevo valor de RSSI a la lista
                    rssiList.add(parsedBeacon.rssi ?: 0)

                    // Mantener la lista de RSSI limitada a los últimos 10 valores
                    if (rssiList.size > 10) {
//                    Log.e("ArtRoomViewModel", "Lleno se elimina" + rssiList.get(0) + " ingresa : " + parsedBeacon)
                        rssiList.removeAt(0) // Eliminar el valor más antiguo
                    }

                    // Actualizar la lista de todos los beacons escaneados (histórico)
                    updateScannedBeaconsList(parsedBeacon)
                    Log.d(TAG, "PARA Almacenar: $parsedBeacon")
                }
            }
        } else {
            Log.d(TAG, "Beacon ignorado por señal débil: $beacon")

        }
    }

    private val onBatchScanResultAction: (MutableList<ScanResult>?) -> Unit = {
        Log.d(TAG, "BatchScanResult: ${it.toString()}")
    }

    private val onScanFailedAction: (Int) -> Unit = {
        Log.d(TAG, "ScanFailed: $it")
    }

    private fun updateScannedBeaconsList(beacon: Beacon) {
        // Actualizar el timestamp del beacon
        beacon.timestamp = System.currentTimeMillis()

        // Verificar si el beacon ya está en la lista de recientes
        val existingBeacon = recentBeacons.find {
            it.major == beacon.major && it.minor == beacon.minor
        }

        if (existingBeacon == null) {
            // Si no está en la lista, añadirlo
            recentBeacons.add(beacon)
        } else {
            // Si está en la lista, actualizar sus datos
            existingBeacon.rssi = beacon.rssi
            existingBeacon.timestamp = beacon.timestamp
        }
//
        cleanRecentBeaconsList()

    }

    // Clase interna que extiende Binder y define métodos públicos del servicio
    inner class MyBinder : Binder() {
        fun getService(): BeaconScannerService {
            return this@BeaconScannerService
        }
    }
}