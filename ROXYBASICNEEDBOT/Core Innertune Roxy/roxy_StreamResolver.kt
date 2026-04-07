package roxy.music.app

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import java.net.URLDecoder
import okhttp3.OkHttpClient

class RoxyStreamResolver(
    private val apiClient: RoxyInnerTubeApiClient,
    private val newPipeDecipherEngine: RoxyNewPipeDecipherStub
) {
    // In-memory cache for stream URLs: authFingerprint:videoId:itag -> Pair<Url, ExpiryTimestamp>
    private val streamCache = mutableMapOf<String, Pair<String, Long>>()
    
    // Track blocked clients based on failure (blocked util time)
    private val blockedClients = mutableMapOf<RoxyClientName, Long>()

    suspend fun resolveStreamUrl(videoId: String, visitorData: String, poToken: String? = null): String? {
        for (client in RoxyClientIdentities.fallbackList) {
            val blockedUntil = blockedClients[client.clientName]
            if (blockedUntil != null && System.currentTimeMillis() < blockedUntil) {
                continue
            }

            val response = try {
                apiClient.getPlayer(videoId, client, visitorData, poToken)
            } catch (e: Exception) {
                continue
            }
            
            if (response.playabilityStatus?.status == "ERROR") {
                continue
            }

            val validFormats = response.streamingData?.adaptiveFormats?.filter {
                it.mimeType.startsWith("audio/") && it.bitrate > 0 && (it.url != null || it.signatureCipher != null)
            }?.sortedByDescending { it.bitrate }

            if (validFormats.isNullOrEmpty()) continue

            val bestFormat = validFormats.firstOrNull { it.mimeType.contains("opus") } ?: validFormats.first()
            val cacheKey = "$visitorData:$videoId:${bestFormat.itag}"
            
            val cached = streamCache[cacheKey]
            if (cached != null && System.currentTimeMillis() < cached.second) {
                return cached.first
            }

            var finalUrl = bestFormat.url
            if (finalUrl == null && bestFormat.signatureCipher != null) {
                finalUrl = newPipeDecipherEngine.decipher(bestFormat.signatureCipher!!, videoId)
            }

            if (finalUrl != null) {
                val isValid = apiClient.validateUrl(finalUrl, client)
                if (isValid) {
                    val expiresInSeconds = response.streamingData.expiresInSeconds?.toLongOrNull() ?: 21600L
                    val expiryTimestamp = System.currentTimeMillis() + (expiresInSeconds * 1000)
                    streamCache[cacheKey] = finalUrl to expiryTimestamp
                    return finalUrl
                } else {
                    blockedClients[client.clientName] = System.currentTimeMillis() + (10 * 60 * 1000)
                }
            }
        }
        return null
    }
}

class RoxyNewPipeDecipherStub {

    // NewPipe ko ek baar initialize karna hai app start pe
    init {
        try {
            NewPipe.init(RoxyOkHttpDownloader())
        } catch (e: Exception) {
            // Already initialized — ignore
        }
    }

    fun decipher(signatureCipher: String, videoId: String): String? {
        return try {
            // Step 1: signatureCipher parse karo — format hai: s=SIG&sp=sig&url=BASE_URL
            val params = signatureCipher.split("&").associate { param ->
                val idx = param.indexOf('=')
                if (idx == -1) param to ""
                else param.substring(0, idx) to
                    URLDecoder.decode(param.substring(idx + 1), "UTF-8")
            }

            val encodedSig = params["s"] ?: return null
            val sigParam   = params["sp"] ?: "sig"
            val baseUrl    = params["url"] ?: return null

            // Step 2: NewPipe se signature decipher karo
            // Yeh internally YouTube JS player fetch karke signature decode karta hai
            val deciphered = YoutubeJavaScriptPlayerManager
                .deobfuscateSignature(videoId, encodedSig)

            // Step 3: Deciphered signature URL me append karo
            "$baseUrl&$sigParam=$deciphered"

        } catch (e: Exception) {
            null
        }
    }
}

// NewPipe ke liye simple OkHttp downloader wrapper
class RoxyOkHttpDownloader : Downloader() {
    private val httpClient = OkHttpClient()

    override fun execute(request: Request): Response {
        val reqBuilder = okhttp3.Request.Builder().url(request.url())

        request.headers().forEach { (key, values) ->
            values.forEach { value -> reqBuilder.addHeader(key, value) }
        }

        val body = request.dataToSend()?.let {
            okhttp3.RequestBody.create(null, it)
        }

        reqBuilder.method(request.httpMethod(), body)

        val response = httpClient.newCall(reqBuilder.build()).execute()
        val responseBody = response.body?.string() ?: ""
        val headers = response.headers.toMultimap()

        return Response(
            response.code,
            response.message,
            headers,
            responseBody,
            request.url()
        )
    }
}
