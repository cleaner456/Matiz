package roxy.music.app

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import roxy.music.app.models.SongItem
import roxy.music.app.models.YouTubeClient
import roxy.music.app.models.YouTubeClient.Companion.WEB_REMIX
import roxy.music.app.models.YouTubeClient.Companion.ANDROID_MUSIC

class MusicViewModel(
    val player: ExoPlayer
) : ViewModel() {

    init {
        // Initialize YouTube object with visitorData on startup
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch fresh visitorData from YouTube
                val freshVisitorData = YouTube.visitorData()
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                if (freshVisitorData != null) {
                    YouTube.visitorData = freshVisitorData
                } else {
                    YouTube.visitorData = "CgtEVG1fM1ZoY2VZOCIYEAA="
                }
            } catch (e: Exception) {
                YouTube.visitorData = "CgtEVG1fM1ZoY2VZOCIYEAA="
            }
        }
    }

    private val _searchResults = MutableStateFlow<List<RoxySearchResult>>(emptyList())
    val searchResults: StateFlow<List<RoxySearchResult>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isStreamLoading = MutableStateFlow(false)
    val isStreamLoading: StateFlow<Boolean> = _isStreamLoading.asStateFlow()

    private val _streamError = MutableStateFlow<String?>(null)
    val streamError: StateFlow<String?> = _streamError.asStateFlow()

    private val _currentPlaying = MutableStateFlow<RoxySearchResult?>(null)
    val currentPlaying: StateFlow<RoxySearchResult?> = _currentPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _totalDuration = MutableStateFlow(0L)
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    // Callback for navigation after song starts loading
    var onSongStarted: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> _isBuffering.value = true
                    Player.STATE_READY -> {
                        _isBuffering.value = false
                        _totalDuration.value = player.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        _isPlaying.value = false
                        _isBuffering.value = false
                    }
                    Player.STATE_IDLE -> _isBuffering.value = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _streamError.value = "Playback error: ${error.message}"
                _isStreamLoading.value = false
                _isBuffering.value = false
            }
        })

        // Position tracker
        viewModelScope.launch {
            while (true) {
                delay(500)
                withContext(Dispatchers.Main) {
                    if (player.isPlaying) {
                        _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                    }
                }
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val page = YouTube.search(query, YouTube.SearchFilter.Songs)
                    .getOrThrow()
                val results = page.items.filterIsInstance<SongItem>().map { song ->
                    RoxySearchResult(
                        title = song.title,
                        artist = song.artists?.joinToString(", ") { it.name } ?: "Unknown",
                        videoId = song.id,
                        thumbnailUrl = song.thumbnail ?: ""
                    )
                }
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
                _streamError.value = "Search failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playSong(song: RoxySearchResult) {
        viewModelScope.launch {
            _currentPlaying.value = song
            _isStreamLoading.value = true
            _streamError.value = null

            // Navigate to player immediately
            withContext(Dispatchers.Main) {
                onSongStarted?.invoke()
            }

            try {
                val streamUrl = withContext(Dispatchers.IO) {
                    try {
                        // Step 1: Get player response from YouTube
                        val playerResponse = YouTube.player(
                            videoId = song.videoId,
                            playlistId = null,
                            client = YouTubeClient.ANDROID_MUSIC
                        ).getOrThrow()
                
                        if (playerResponse.playabilityStatus.status != "OK") {
                            null
                        } else {
                            // Step 2: Find best audio format
                            val format = playerResponse.streamingData?.adaptiveFormats
                                ?.filter { it.isAudio && it.bitrate > 0 }
                                ?.filter { it.url != null || it.signatureCipher != null || it.cipher != null }
                                ?.sortedByDescending { it.bitrate }
                                ?.firstOrNull()
                
                            // Step 3: Decode URL using NewPipeUtils (handles both direct + ciphered)
                            format?.let {
                                NewPipeUtils.getStreamUrl(it, song.videoId).getOrNull()
                            }
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                if (streamUrl != null) {
                    // ExoPlayer MUST be accessed on main thread
                    withContext(Dispatchers.Main) {
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
                } else {
                    _streamError.value = "Could not resolve stream for \"${song.title}\""
                }
            } catch (e: Exception) {
                _streamError.value = "Error: ${e.message}"
            } finally {
                _isStreamLoading.value = false
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

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun seekForward() {
        val newPosition = (player.currentPosition + 10_000).coerceAtMost(player.duration)
        seekTo(newPosition)
    }

    fun seekBackward() {
        val newPosition = (player.currentPosition - 10_000).coerceAtLeast(0)
        seekTo(newPosition)
    }

    fun clearError() {
        _streamError.value = null
    }
}
