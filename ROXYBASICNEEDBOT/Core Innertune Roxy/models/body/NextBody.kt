/*
 * Roxy Project Original (2026)
 * KÃ²i Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package roxy.music.app.models.body

import roxy.music.app.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class NextBody(
    val context: Context,
    val videoId: String?,
    val playlistId: String?,
    val playlistSetVideoId: String?,
    val index: Int?,
    val params: String?,
    val continuation: String?,
)

