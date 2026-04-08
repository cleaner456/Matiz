package roxy.music.app.ui.menu

import roxy.music.app.db.entities.PlaylistSongMap
import roxy.music.app.YouTube

suspend fun removeSongFromRemotePlaylist(
    playlistBrowseId: String,
    playlistSongMap: PlaylistSongMap,
) {
    val setVideoIds =
        playlistSongMap.setVideoId?.let(::listOf)
            ?: YouTube.playlistEntrySetVideoIds(playlistBrowseId, playlistSongMap.songId)
                .getOrDefault(emptyList())

    setVideoIds
        .distinct()
        .forEach { setVideoId ->
            YouTube.removeFromPlaylist(playlistBrowseId, playlistSongMap.songId, setVideoId)
        }
}
