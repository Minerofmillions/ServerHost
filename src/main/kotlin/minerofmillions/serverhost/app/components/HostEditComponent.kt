package minerofmillions.serverhost.app.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import io.github.minerofmillions.decompose.mutableValueListOf
import minerofmillions.serverhost.HostConfig
import minerofmillions.serverhost.ServerConfig

class HostEditComponent(
    private val onCancel: () -> Unit,
    private val onSave: (HostConfig) -> Unit,
    private val onSetDarkMode: (Boolean) -> Unit,
    val darkMode: Value<Boolean>,
    context: ComponentContext,
) :
    ComponentContext by context {
    val startPortValue: MutableValue<Int>
    val serversValue = mutableValueListOf<ServerConfig>()

    init {
        val config = HostConfig.readConfig()
        startPortValue = MutableValue(config.startPort)
        serversValue.addAll(config.servers)
    }

    fun cancel() = onCancel()
    fun save() = onSave(HostConfig(serversValue.value, startPortValue.value))

    fun setInitialPort(value: String) {
        value.toIntOrNull()?.let { startPortValue.value = it }
    }

    fun setServerName(server: ServerConfig, serverName: String) {
        val serverIndex = serversValue.indexOf(server)
        serversValue[serverIndex] = serversValue[serverIndex].copy(serverName = serverName)
    }

    fun setServerStart(server: ServerConfig, startCommand: String) {
        val serverIndex = serversValue.indexOf(server)
        serversValue[serverIndex] = serversValue[serverIndex].copy(startCommand = startCommand)
    }

    fun setServerBase(server: ServerConfig, baseDirectory: String) {
        val serverIndex = serversValue.indexOf(server)
        serversValue[serverIndex] = serversValue[serverIndex].copy(baseDirectory = baseDirectory)
    }

    fun removeServer(server: ServerConfig) {
        serversValue.remove(server)
    }

    fun setDarkMode(value: Boolean) = onSetDarkMode(value)

    fun addServer() {
        val nextServerNumber = serversValue.size + 1
        serversValue += ServerConfig("", "", "Server $nextServerNumber")
    }
}