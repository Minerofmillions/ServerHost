package minerofmillions.serverhost.app.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import minerofmillions.serverhost.HostConfig
import minerofmillions.serverhost.*
import minerofmillions.serverhost.app.runOnUiThread
import minerofmillions.utils.any
import minerofmillions.utils.filter
import minerofmillions.utils.forEachParallel
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

class ServerHostComponent(
    private val config: HostConfig,
    private val onConfigure: () -> Unit,
    private val onSetDarkMode: (Boolean) -> Unit,
    val darkMode: Value<Boolean>,
    componentContext: ComponentContext,
) : ComponentContext by componentContext {
    private val servers =
        config.servers.mapIndexed { index, it -> Server.fromConfig(it, config.startPort + index, componentContext) }

    private val coroutineContext = coroutineScope(Dispatchers.IO)
    val anyServerActive = servers.map(Server::logShowing).any()

    private val _erroredServers = servers.filter { it.isErrored }
    val erroredServers: Value<List<Server>> = _erroredServers

    private val _validServers = servers.filter { it.isErrored.map(Boolean::not) }
    val validServers: Value<List<Server>> = _validServers

    init {
        coroutineContext.launch {
            remoteAccess()
        }
    }

    private suspend fun remoteAccess() = coroutineScope {
        val serverSocket = ServerSocket(8080)
        serverSocket.soTimeout = 250
        while (isActive) {
            val socket = try {
                serverSocket.accept()
            } catch (_: SocketTimeoutException) {
                null
            } ?: continue
            launch {
                remoteAccessSocket(socket)
            }
        }
        serverSocket.close()
    }

    private suspend fun remoteAccessSocket(socket: Socket) = coroutineScope {
        val inputStream = socket.getInputStream()
        val outputStream = socket.getOutputStream()

        while (isActive) {
            val requestPacket = inputStream.readUncompressedPacket()
            when (requestPacket.packetId) {
                0 -> {
                    outputStream.writePacket(queryGeneralInformation())
                    servers.indices.forEach {
                        outputStream.writePacket(queryServer(it))
                    }
                }

                1 -> outputStream.writePacket(queryGeneralInformation())
                2 -> {
                    val (id) = inputStream.readVarInt()
                    outputStream.writePacket(queryServer(id))
                }

            }
        }

        socket.close()
    }

    private fun queryGeneralInformation(): Packet =
        Packet(0, encodeVarInt(config.startPort) + encodeVarInt(servers.size))

    private fun queryServer(id: Int): Packet {
        if (id !in servers.indices) return Packet(0, encodeString("Index out of bounds."))
        val server = servers[id]
        val name = encodeString(server.serverName)
        val directory = encodeString(server.baseDirectory)
        val timeoutDuration = encodeVarLong(server.timeoutDuration.value)
        val state = when (server.serverState.value) {
            Server.ServerState.STOPPED -> 0b00
            Server.ServerState.STARTED -> 0b01
            Server.ServerState.STOPPING -> 0b10
            is Server.ServerState.ERRORED -> 0b11
        } + (if (server.timeoutEnabled.value) 0b100 else 0b000) + (server.timeoutUnit.value.ordinal.toByte() * 0b1000)
        return Packet(1, name + directory + state.toByte() + timeoutDuration)
    }

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