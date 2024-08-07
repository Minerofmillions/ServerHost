package io.github.minerofmillions.serverhost.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.minerofmillions.serverhost.app.components.AppComponent
import io.github.minerofmillions.serverhost.app.components.HostEditComponent
import io.github.minerofmillions.serverhost.app.components.ServerHostComponent

@Composable
fun App(component: AppComponent) {
    val child by component.slot.subscribeAsState()
    child.child?.instance?.also {
        when (it) {
            is ServerHostComponent -> ServerHost(it)
            is HostEditComponent -> HostEdit(it)
        }
    }
}