package minerofmillions.serverhost

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import java.io.ByteArrayInputStream

data class StatusResponse @JsonCreator constructor(
    @JsonProperty("version") val version: StatusVersion,
    @JsonProperty("players") val players: StatusPlayers,
    @JsonProperty("description") val description: Text,
    @JsonProperty("favicon") val favicon: String? = null,
) {
    companion object {
        private val mapper = ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        fun fromPacket(packet: Packet): StatusResponse? {
            val statusData = ByteArrayInputStream(packet.data)
            val (responseStringLength) = statusData.readVarInt()
            val responseString = statusData.readNBytes(responseStringLength)

            return try {
                mapper.readValue(responseString, StatusResponse::class.java)
            } catch (e: ValueInstantiationException) {
                e.printStackTrace()
                null
            }
        }
    }
}