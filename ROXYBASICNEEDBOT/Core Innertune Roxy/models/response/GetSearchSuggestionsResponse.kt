/*
 * Roxy Project Original (2026)
 * KÃ²i Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package roxy.music.app.models.response

import roxy.music.app.models.SearchSuggestionsSectionRenderer
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsResponse(
    val contents: List<Content>?,
) {
    @Serializable
    data class Content(
        val searchSuggestionsSectionRenderer: SearchSuggestionsSectionRenderer,
    )
}

