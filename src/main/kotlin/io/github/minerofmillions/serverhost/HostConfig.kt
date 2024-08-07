package io.github.minerofmillions.serverhost

import com.arkivanov.decompose.ComponentContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.properties.Properties.Default.decodeFromStringMap
import kotlinx.serialization.properties.Properties.Default.encodeToStringMap
import java.io.File

@Serializable
data class HostConfig(val servers: List<ServerConfig>, val startPort: Int = 25565) {
    companion object {
        private val configFile = File("host.properties")
        private val DEFAULT by lazy {
            HostConfig(
                listOf(
                    ServerConfig("/path/to/server1", "run.sh", "server1"),
                    ServerConfig("/path/to/server2", "run.sh", "server2")
                )
            )
        }

        @OptIn(ExperimentalSerializationApi::class)
        fun readConfig(): HostConfig {
            if (!configFile.exists()) writeConfig(DEFAULT)
            return decodeFromStringMap(serializer(), readPropertiesFile(configFile))
        }

        @OptIn(ExperimentalSerializationApi::class)
        fun writeConfig(config: HostConfig) {
            writePropertiesFile(encodeToStringMap(serializer(), config), configFile)
        }

        fun getServers(context: ComponentContext) = readConfig().let { config ->
            config.servers.mapIndexed { index, it -> Server.fromConfig(it, config.startPort + index, context) }
        }
    }
}

fun writePropertiesFile(values: Map<String, String>, file: File) =
    file.writeText(values.entries.joinToString("\n") { (key, value) -> "${key}=${value}" })

fun readPropertiesFile(file: File): Map<String, String> =
    file.readLines().filterNot { it.isBlank() || it.startsWith('#') }.associate {
        val (k, v) = it.split('=', limit = 2)
        k to v
    }

@Serializable
data class ServerConfig(val baseDirectory: String, val startCommand: String, val serverName: String)
