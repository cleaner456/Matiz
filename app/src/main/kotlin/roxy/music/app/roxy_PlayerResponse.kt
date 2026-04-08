package roxy.music.app

import kotlinx.serialization.Serializable

@Serializable
data class RoxyPlayerResponse(
    val playabilityStatus: RoxyPlayabilityStatus? = null,
    val streamingData: RoxyStreamingData? = null,
    val videoDetails: RoxyVideoDetails? = null
)

@Serializable
data class RoxyVideoDetails(
    val videoId: String? = null,
    val title: String? = null,
    val author: String? = null,
    val lengthSeconds: String? = null
)

@Serializable
data class RoxyPlayabilityStatus(
    val status: String, // OK, ERROR, UNPLAYABLE
    val reason: String? = null
)

@Serializable
data class RoxyStreamingData(
    val expiresInSeconds: String? = null,
    val adaptiveFormats: List<RoxyAdaptiveFormat> = emptyList()
)

@Serializable
data class RoxyAdaptiveFormat(
    val itag: Int,
    val url: String? = null,
    val cipher: String? = null,
    val signatureCipher: String? = null,
    val mimeType: String,
    val bitrate: Int,
    val averageBitrate: Int? = null,
    val audioQuality: String? = null,
    val approxDurationMs: String? = null,
    val contentLength: String? = null
)

// Search response models
@Serializable
data class RoxySearchResponse(
    val contents: RoxySearchContents? = null
)

@Serializable
data class RoxySearchContents(
    val tabbedSearchResultsRenderer: RoxyTabbedSearchResultsRenderer? = null
)

@Serializable
data class RoxyTabbedSearchResultsRenderer(
    val tabs: List<RoxyTab> = emptyList()
)

@Serializable
data class RoxyTab(
    val tabRenderer: RoxyTabRenderer? = null
)

@Serializable
data class RoxyTabRenderer(
    val content: RoxySectionListRendererWrapper? = null
)

@Serializable
data class RoxySectionListRendererWrapper(
    val sectionListRenderer: RoxySectionListRenderer? = null
)

@Serializable
data class RoxySectionListRenderer(
    val contents: List<RoxyMusicShelfRendererWrapper> = emptyList()
)

@Serializable
data class RoxyMusicShelfRendererWrapper(
    val musicShelfRenderer: RoxyMusicShelfRenderer? = null
)

@Serializable
data class RoxyMusicShelfRenderer(
    val contents: List<RoxyMusicResponsiveListItemRendererWrapper> = emptyList()
)

@Serializable
data class RoxyMusicResponsiveListItemRendererWrapper(
    val musicResponsiveListItemRenderer: RoxyMusicResponsiveListItemRenderer? = null
)

@Serializable
data class RoxyMusicResponsiveListItemRenderer(
    val trackingParams: String? = null,
    val flexColumns: List<RoxyFlexColumn> = emptyList(),
    val playlistItemData: RoxyPlaylistItemData? = null,
    val thumbnail: RoxyThumbnailDetails? = null
)

@Serializable
data class RoxyFlexColumn(
    val musicResponsiveListItemFlexColumnRenderer: RoxyMusicResponsiveListItemFlexColumnRenderer? = null
)

@Serializable
data class RoxyMusicResponsiveListItemFlexColumnRenderer(
    val text: RoxyTextStructure? = null
)

@Serializable
data class RoxyTextStructure(
    val runs: List<RoxyRun> = emptyList()
)

@Serializable
data class RoxyRun(
    val text: String
)

@Serializable
data class RoxyPlaylistItemData(
    val videoId: String
)

@Serializable
data class RoxyThumbnailDetails(
    val musicThumbnailRenderer: RoxyMusicThumbnailRenderer? = null
)

@Serializable
data class RoxyMusicThumbnailRenderer(
    val thumbnail: RoxyThumbnailObject? = null
)

@Serializable
data class RoxyThumbnailObject(
    val thumbnails: List<RoxyThumbnailUrl> = emptyList()
)

@Serializable
data class RoxyThumbnailUrl(
    val url: String
)
