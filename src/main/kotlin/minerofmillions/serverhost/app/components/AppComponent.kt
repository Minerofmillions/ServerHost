package minerofmillions.serverhost.app.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.essenty.parcelable.Parcelable
import kotlinx.serialization.Serializable
import minerofmillions.serverhost.HostConfig

class AppComponent(componentContext: ComponentContext) : ComponentContext by componentContext {
    private val navigation = SlotNavigation<Config>()
    val slot = childSlot(navigation, initialConfiguration = { Config.HOST }) { config, childContext ->
        when (config) {
            is Config.HOST -> ServerHostComponent(HostConfig.getServers(childContext), {
                navigation.activate(Config.EDIT)
            }, childContext)

            Config.EDIT -> HostEditComponent({
                navigation.activate(Config.HOST)
            }, {
               HostConfig.writeConfig(it)
                navigation.activate(Config.HOST)
            }, childContext)
        }
    }

    fun closeServers() {
        val slotValue = slot.value.child?.instance ?: return
        if (slotValue is ServerHostComponent) slotValue.closeServers()
    }

    @Serializable
    sealed interface Config : Parcelable {
        data object HOST : Config {
            private fun readResolve(): Any = HOST
        }

        data object EDIT : Config {
            private fun readResolve(): Any = EDIT
        }
    }
}