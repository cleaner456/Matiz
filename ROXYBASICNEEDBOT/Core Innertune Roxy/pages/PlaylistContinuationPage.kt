/*
 * Roxy Project Original (2026)
 * KÃ²i Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package roxy.music.app.pages

import roxy.music.app.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)

