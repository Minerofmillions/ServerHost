package minerofmillions.serverhost

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.getValue
import com.arkivanov.decompose.value.operator.map
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import minerofmillions.utils.splitCommand
import java.io.*
import java.net.ConnectException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
    val command = MutableValue("")

    private val process = MutableValue(ProcessBuilder("java", "-version").start())
    private val processWriter = process.map { it.outputWriter() }
    private lateinit var listener: Job
    private lateinit var logTransfer: Job

    val timeoutEnabled = MutableValue(true)
    val timeoutDuration = MutableValue(5L)
    val timeoutUnit = MutableValue(TimeUnit.MINUTES)

    val logShowing = serverState.map { it == ServerState.STARTED || it == ServerState.STOPPING }
    val isErrored = serverState.map { it is ServerState.ERRORED }

    private val coroutineScope = coroutineScope(Dispatchers.IO)

    val baseDirectory: String = serverDirectory.absolutePath
    private val responseFile = serverDirectory.resolve("host.response")

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
            if (!serverPropertiesFile.exists()) runBlocking {
                println("Running server at \"$serverDirectory\" to generate properties.")
                startServer()
                stop()
                awaitServerStopped()
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
            socket.soTimeout = 250
            while (!socket.isClosed && isActive) {
                val connection = try {
                    socket.accept()
                } catch (_: SocketTimeoutException) {
                    continue
                }
                launch { handleConnection(connection) }
            }
            println("Closing listener: $serverName")
            socket.close()
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
                val responseBase = try {
                    responseFile.takeIf(File::isFile)?.let { mapper.readValue(it, StatusResponse::class.java) }
                } catch (e: DatabindException) {
                    null
                }
                toClient.writePacket(
                    Packet.statusResponsePacket(
                        StatusResponse(
                            responseBase?.version ?: StatusVersion("1.19.2", 760),
                            StatusPlayers(0, 0),
                            PlainTextObject("$serverName is hosted. Please connect to start."),
                            responseBase?.favicon
                        )
                    )
                )
            }
            toClient.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun stopListener() {
        if (!::listener.isInitialized) return
        listener.cancel()
        listener.join()
    }

    private fun start() = coroutineScope.launch {
        if (serverState.value != ServerState.STOPPED) return@launch
        stopListener()
        startServer()
    }

    private suspend fun startServer() = coroutineScope {
        serverState.value = ServerState.STARTED
        serverLog.value = emptyList()
        process.value = processBuilder.start()
        logTransfer = launch {
            supervisorScope {
                launch { transfer(process.value.inputReader()) }
                launch { transfer(process.value.errorReader()) }
                launch { autoOff() }
            }
        }
        process.value.onExit().thenRun {
            println("Server $serverName stopped.")
            serverState.value = ServerState.STOPPED
            logTransfer.cancel()
            startListener()
        }
    }

    private fun stop() {
        if (serverState.value != ServerState.STARTED) return
        serverState.value = ServerState.STOPPING
        coroutineScope.launch {
            writeToServer("stop")
            process.value.waitFor()
        }
    }

    fun gatherFavicon() = coroutineScope.launch {
        val statusResponse = pingServer().await() ?: return@launch
        mapper.writeValue(responseFile, statusResponse)
    }

    private suspend fun pingServer() = coroutineScope {
        async {
            if (serverState.value != ServerState.STARTED) null
            else try {
                Socket("localhost", portToUse).use { socket ->
                    DataInputStream(socket.getInputStream()).use { fromServer ->
                        DataOutputStream(socket.getOutputStream()).use { toServer ->
                            toServer.writePacket(Packet.handshakePacket(765, "localhost", portToUse, false))
                            toServer.writePacket(Packet.statusRequestPacket())
                            val status = fromServer.readUncompressedPacket()

                            StatusResponse.fromPacket(status)
                        }
                    }
                }
            } catch (e: ConnectException) {
                null
            }
        }
    }

    private suspend fun transfer(stream: BufferedReader) = coroutineScope {
        while (isActive) {
            val nextLine = stream.readLine() ?: break
            serverLog.value += nextLine
        }
        stream.close()
    }

    private suspend fun autoOff() = coroutineScope {
        val timeoutEnabled by timeoutEnabled
        val timeoutDuration by timeoutDuration
        val timeoutUnit by timeoutUnit
        var hadNoPlayers = false
        while (isActive) {
            if (timeoutEnabled) {
                delay(timeoutUnit(timeoutDuration) / 2)
                val numPlayersOnline = pingServer().await()?.players?.online
                if (numPlayersOnline == null) {
                    println("Skipping timeout check (null response): $serverName")
                    continue
                }
                else if (numPlayersOnline == 0) {
                    if (hadNoPlayers) stop()
                    else hadNoPlayers = true
                } else hadNoPlayers = false
            } else {
                hadNoPlayers = false
                delay(AUTO_OFF_PING_DELAY)
            }
        }
    }

    fun setAutoOff(active: Boolean) {
        timeoutEnabled.value = active
    }

    fun setAutoOffDelay(delay: String) {
        val newDelay = delay.toLongOrNull() ?: return
        timeoutDuration.value = newDelay
    }

    fun setCommand(str: String) {
        this.command.value = str
    }

    fun sendCommand() {
        writeToServer(command.value)
        command.value = ""
    }

    private fun writeToServer(str: String) {
        val it = processWriter.value
        it.write(str)
        it.newLine()
        it.flush()
    }

    fun selectTimeoutUnit(unit: TimeUnit) {
        timeoutUnit.value = unit
    }

    companion object {
        private val mapper = ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        private const val AUTO_OFF_PING_DELAY: Long = 1000

        fun fromConfig(config: ServerConfig, portToUse: Int, context: ComponentContext): Server =
            Server(File(config.baseDirectory), config.startCommand, config.serverName, portToUse, context)
    }

    sealed interface ServerState {
        data object STARTED : ServerState
        data object STOPPING : ServerState
        data object STOPPED : ServerState
        data class ERRORED(val reason: String) : ServerState
    }

    enum class TimeUnit(val abbreviation: String, val conversion: (Long) -> Duration) {
        MILLIS("ms", { it.milliseconds }),
        SECONDS("secs", { it.seconds }),
        MINUTES("mins", { it.minutes }),
        HOURS("hrs", { it.hours }),
        DAYS("days", { it.days });

        operator fun invoke(duration: Long) = conversion(duration)
    }
}
