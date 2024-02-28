package minerofmillions.serverhost.app.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import minerofmillions.serverhost.Server
import minerofmillions.serverhost.app.runOnUiThread
import minerofmillions.serverhost.coroutineScope
import minerofmillions.utils.any

class ServerHostComponent(
    val servers: List<Server>,
    private val onConfigure: () -> Unit,
    private val onSetDarkMode: (Boolean) -> Unit,
    val darkMode: Value<Boolean>,
    componentContext: ComponentContext,
) : ComponentContext by componentContext {
    private val coroutineContext = coroutineScope(Dispatchers.IO)
    val anyServerActive = servers.map(Server::logShowing).any()

    fun closeServers() {
        servers.forEach { it.setServerActive(false) }
        servers.forEach { it.awaitServerStopped() }
    }

    fun configure() {
        coroutineContext.launch {
            closeServers()
        }.invokeOnCompletion {
            runOnUiThread(onConfigure)
        }
    }

    fun setDarkMode(value: Boolean) = onSetDarkMode(value)
}