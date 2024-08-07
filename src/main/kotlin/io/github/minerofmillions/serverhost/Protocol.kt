package io.github.minerofmillions.serverhost

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.*
import java.net.SocketException
import java.util.zip.Inflater

@Throws(IOException::class)
fun InputStream.readVarInt(): Pair<Int, Int> {
    var i = 0
    var j = 0
    while (true) {
        val k = read().takeIf { it > -1 } ?: throw IOException("Stream ended early reading VarInt. ($j bytes)")
        i += (k and 0x7f) shl j++ * 7
        if (j > 5) throw RuntimeException("VarInt too big")
        if ((k and 0x80) == 0) break
    }
    return i to j
}

@Throws(IOException::class)
fun InputStream.readVarLong(): Pair<Long, Int> {
    var i = 0L
    var j = 0
    while (true) {
        val k = read().takeIf { it > -1 } ?: throw IOException("Stream ended early reading VarLong. ($j bytes)")
        i += (k and 0x7f) shl (j++ * 7)
        if (j > 10) throw RuntimeException("VarLong too big")
        if ((k and 0x80) == 0) break
    }
    return i to j
}

fun encodeVarInt(param: Int): ByteArray {
    var paramInt = param
    val bytes = mutableListOf<Byte>()
    while (true) {
        if ((paramInt and 0x7f.inv()) == 0) {
            bytes.add(paramInt.toByte())
            return bytes.toByteArray()
        }

        bytes.add(((paramInt and 0x7f) + 0x80).toByte())
        paramInt = paramInt ushr 7
    }
}

fun encodeVarLong(param: Long): ByteArray {
    var paramLong = param
    val bytes = mutableListOf<Byte>()
    while (true) {
        if ((paramLong and 0x7f.inv()) == 0L) {
            bytes.add(paramLong.toByte())
            return bytes.toByteArray()
        }

        bytes.add(((paramLong and 0x7f) + 0x80).toByte())

        paramLong = paramLong ushr 7
    }
}

fun encodeString(string: String): ByteArray {
    val encoded = string.encodeToByteArray()
    val length = encodeVarInt(encoded.size)
    return length + encoded
}

@Throws(IOException::class)
fun OutputStream.writeVarInt(param: Int) {
    write(encodeVarInt(param))
}

@Throws(IOException::class)
fun InputStream.readString(): String {
    val (length, _) = readVarInt()
    return readNBytes(length).decodeToString()
}

@Throws(IOException::class)
fun OutputStream.writeString(string: String) {
    write(encodeString(string))
}

fun InputStream.readUncompressedPacket(): Packet {
    val (packetLength, _) = readVarInt()
    val (packetId, packetIdBytes) = readVarInt()
    val remainingSize = packetLength - packetIdBytes
    val data = try {
        readNBytes(remainingSize)
    } catch (e: SocketException) {
        throw Exception("Failed to read $remainingSize bytes for packet 0x${packetId.toString(16)}", e)
    }
    return Packet(packetId, data)
}

fun InputStream.readCompressedPacket(): Packet {
    val (packetLength) = readVarInt()
    val (dataLength, dataLengthBytes) = readVarInt()
    if (dataLength == 0) return readUncompressedPacket()
    val compressedData = readNBytes(packetLength - dataLengthBytes)
    val inflater = Inflater()
    inflater.setInput(compressedData)
    val result = ByteArray(dataLength)
    inflater.inflate(result)
    inflater.end()
    return ByteArrayInputStream(result).readUncompressedPacket()
}

fun OutputStream.writePacket(packet: Packet) {
    write(encodePacket(packet))
}

fun encodePacket(packet: Packet): ByteArray {
    val id = encodeVarInt(packet.packetId)
    val totalLength = id.size + packet.data.size
    return encodeVarInt(totalLength) + id + packet.data
}

data class Packet(val packetId: Int, val data: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Packet

        if (packetId != other.packetId) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packetId
        result = 31 * result + data.contentHashCode()
        return result
    }

    companion object {
        private val mapper = ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)

        fun handshakePacket(protocolVersion: Int, serverAddress: String, port: Int, isLogin: Boolean): Packet {
            val data = ByteArrayOutputStream().use {
                it.writeVarInt(protocolVersion)
                it.writeString(serverAddress)
                it.writeBytes(byteArrayOf((port / 256).toByte(), port.toByte()))
                it.writeVarInt(if (isLogin) 2 else 1)
                it.toByteArray()
            }
            return Packet(0, data)
        }

        fun statusRequestPacket(): Packet = Packet(0, emptyArray<Byte>().toByteArray())
        fun statusResponsePacket(statusResponse: StatusResponse): Packet =
            Packet(
                0,
                encodeString(mapper.writeValueAsString(statusResponse))
            )

        fun disconnectPacket(text: Text): Packet {
            return Packet(0, encodeString(mapper.writeValueAsString(text)))
        }
    }
}