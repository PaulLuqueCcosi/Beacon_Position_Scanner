package com.danp.beacon_position_scanner


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.danp.beacon_position_scanner.viewModel.MarkerViewModel
import kotlin.random.Random

object ProjSizes {
    val heightCM = 810.dp
    val widthCM = 765.dp
    const val proportion = 2.22f
    val heightL: Dp = heightCM / proportion
    val widthL: Dp = widthCM / proportion
}
//convierte una medida en Dp a pixeles utilizando la densidad actual del dispositivo
@Composable
fun DpToPx(dp: Dp): Float {
    val density = LocalDensity.current
    return remember(dp, density) {
        with(density) { dp.toPx() }
    }
}

@Composable
fun Final(viewModel: MarkerViewModel = MarkerViewModel()) {
    val switchState = viewModel.getSwitchState().observeAsState(false)
    viewModel.setContex(LocalContext.current)
    val statusMessage = viewModel.statusMessage.observeAsState("No datos de inicio")


    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {


            Switch(
                checked = switchState.value,
                onCheckedChange = { isChecked ->
                    viewModel.onChangeSwitchState(isChecked)
                },
                modifier = Modifier.padding(16.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(top = 30.dp, end = 20.dp, start = 20.dp)
            ) {
                RoomCanvas()
                // Obtenemos las posiciones del ViewModel
                TargetPos(offset = Offset(DpToPx(viewModel.posX / ProjSizes.proportion), DpToPx(viewModel.posY / ProjSizes.proportion)))
            }
            // Spacer para alinear el botón en la parte inferior
            Text(
                text = statusMessage.value ?: "No status",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            Button(
                onClick = {
                    // Generamos nuevas posiciones aleatorias
                    val newPosX = (0..ProjSizes.widthCM.value.toInt() - 1).random().dp
                    val newPosY = (0..ProjSizes.heightCM.value.toInt() - 1).random().dp
                    // Actualizamos las posiciones del marcador a través del ViewModel
                    viewModel.updatePosition(newPosX, newPosY)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Mover Marcador")
            }
        }
    }
}
@Composable
fun RoomCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val width = ProjSizes.widthL.toPx()
        val height = ProjSizes.heightL.toPx()

        val tilesHorizontal = 17
        val tilesVertical = 18

        val tileWidth = width / tilesHorizontal
        val tileHeight = height / tilesVertical
        drawRect(
            color = Color.LightGray,
            size = Size(ProjSizes.widthL.toPx(), ProjSizes.heightL.toPx())
        )
        // Dibujamos las líneas de la cuadrícula horizontal
        for (i in 1 until tilesVertical) {
            val y = i * tileHeight
            drawLine(
                color = Color.Gray,
                start = Offset(0f, y),
                end = Offset(width, y)
            )
        }

        // Dibujamos las líneas de la cuadrícula vertical
        for (i in 1 until tilesHorizontal) {
            val x = i * tileWidth
            drawLine(
                color = Color.Gray,
                start = Offset(x, 0f),
                end = Offset(x, height)
            )
        }

        // Dibujamos el rectángulo negro entre las lozetas 8, 9, 10 de la primera columna
        val startY = 5 * tileHeight
        val endY = 8 * tileHeight
        drawRect(
            color = Color(0xFF800000),
            topLeft = Offset(0f, startY),
            size = Size(tileWidth, endY - startY)
        )

        // Pintamos de negro las lozetas 5 a 14 de la primera fila
        for (i in 4 until 14) {
            val startX = i * tileWidth
            drawRect(
                color = Color(0xFF87CEFA),
                topLeft = Offset(startX, 0f),
                size = Size(tileWidth, tileHeight)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoomPreview() {
    RoomCanvas()
}

@Composable
fun TargetPos(offset: Offset, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawCircle(Color.Blue, radius = 35f, center = offset)
    }
}

