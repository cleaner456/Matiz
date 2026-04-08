/*
 * Roxy Project Original (2026)
 * KÃ²i Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package roxy.music.app.pages

import roxy.music.app.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)

