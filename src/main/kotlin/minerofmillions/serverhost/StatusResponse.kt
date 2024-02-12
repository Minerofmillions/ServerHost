package minerofmillions.serverhost

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class StatusResponse @JsonCreator constructor(
    @JsonProperty("version") val version: StatusVersion,
    @JsonProperty("players") val players: StatusPlayers,
    @JsonProperty("description") val description: Text,
    @JsonProperty("favicon") val favicon: String? = null,
)