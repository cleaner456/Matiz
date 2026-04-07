package roxy.music.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel(
    val player: ExoPlayer
) : ViewModel() {

    private val apiClient = RoxyInnerTubeApiClient()
    private val resolver = RoxyStreamResolver(apiClient, RoxyNewPipeDecipherStub())
    private val searchFunction = RoxySearchFunction(apiClient)

    private val _searchResults = MutableStateFlow<List<RoxySearchResult>>(emptyList())
    val searchResults: StateFlow<List<RoxySearchResult>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentPlaying = MutableStateFlow<RoxySearchResult?>(null)
    val currentPlaying: StateFlow<RoxySearchResult?> = _currentPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
    }

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Ideally extract visitorData generically. We'll use the hardcoded one for simplicity
                val visitorData = "CgtEVG1fM1ZoY2VZOCIYEAA%3D"
                val results = searchFunction.performSearch(query, visitorData)
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playSong(song: RoxySearchResult) {
        viewModelScope.launch {
            _currentPlaying.value = song
            val visitorData = "CgtEVG1fM1ZoY2VZOCIYEAA%3D"
            val streamUrl = resolver.resolveStreamUrl(song.videoId, visitorData)
            
            if (streamUrl != null) {
                val mediaItem = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaId(song.videoId)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setArtworkUri(android.net.Uri.parse(song.thumbnailUrl))
                            .build()
                    )
                    .build()
                    
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }
}
