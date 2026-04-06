package roxy.music.app

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class RoxyInnerTubeApiClient {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val baseUrl = "https://music.youtube.com/youtubei/v1"

    suspend fun getPlayer(
        videoId: String,
        ytClient: RoxyYouTubeClient,
        visitorData: String,
        poToken: String? = null,
        signatureTimestamp: Int = 19725
    ): RoxyPlayerResponse {
        val payload = buildJsonObject {
            put("context", buildJsonObject {
                put("client", buildJsonObject {
                    put("clientName", ytClient.clientName.name)
                    put("clientVersion", ytClient.clientVersion)
                    put("hl", ytClient.hl)
                    put("gl", ytClient.gl)
                    put("visitorData", visitorData)
                })
            })
            put("videoId", videoId)
            put("playbackContext", buildJsonObject {
                put("contentPlaybackContext", buildJsonObject {
                    put("signatureTimestamp", signatureTimestamp)
                })
            })
            if (poToken != null) {
                put("serviceIntegrityDimensions", buildJsonObject {
                    put("poToken", poToken)
                })
            }
        }

        return client.post("$baseUrl/player") {
            contentType(ContentType.Application.Json)
            setHeaders(ytClient, visitorData)
            setBody(payload)
        }.body()
    }

    suspend fun search(
        query: String,
        ytClient: RoxyYouTubeClient,
        visitorData: String
    ): RoxySearchResponse {
        val payload = buildJsonObject {
            put("context", buildJsonObject {
                put("client", buildJsonObject {
                    put("clientName", ytClient.clientName.name)
                    put("clientVersion", ytClient.clientVersion)
                    put("hl", ytClient.hl)
                    put("gl", ytClient.gl)
                    put("visitorData", visitorData)
                })
            })
            put("query", query)
        }

        return client.post("$baseUrl/search") {
            contentType(ContentType.Application.Json)
            setHeaders(ytClient, visitorData)
            setBody(payload)
        }.body()
    }

    suspend fun validateUrl(url: String, ytClient: RoxyYouTubeClient): Boolean {
        val response = client.head(url) {
            header("Range", "bytes=0-0")
            header("User-Agent", ytClient.userAgent)
        }
        return response.status.isSuccess() || response.status.value == 206 // Partial Content
    }

    private fun HttpRequestBuilder.setHeaders(ytClient: RoxyYouTubeClient, visitorData: String) {
        // Need to parse clientName mapping to ints or strings properly based on InnerTube specs
        header("X-YouTube-Client-Name", "1") // often 1 for WEB
        header("X-YouTube-Client-Version", ytClient.clientVersion)
        header("X-Goog-Api-Format-Version", "1")
        header("X-Goog-Visitor-Id", visitorData)
        header("User-Agent", ytClient.userAgent)
        header("Referer", "https://music.youtube.com/")
        header("Origin", "https://music.youtube.com")
    }
}
