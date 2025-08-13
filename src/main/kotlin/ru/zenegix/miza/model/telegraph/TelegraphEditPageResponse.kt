package ru.zenegix.miza.model.telegraph

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class TelegraphEditPageResponse(
    val result: Result,
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Result(
        val path: String,
    )
}
