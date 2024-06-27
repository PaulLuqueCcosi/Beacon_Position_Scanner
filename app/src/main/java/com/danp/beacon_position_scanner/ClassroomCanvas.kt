package com.danp.beacon_position_scanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class ClassroomCanvas {
}

object ProjSizes {
    val heightCM = 810.dp
    val widthCM = 765.dp
    const val proportion = 2.22f
    val heightL: Dp = heightCM / proportion
    val widthL: Dp = widthCM / proportion
}

@Composable
fun DpToPx(dp: Dp): Float {
    val density = LocalDensity.current
    return remember(dp, density) {
        with(density) { dp.toPx() }
    }
}


@Preview(showBackground = true)
@Composable
fun Final() {
    val posX: Dp = 106.dp // 0..765 Dp
    val posY: Dp = 620.dp // 0...810 Dp/

    val posXProp = DpToPx(dp = posX / ProjSizes.proportion)
    val posYProp = DpToPx(dp = posY / ProjSizes.proportion)

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 30.dp, end = 20.dp, start = 20.dp)
            ) {
                RoomCanvas()
                RoomCanvas2()
                TargetPos(offset = Offset(posXProp, posYProp))
            }
        }

    }
}
@Preview(showBackground = true)
@Composable
fun RoomCanvas2(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val width = ProjSizes.widthL.toPx()
        val height = ProjSizes.heightL.toPx()

        val tilesHorizontal = 17
        val tilesVertical = 18

        val tileWidth = width / tilesHorizontal
        val tileHeight = height / tilesVertical

        // Dibujar el rectángulo de la habitación
        drawRect(
            color = Color.LightGray,
            size = Size(width, height)
        )

        // Dibujar las líneas de la cuadrícula horizontal
        for (i in 1 until tilesVertical) {
            val y = i * tileHeight
            drawLine(
                color = Color.Gray,
                start = Offset(0f, y),
                end = Offset(width, y)
            )
        }

        // Dibujar las líneas de la cuadrícula vertical
        for (i in 1 until tilesHorizontal) {
            val x = i * tileWidth
            drawLine(
                color = Color.Gray,
                start = Offset(x, 0f),
                end = Offset(x, height)
            )
        }

        // Dibujar el rectángulo negro entre las lozetas 8, 9, 10 de la primera columna
        val startY = 7 * tileHeight
        val endY = 10 * tileHeight
        drawRect(
            color = Color(0xFF800000),
            topLeft = Offset(0f, startY),
            size = Size(tileWidth, endY - startY)
        )

        // Pintar de negro las lozetas 5 a 14 de la primera fila
        for (i in 4 until 14) {  // Ajuste de índices ya que queremos pintar de la lozeta 5 a 14 (índice 4 a 13)
            val startX = i * tileWidth
            drawRect(
                color = Color(0xFF87CEFA),
                topLeft = Offset(startX, 0f),
                size = Size(tileWidth, tileHeight)
            )
        }
    }
}
@Composable
fun RoomCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(
            color = Color.LightGray,
            size = Size(ProjSizes.widthL.toPx(), ProjSizes.heightL.toPx())
        )
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

@Composable
fun PositionFields(modifier: Modifier = Modifier) {


}