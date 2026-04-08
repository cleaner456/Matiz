/*
 * Roxy Project Original (2026)
 * KÃ²i Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package roxy.music.app.models.body

import roxy.music.app.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)

