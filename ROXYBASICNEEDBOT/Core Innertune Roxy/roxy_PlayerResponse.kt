package roxy.music.app

import kotlinx.serialization.Serializable

@Serializable
data class RoxyPlayerResponse(
    val playabilityStatus: RoxyPlayabilityStatus? = null,
    val streamingData: RoxyStreamingData? = null
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
    val signatureCipher: String? = null,
    val mimeType: String,
    val bitrate: Int,
    val averageBitrate: Int? = null,
    val audioQuality: String? = null,
    val contentLength: String? = null
)

// Search response models (simplified for representation)
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
    val contents: List<RoxyItemSectionRendererWrapper> = emptyList()
)

@Serializable
data class RoxyItemSectionRendererWrapper(
    val itemSectionRenderer: RoxyItemSectionRenderer? = null
)

@Serializable
data class RoxyItemSectionRenderer(
    val contents: List<RoxyMusicResponsiveListItemRendererWrapper> = emptyList()
)

@Serializable
data class RoxyMusicResponsiveListItemRendererWrapper(
    val musicResponsiveListItemRenderer: RoxyMusicResponsiveListItemRenderer? = null
)

@Serializable
data class RoxyMusicResponsiveListItemRenderer(
    val trackingParams: String? = null
)
