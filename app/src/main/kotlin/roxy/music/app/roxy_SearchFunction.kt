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
            val items = sectionWrapper.musicShelfRenderer?.contents ?: continue
            for (itemWrapper in items) {
                val renderer = itemWrapper.musicResponsiveListItemRenderer ?: continue
                
                val videoId = renderer.playlistItemData?.videoId ?: continue
                val title = renderer.flexColumns.getOrNull(0)
                    ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text ?: "Unknown Title"
                
                val artist = renderer.flexColumns.getOrNull(1)
                    ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.joinToString("") { it.text } ?: "Unknown Artist"
                
                val thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url ?: ""
                
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
