package minerofmillions.serverhost

import java.io.ByteArrayInputStream
import java.io.DataInputStream

data class HandshakeResponse(val protocolVersion: Int, val serverAddress: String, val port: Int, val isLogin: Boolean) {
    companion object {
        @Throws(PacketEncodingException::class)
        fun parseHandshakeResponse(packet: Packet): HandshakeResponse {
            if (packet.packetId != 0) throw PacketEncodingException("Handshake Response packet id != 0 (${packet.packetId})")
            val data = DataInputStream(ByteArrayInputStream(packet.data))
            val (protocolVersion) = data.readVarInt()
            val serverAddress = data.readString()
            val port = data.readUnsignedShort()
            val (isLogin) = data.readVarInt()
            data.close()
            return HandshakeResponse(protocolVersion, serverAddress, port, isLogin == 2)
        }
    }
}