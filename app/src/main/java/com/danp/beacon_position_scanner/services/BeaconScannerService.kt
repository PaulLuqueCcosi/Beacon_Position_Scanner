package com.danp.artexploreapp.services

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
import androidx.core.app.ActivityCompat
import com.idnp2024a.beaconscanner.BeaconScanerLibrary.Beacon
import com.idnp2024a.beaconscanner.BeaconScanerLibrary.BeaconParser
import com.idnp2024a.beaconscanner.BeaconScanerLibrary.BleScanCallback
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BeaconScannerService : Service() {

    @Inject
    lateinit var context: Context // Contexto del servicio
    private val TAG = "BeaconScannerService"
    private val binder = MyBinder()

    private var newXPosition: Double? = 0.0
    private var newYPosition: Double? = 0.0
    private var nearestBeacon : Beacon? = null

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var btScanner: BluetoothLeScanner
    private lateinit var bluetoothAdapter: BluetoothAdapter
//    private lateinit var context: Context // Contexto del servicio

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
        // Limitar el tamaño de la lista de recientes (ejemplo: últimos 3 segundos)
        val currentTime = System.currentTimeMillis()
        recentBeacons.removeIf { currentTime - it.timestamp > 2 * 1000 }    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OnCreate BeaconScannerService")

        Log.d(TAG, "Check Permission")
//        val permissionManager = PermissionManager.from(context)


        context = applicationContext // Asignar el contexto de la aplicación
        initBluetooth()
        scanCallBack  = createBleScanCallback();

        startScannerBeacons(scanCallBack!!)
        handler.post(beaconCleanerRunnable) // Iniciar el Runnable

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

    private fun startScannerBeacons(scanCallBack:BleScanCallback ){
        if(btScanner == null)
            return

//        val scanCallBack  = createBleScanCallback();
        Log.i(TAG, "Start scanning");
        if (!isLocationEnabled() || !isBluetoothEnabled()) {
            Log.d(TAG, "Servicios no activados - La localizacion y el Bluetooth tienen que estar activos");
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
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        btScanner.startScan(listOf(scanFilter), scanSettings,scanCallBack)

    }


    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            android.location.LocationManager.NETWORK_PROVIDER
        )
    }

    private fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy BeaconScannerService")
        if(scanCallBack !=null) {
            bluetoothScanStop(scanCallBack!!)
        }

        handler.removeCallbacks(beaconCleanerRunnable) // Detener el Runnable


    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand BeaconScannerService")
    }
    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private fun bluetoothScanStop(bleScanCallback: BleScanCallback) {
        Log.d(TAG, "Stopping Bluetooth scan...")
        if (btScanner != null) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            btScanner.stopScan(bleScanCallback)
        } else {
            Log.d(TAG, "BluetoothLeScanner is null")
        }
    }




    // Metodo publicos
    fun getNewXPosition(): Double? {
        handleBeacons(recentBeacons)
        val newX = newXPosition
        newXPosition = null  // Limpiar la variable currentGallery
        newYPosition = null  // Limpiar la variable nearestPainting
        return newX
    }

    // Metodos publicos
    fun getNewYPosition(): Double? {
//        hanleBeacons(recentBeacons)
        handleBeacons(recentBeacons)
        val newY = newYPosition
        newXPosition = null  // Limpiar la variable currentGallery
        newYPosition = null  // Limpiar la variable nearestPainting
        return newY
    }


    private fun getPaintingFromMinor(major: Int, minor: Int): String {
        // Implementa la lógica para mapear Minor a pinturas específicas dentro de una galería
        return "Pintura $minor en Galería $major , distance ${nearestBeacon?.distance}"
    }

    private fun handleBeacons(beacons: Collection<Beacon>) {
        val beaconCerca = getNearestBeacons(beacons.toList(), 3)

        if (beaconCerca.size != 3) {
            newXPosition = 1.1
            newYPosition = 1.1
        } else {
            Log.d(TAG, "LISTA: $beaconCerca")
            newXPosition = 2.0
            newYPosition = 2.0
        }


    }

    private fun getMostFrequentMajor(beacons: List<Beacon>): Int? {
        // Contar las apariciones de cada major en los beacons recientes
        val majorCountMap = beacons.groupingBy { it.major }.eachCount()

        // Encontrar el major con mayor frecuencia
        return majorCountMap.maxByOrNull { it.value }?.key
    }

//    private fun getNearestBeaconWithMajor(beacons: List<Beacon>, major: Int): Beacon? {
//        // Filtrar los beacons con el major especificado
//        val filteredBeacons = beacons.filter { it.major == major }
//
//        // Encontrar el beacon más cercano con el major especificado
//        var closestBeacon: Beacon? = null
//        var closestDistance = Double.MAX_VALUE
//
//        for (beacon in filteredBeacons) {
//            val beaconId = "${beacon.major}-${beacon.minor}"
//
//            val distance = beacon.calculateDistanceAverageFilter(beacon.txPower!!, beacon.rssi!!, 3.0, beaconRssiMap.get(beaconId)!!)
//            if (distance != null) {
//                if (distance < closestDistance) {
//                    closestDistance = distance
//                    closestBeacon = beacon
//                }
//            }
//        }
//
//        return closestBeacon
//    }

    private fun getNearestBeacons(beacons: List<Beacon>, number: Int = 3): List<Beacon> {
        // List to store beacons with their calculated distances
        val beaconDistanceList = mutableListOf<Pair<Beacon, Double>>()

        for (beacon in beacons) {
            val beaconId = "${beacon.major}-${beacon.minor}"

            val distance = beacon.calculateDistanceAverageFilter(beacon.txPower!!, beacon.rssi!!, 3.0, beaconRssiMap.get(beaconId)!!)
            if (distance != null) {
                beaconDistanceList.add(beacon to distance)
            }
        }

        // Sort the list by distance and return the closest 'number' beacons
        return beaconDistanceList.sortedBy { it.second }.take(number).map { it.first }
    }

    private fun createBleScanCallback(): BleScanCallback {
        return BleScanCallback(
            onScanResultAction,
            onBatchScanResultAction,
            onScanFailedAction
        )
    }

    private val onScanResultAction: (ScanResult?) -> Unit = { result ->
        Log.d(TAG, "onScanResultAction")
//        Log.d(TAG, result.toString())

        val scanRecord = result?.scanRecord
        val beacon = Beacon(result?.device?.address).apply {
            manufacturer = result?.device?.name
            rssi = result?.rssi
        }
        Log.d(TAG, "Scan: $beacon")
        val rssiThreshold = -100 // Ejemplo: Ignorar señales con RSSI menor a -100 dBm

        // Verificar si el RSSI es mayor al umbral
        if (beacon.rssi != null && beacon.rssi!! >= rssiThreshold) {
            scanRecord?.bytes?.let {
                val parsedBeacon = BeaconParser.parseIBeacon(it, beacon.rssi)

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
        }else{
            Log.d(TAG, "Beacon ignorado por señal débil: $beacon")

        }
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


    private val onBatchScanResultAction: (MutableList<ScanResult>?) -> Unit = {
        Log.d(TAG, "BatchScanResult: ${it.toString()}")
    }

    private val onScanFailedAction: (Int) -> Unit = {
        Log.d(TAG, "ScanFailed: $it")
    }

    // Clase interna que extiende Binder y define métodos públicos del servicio
    inner class MyBinder : Binder() {
        fun getService(): BeaconScannerService {
            return this@BeaconScannerService
        }
    }
}