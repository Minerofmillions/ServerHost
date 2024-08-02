package minerofmillions.serverhost.app.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import minerofmillions.serverhost.Server
import minerofmillions.serverhost.app.runOnUiThread
import minerofmillions.serverhost.coroutineScope
import minerofmillions.utils.any
import minerofmillions.utils.filter
import minerofmillions.utils.forEachParallel

class ServerHostComponent(
    val servers: List<Server>,
    private val onConfigure: () -> Unit,
    private val onSetDarkMode: (Boolean) -> Unit,
    val darkMode: Value<Boolean>,
    componentContext: ComponentContext,
) : ComponentContext by componentContext {
    private val coroutineContext = coroutineScope(Dispatchers.IO)
    val anyServerActive = servers.map(Server::logShowing).any()

    private val _erroredServers = servers.filter { it.isErrored }
    val erroredServers: Value<List<Server>> = _erroredServers

    private val _validServers = servers.filter { it.isErrored.map(Boolean::not) }
    val validServers: Value<List<Server>> = _validServers

    suspend fun closeServers() {
        servers.forEach { it.setServerActive(false) }
        servers.forEachParallel { it.awaitServerStopped() }
        servers.forEach { it.stopListener() }
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