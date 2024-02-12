package minerofmillions.serverhost

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class StatusVersion @JsonCreator constructor(
    @JsonProperty("name") val name: String,
    @JsonProperty("protocol") val protocol: Int,
)