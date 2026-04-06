package roxy.music.app

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
                finalUrl = newPipeDecipherEngine.decipher(bestFormat.signatureCipher)
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
    fun decipher(signatureCipher: String): String {
        return "https://deciphered-url.com"
    }
}
