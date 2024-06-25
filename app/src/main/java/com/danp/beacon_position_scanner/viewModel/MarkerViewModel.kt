import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class MarkerViewModel : ViewModel() {

    // Estado mutable para las posiciones del marcador
    var posX by mutableStateOf(400.dp)
        private set

    var posY by mutableStateOf(350.dp)
        private set

    // Función para actualizar las posiciones del marcador
    fun updatePosition(newPosX: Dp, newPosY: Dp) {
        posX = newPosX
        posY = newPosY
    }
}
