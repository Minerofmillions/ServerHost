package minerofmillions.serverhost

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class StatusPlayers @JsonCreator constructor(
    @JsonProperty("max") val max: Int,
    @JsonProperty("online") val online: Int,
    @JsonProperty("sample") val sample: List<StatusPlayer>? = null,
)