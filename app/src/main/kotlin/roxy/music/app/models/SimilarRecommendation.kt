package roxy.music.app.models

import roxy.music.app.models.YTItem
import roxy.music.app.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
