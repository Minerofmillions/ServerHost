package minerofmillions.serverhost

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes(JsonSubTypes.Type(PlainTextObject::class))
sealed interface Text

data class PlainTextObject @JsonCreator constructor(@JsonProperty("text") val text: String) : Text
