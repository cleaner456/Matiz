/*
 * Roxy Project Original (2026)
 * KÃ²i Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package roxy.music.app.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicNavigationButtonRenderer(
    val buttonText: Runs,
    val solid: Solid?,
    val iconStyle: IconStyle?,
    val clickCommand: NavigationEndpoint,
) {
    @Serializable
    data class Solid(
        val leftStripeColor: Long,
    )

    @Serializable
    data class IconStyle(
        val icon: Icon,
    )
}

