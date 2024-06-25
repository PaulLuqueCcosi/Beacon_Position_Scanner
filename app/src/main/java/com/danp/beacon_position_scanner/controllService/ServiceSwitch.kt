package com.danp.beacon_position_scanner.controllService

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun EjemploService(viewModel: ServiceSwitchViewModel){
    val switchState = viewModel.getSwitchState().observeAsState(false)
    viewModel.setContex(LocalContext.current)


    Switch(
        checked = switchState.value,
        onCheckedChange = { isChecked ->
            viewModel.onChangeSwitchState(isChecked)
        },
        modifier = Modifier.padding(16.dp)
    )
}