package minerofmillions.serverhost.app.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
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
    val serversValue: MutableValue<List<ServerConfig>>

    init {
        val config = HostConfig.readConfig()
        startPortValue = MutableValue(config.startPort)
        serversValue = MutableValue(config.servers)
    }

    fun cancel() = onCancel()
    fun save() = onSave(HostConfig(serversValue.value, startPortValue.value))

    fun setInitialPort(value: String) {
        value.toIntOrNull()?.let { startPortValue.value = it }
    }

    fun setServerName(server: ServerConfig, serverName: String) {
        serversValue.update { servers ->
            servers.map { if (it == server) it.copy(serverName = serverName) else it }
        }
    }

    fun setServerStart(server: ServerConfig, startCommand: String) {
        serversValue.update { servers ->
            servers.map { if (it == server) it.copy(startCommand = startCommand) else it }
        }
    }

    fun setServerBase(server: ServerConfig, baseDirectory: String) {
        serversValue.update { servers ->
            servers.map { if (it == server) it.copy(baseDirectory = baseDirectory) else it }
        }
    }

    fun setDarkMode(value: Boolean) = onSetDarkMode(value)

    fun addServer() {
        val nextServerNumber = serversValue.value.size + 1
        serversValue.value += ServerConfig("", "", "Server $nextServerNumber")
    }
}