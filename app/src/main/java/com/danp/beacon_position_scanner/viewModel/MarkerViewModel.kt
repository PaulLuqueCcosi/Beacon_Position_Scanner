import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.danp.artexploreapp.services.BeaconScannerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MarkerViewModel : ViewModel() {

    // Estado mutable para las posiciones del marcador
    var posX by mutableStateOf(400.dp)
        private set

    var posY by mutableStateOf(350.dp)
        private set

    // Función para actualizar las posiciones del marcador
    fun updatePosition(newPosX: Dp?, newPosY: Dp?) {
        if(newPosX == null || newPosY == null){
            return
        }
        posX = newPosX
        posY = newPosY
    }


    ////////////////////


    private val TAG: String = "ArtRoomViewModel"
    private var beaconService: BeaconScannerService? = null
    private var isBound: Boolean = false

    //    var switchState by mutableStateOf(false)
//        private set
    private val switchState = MutableLiveData(false)
    fun getSwitchState(): LiveData<Boolean> = switchState


    private lateinit var context: Context

    fun setContex(contex: Context) {
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


    fun onChangeSwitchState(newSwitch: Boolean) {
        Log.d(TAG, "Change State $switchState -> $newSwitch")

        switchState.setValue(newSwitch)
//        switchState = newSwitch

        if (getSwitchState().value!!) {
            startServiceScanBeacon()
        } else {
            stopServiceScanBeacon()
        }
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
        if (isBound) {
            Log.d(TAG, "Service running, no create other")
            return
        }
        val intent = Intent(context, BeaconScannerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        context.startService(intent)
    }
    fun Double.toDp(): Dp {
        val intValue = this.toInt()
        return intValue.dp
    }
    private fun startPollingService() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isBound) {
                beaconService?.let { service ->
                    val positionXFromService = service.getNewXPosition()
                    val positionYFromService = service.getNewYPosition()
                    val newPosX = positionXFromService?.toDp()
                    val newPosY = positionYFromService?.toDp()

                    updatePosition(newPosX, newPosY)
                    Log.d(TAG, "New X: $positionXFromService")
                    Log.d(TAG, "New Y: $positionYFromService")
                }
                delay(1000) // Espera 5 segundos antes de la próxima consulta
            }
        }
    }


}
