package roxy.music.app

data class RoxySearchResult(
    val title: String,
    val artist: String,
    val videoId: String,
    val thumbnailUrl: String
)

class RoxySearchFunction(
    private val apiClient: RoxyInnerTubeApiClient
) {
    suspend fun performSearch(query: String, visitorData: String): List<RoxySearchResult> {
        val response = apiClient.search(query, RoxyClientIdentities.WEB_REMIX, visitorData)
        val results = mutableListOf<RoxySearchResult>()
        
        val tabs = response.contents?.tabbedSearchResultsRenderer?.tabs ?: return emptyList()
        val sections = tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents ?: return emptyList()
        
        for (sectionWrapper in sections) {
            val items = sectionWrapper.itemSectionRenderer?.contents ?: continue
            for (itemWrapper in items) {
                val renderer = itemWrapper.musicResponsiveListItemRenderer ?: continue
                
                val videoId = "mock_video_id"
                val title = "Mock Title extracted from InnerTube response"
                val artist = "Mock Artist"
                val thumbnail = "https://i.ytimg.com/vi/mock/hqdefault.jpg"
                
                results.add(
                    RoxySearchResult(
                        title = title,
                        artist = artist,
                        videoId = videoId,
                        thumbnailUrl = thumbnail
                    )
                )
            }
        }
        return results
    }
}
