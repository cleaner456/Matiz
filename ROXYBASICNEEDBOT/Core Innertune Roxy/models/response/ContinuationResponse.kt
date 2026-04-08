/*
 * Roxy Project Original (2026)
 * KÃ²i Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package roxy.music.app.models.response
 
 import roxy.music.app.models.MusicShelfRenderer
 import kotlinx.serialization.Serializable
 
 @Serializable
 data class ContinuationResponse(
     val onResponseReceivedActions: List<ResponseAction>?,
 ) {
 
     @Serializable
     data class ResponseAction(
         val appendContinuationItemsAction: ContinuationItems?,
     )
 
     @Serializable
     data class ContinuationItems(
         val continuationItems: List<MusicShelfRenderer.Content>?,
     )
 }

