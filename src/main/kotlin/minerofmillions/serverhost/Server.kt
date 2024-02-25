package minerofmillions.serverhost

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.observe
import com.arkivanov.decompose.value.operator.map
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import minerofmillions.utils.splitCommand
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.minutes

class Server(
    serverDirectory: File,
    startCommand: String,
    val serverName: String,
    private val portToUse: Int,
    componentContext: ComponentContext,
) : ComponentContext by componentContext {
    private val processBuilder = ProcessBuilder().directory(serverDirectory).command(startCommand.splitCommand())
    val serverState = MutableValue<ServerState>(ServerState.STOPPED)
    val serverLog = MutableValue(emptyList<String>())
    private val process = MutableValue(ProcessBuilder("java", "-version").start())
    private lateinit var listener: Job
    private lateinit var logTransfer: Job
    val autoOff = MutableValue(true)
    val autoOffDuration = MutableValue(10.minutes.inWholeMilliseconds)

    val logShowing = serverState.map { it == ServerState.STARTED || it == ServerState.STOPPING }
    val isErrored = serverState.map { it is ServerState.ERRORED }

    private val coroutineScope = coroutineScope(Dispatchers.IO)

    val baseDirectory: String = serverDirectory.absolutePath
    private val faviconFile = serverDirectory.resolve("host.favicon")

    init {
        if (!serverDirectory.isDirectory) {
            serverState.value = ServerState.ERRORED("Invalid Server Directory")
        } else {
            serverDirectory.resolve(startCommand).takeIf(File::isFile)?.let { commandFile ->
                val contents = commandFile.readText().trim()
                if (contents.endsWith("pause")) {
                    commandFile.writeText(contents.removeSuffix("pause").trim())
                }
            }

            serverDirectory.resolve("eula.txt").takeUnless(File::exists)?.writeText("eula=true")

            val serverPropertiesFile = serverDirectory.resolve("server.properties")
            if (!serverPropertiesFile.exists()) {
                println("Running server at \"$serverDirectory\" to generate properties.")
                start()
                stop()
            }

            val properties = readPropertiesFile(serverPropertiesFile).toMutableMap()
            properties["server-port"] = portToUse.toString()
            writePropertiesFile(properties, serverPropertiesFile)

            startListener()
        }
    }

    fun setServerActive(active: Boolean) {
        if (serverState.value == ServerState.STOPPED && !active || serverState.value == ServerState.STARTED && active) return
        else if (active) {
            start()
        } else {
            stop()
        }
    }

    fun awaitServerStopped() = process.value.waitFor()

    private fun startListener() {
        listener = coroutineScope.launch {
            val socket = ServerSocket(portToUse)
            socket.soTimeout = 500
            while (!socket.isClosed && this.isActive) {
                val connection = try {
                    socket.accept()
                } catch (_: SocketTimeoutException) {
                    continue
                }
                launch { handleConnection(connection) }
            }
        }
    }

    private suspend fun handleConnection(connection: Socket) {
        println(connection.remoteSocketAddress)
        val (toClient, fromClient) = withContext(Dispatchers.IO) {
            connection.getOutputStream() to connection.getInputStream()
        }

        try {
            val handshake = fromClient.readUncompressedPacket()
            val handshakeResponse = HandshakeResponse.parseHandshakeResponse(handshake)

            val nextPacket = fromClient.readUncompressedPacket()

            if (handshakeResponse.isLogin) {
                toClient.writePacket(Packet.disconnectPacket(PlainTextObject("$serverName will start. Please wait.")))
                start()
            } else {
                toClient.writePacket(
                    Packet.statusResponsePacket(
                        StatusResponse(
                            StatusVersion("1.19.2", 760),
                            StatusPlayers(0, 0),
                            PlainTextObject("$serverName is hosted. Please connect to start."),
                            faviconFile.takeIf(File::exists)?.readText()?.trim()
                        )
                    )
                )
            }
            toClient.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopListener() {
        listener.cancel()
    }

    private fun start() {
        if (serverState.value != ServerState.STOPPED) return
        stopListener()
        serverState.value = ServerState.STARTED
        serverLog.value = emptyList()
        process.value = processBuilder.start()
        logTransfer = coroutineScope.launch {
            coroutineScope {
                transfer(process.value.inputReader())
                transfer(process.value.errorReader())
                autoOff()
            }
        }
        process.value.onExit().thenRun {
            serverState.value = ServerState.STOPPED
            logTransfer.cancel()
            startListener()
        }
    }

    private fun stop() {
        if (serverState.value != ServerState.STARTED) return
        serverState.value = ServerState.STOPPING
        coroutineScope.launch {
            val it = process.value.outputWriter()
            it.write("stop")
            it.newLine()
            it.flush()
            process.value.waitFor()
        }
    }

    fun gatherFavicon() = coroutineScope.launch {
        val favicon = pingServer().await()?.favicon ?: return@launch
        assert(favicon.startsWith("data:image/png;base64,"))
        faviconFile.writeText(favicon)
    }

    private fun pingServer(): Deferred<StatusResponse?> = coroutineScope.async {
        if (serverState.value != ServerState.STARTED) null
        else Socket("localhost", portToUse).use { socket ->
            DataInputStream(socket.getInputStream()).use { fromServer ->
                DataOutputStream(socket.getOutputStream()).use { toServer ->
                    toServer.writePacket(Packet.handshakePacket(765, "localhost", portToUse, false))
                    toServer.writePacket(Packet.statusRequestPacket())
                    val status = fromServer.readUncompressedPacket()

                    val statusData = ByteArrayInputStream(status.data)
                    val (responseStringLength) = statusData.readVarInt()
                    val responseString = statusData.readNBytes(responseStringLength)

                    mapper.readValue(responseString, StatusResponse::class.java)
                }
            }
        }
    }

    private fun CoroutineScope.transfer(stream: BufferedReader) = launch {
        while (isActive) {
            val nextLine = stream.readLine() ?: break
            serverLog.value += nextLine
        }
        stream.close()
    }

    private fun CoroutineScope.autoOff() {
        var currentJob: Job by Delegates.notNull()
        autoOff.observe(lifecycle) {
            if (it) currentJob = launch {
                var hadNoPlayers = false
                while (isActive) {
                    delay(autoOffDuration.value)
                    val response = pingServer().await() ?: continue
                    if (response.players.online == 0) {
                        if (hadNoPlayers) stop()
                        else hadNoPlayers = true
                    } else hadNoPlayers = false
                }
            }
            else currentJob.cancel()
        }
    }

    fun setAutoOff(active: Boolean) {
        autoOff.value = active
    }

    fun setAutoOffDelay(delay: String) {
        val newDelay = delay.toLongOrNull() ?: return
        autoOffDuration.value = newDelay
    }

    companion object {
        private val mapper = ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        fun fromConfig(config: ServerConfig, portToUse: Int, context: ComponentContext): Server =
            Server(File(config.baseDirectory), config.startCommand, config.serverName, portToUse, context)
    }

    sealed interface ServerState {
        data object STARTED : ServerState
        data object STOPPING : ServerState
        data object STOPPED : ServerState
        data class ERRORED(val reason: String) : ServerState
    }
}
