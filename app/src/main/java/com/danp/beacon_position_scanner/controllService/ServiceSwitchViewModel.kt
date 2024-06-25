package com.danp.artexploreapp.artRoom.presentation.viewModels
import androidx.lifecycle.SavedStateHandle


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danp.artexploreapp.services.BeaconScannerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class ServiceSwitchViewModel : ViewModel() {
    private val TAG: String = "ArtRoomViewModel"
    private var beaconService: BeaconScannerService? = null
    private var isBound: Boolean = false

    //    var switchState by mutableStateOf(false)
//        private set
    private val switchState = MutableLiveData(false)
    fun getSwitchState(): LiveData<Boolean> = switchState

    var showDialog2 by mutableStateOf(false)
        private set
    var showDialog1 by mutableStateOf(false)
        private set
    var circlePosition by mutableStateOf(Offset(300f, 300f))
        private set

    lateinit var context : Context

    fun setContex(contex: Context){
        this.context = contex
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as BeaconScannerService.MyBinder
            beaconService = binder.getService()
            isBound = true
            startPollingService()

        }

        override fun onServiceDisconnected(className: ComponentName?) {
            beaconService = null
            isBound = false
        }
    }

    fun onChangeShowDialog(newShowDialog: Boolean) {
        showDialog1 = newShowDialog
        showDialog2 = newShowDialog
    }

    fun getRandomPosition(screenWidth: Float, screenHeight: Float, circleRadius: Float): Offset {
        val newX = (Random.nextFloat() * (screenWidth - 2 * circleRadius)) + circleRadius
        val newY = (Random.nextFloat() * (screenHeight - 2 * circleRadius)) + circleRadius
        return Offset(newX, newY)
    }


    fun onClickIconBox1() {
        showDialog1 = true
    }

    fun onClickIconBox2() {
        showDialog2 = true
    }

    fun setNewPositionCircle(newPosition: Offset) {
        circlePosition = newPosition
    }

    fun onChangeSwitchState(newSwitch: Boolean){
        Log.d(TAG, "Change State $switchState -> $newSwitch")

        switchState.setValue(newSwitch)
//        switchState = newSwitch

        if (getSwitchState().value!!) {
            startServiceScanBeacon()
        } else {
            stopServiceScanBeacon()
        }
    }

    fun setStopScan(){
        Log.d(TAG, "Change stopping scanner")
        stopServiceScanBeacon()
    }

    private fun stopServiceScanBeacon() {
        Log.d(TAG, "Stopping Service Beacon Scanner - in ArtRoomViewModel")

//        // stopr service
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
        val intent = Intent(context, BeaconScannerService::class.java)
        context.stopService(intent)
    }

    private fun startServiceScanBeacon() {
        Log.d(TAG, "Starting Service Beacon Scanner - in ArtRoomViewModel")
        // Vincular al servicio
        if(isBound){
            Log.d(TAG, "Service running, no create other")
            return
        }
        val intent = Intent(context!!, BeaconScannerService::class.java)
        context!!.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        context!!.startService(intent)
    }

    private fun startPollingService() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isBound) {
                beaconService?.let { service ->
                    val currentGallery = service.getNewXPosition()
                    val nearestPainting = service.getNewYPosition()
                    Log.d(TAG, "New X: $currentGallery")
                    Log.d(TAG, "New Y: $nearestPainting")
                }
                delay(2000) // Espera 5 segundos antes de la próxima consulta
            }
        }
    }



}
