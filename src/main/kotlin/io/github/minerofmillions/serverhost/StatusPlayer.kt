package io.github.minerofmillions.serverhost

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class StatusPlayer @JsonCreator constructor(
    @JsonProperty("name") val name: String,
    @JsonProperty("id") val id: String,
)