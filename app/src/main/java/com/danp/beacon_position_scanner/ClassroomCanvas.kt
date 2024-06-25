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
    val posX: Dp = 86.dp // 0..765 Dp
    val posY: Dp = 220.dp // 0...810 Dp/

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
                TargetPos(offset = Offset(posXProp, posYProp))
            }
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