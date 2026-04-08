package roxy.music.app.models

import roxy.music.app.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
