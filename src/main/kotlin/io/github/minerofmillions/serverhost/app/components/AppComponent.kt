package io.github.minerofmillions.serverhost.app.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.value.MutableValue
import io.github.minerofmillions.serverhost.HostConfig
import io.github.minerofmillions.serverhost.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class AppComponent(componentContext: ComponentContext) : ComponentContext by componentContext {
    private val navigation = SlotNavigation<Config>()
    private val coroutineScope = coroutineScope(Dispatchers.IO)

    val darkMode = MutableValue(true)
    val slot = childSlot(navigation, null, initialConfiguration = { Config.HOST }) { config, childContext ->
        when (config) {
            is Config.HOST -> ServerHostComponent(HostConfig.readConfig(), {
                navigation.activate(Config.EDIT)
            }, { darkMode.value = it }, darkMode, childContext)

            Config.EDIT -> HostEditComponent({
                navigation.activate(Config.HOST)
            }, {
                HostConfig.writeConfig(it)
                navigation.activate(Config.HOST)
            }, { darkMode.value = it }, darkMode, childContext)
        }
    }

    fun closeServers() {
        val slotValue = slot.value.child?.instance ?: return
        if (slotValue is ServerHostComponent) coroutineScope.launch { slotValue.closeServers() }
    }

    @Serializable
    sealed interface Config {
        data object HOST : Config {
            private fun readResolve(): Any = HOST
        }

        data object EDIT : Config {
            private fun readResolve(): Any = EDIT
        }
    }
}