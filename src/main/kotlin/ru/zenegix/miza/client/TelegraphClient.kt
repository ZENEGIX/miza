package ru.zenegix.miza.client

import com.fasterxml.jackson.module.kotlin.readValue
import ru.zenegix.miza.model.telegraph.TelegraphCreateAccountResponse
import ru.zenegix.miza.model.telegraph.TelegraphEditPageResponse
import ru.zenegix.miza.model.telegraph.TelegraphGetPageResponse
import ru.zenegix.miza.utils.OBJECT_MAPPER
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object TelegraphClient {

    private val client = HttpClient.newBuilder().build()

    fun createAccount(): String {
        val request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("https://api.telegra.ph/createAccount?short_name=Miza&author_name=Miza"))
            .build()

        val rawResponse = client.send(request, HttpResponse.BodyHandlers.ofString()).body()
        val response = OBJECT_MAPPER.readValue<TelegraphCreateAccountResponse>(rawResponse)
        return response.result.accessToken
    }

    fun readPageContent(path: String): String? {
        val request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("https://api.telegra.ph/getPage/$path?return_content=true"))
            .build()

        val rawResponse = client.send(request, HttpResponse.BodyHandlers.ofString()).body()
        val response = OBJECT_MAPPER.readValue<TelegraphGetPageResponse>(rawResponse)
        return response.result.content.firstOrNull()?.children?.firstOrNull()
    }

    fun createOrEditPage(path: String?, title: String, content: String, accessToken: String): String {
        val encodedContent = URLEncoder.encode(
            OBJECT_MAPPER.writeValueAsString(
                listOf(mapOf("tag" to "p", "children" to listOf(content)))
            ),
            "UTF-8"
        )
        val url = if (path == null) {
            "https://api.telegra.ph/createPage"
        } else {
            "https://api.telegra.ph/editPage/$path"
        }
        val request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url +
                    "?access_token=$accessToken" +
                    "&title=$title" +
                    "&content=$encodedContent"))
            .build()

        val rawResponse = client.send(request, HttpResponse.BodyHandlers.ofString()).body()
        val response = OBJECT_MAPPER.readValue<TelegraphEditPageResponse>(rawResponse)
        return response.result.path
    }
}
