package ru.zenegix.miza.model.telegraph

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TelegraphGetPageResponse(
    val result: Result,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Result(
        val content: List<ContentEntry>
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class ContentEntry(
            val tag: String,
            val children: List<String>
        )
    }
}
