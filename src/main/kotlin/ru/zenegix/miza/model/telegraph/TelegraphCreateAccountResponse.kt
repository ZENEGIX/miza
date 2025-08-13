package ru.zenegix.miza.model.telegraph

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TelegraphCreateAccountResponse(
    val result: Result,
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Result(
        @JsonProperty("access_token")
        val accessToken: String,
    )
}
