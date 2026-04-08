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
import roxy.music.app.models.WatchEndpoint
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
                
                // Fetch initial Home Screen data
                fetchHomeData()
            } catch (e: Exception) {
                YouTube.visitorData = "CgtEVG1fM1ZoY2VZOCIYEAA="
            }
        }
    }

    private val _homeRecs = MutableStateFlow<List<RoxySearchResult>>(emptyList())
    val homeRecs: StateFlow<List<RoxySearchResult>> = _homeRecs.asStateFlow()

    private val _homePop = MutableStateFlow<List<RoxySearchResult>>(emptyList())
    val homePop: StateFlow<List<RoxySearchResult>> = _homePop.asStateFlow()

    private val _homeTrapCity = MutableStateFlow<List<RoxySearchResult>>(emptyList())
    val homeTrapCity: StateFlow<List<RoxySearchResult>> = _homeTrapCity.asStateFlow()

    private suspend fun fetchHomeData() {
        withContext(Dispatchers.IO) {
            try {
                // Recommendation
                val recs = YouTube.search("Hieuthuhai Vpop", YouTube.SearchFilter.FILTER_SONG).getOrNull()
                recs?.items?.filterIsInstance<SongItem>()?.take(8)?.map { song ->
                    RoxySearchResult(song.title, song.artists?.joinToString(", ") { it.name } ?: "Unknown", song.id, song.thumbnail ?: "")
                }?.let { _homeRecs.value = it }

                // Pop Playlists
                val pop = YouTube.search("Pop Music", YouTube.SearchFilter.FILTER_SONG).getOrNull()
                pop?.items?.filterIsInstance<SongItem>()?.take(6)?.map { song ->
                    RoxySearchResult(song.title, song.artists?.joinToString(", ") { it.name } ?: "Unknown", song.id, song.thumbnail ?: "")
                }?.let { _homePop.value = it }

                // Trap City
                val trap = YouTube.search("Trap City", YouTube.SearchFilter.FILTER_SONG).getOrNull()
                trap?.items?.filterIsInstance<SongItem>()?.take(8)?.map { song ->
                    RoxySearchResult(song.title, song.artists?.joinToString(", ") { it.name } ?: "Unknown", song.id, song.thumbnail ?: "")
                }?.let { _homeTrapCity.value = it }
            } catch (e: Exception) {
                // Handle silent fail for home items
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

    private val _upNextQueue = MutableStateFlow<List<RoxySearchResult>>(emptyList())
    val upNextQueue: StateFlow<List<RoxySearchResult>> = _upNextQueue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

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
                        _isBuffering.value = false
                        playNext()
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
                val page = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
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
            // Setup immediate play state and clear active queue context
            _upNextQueue.value = listOf(song)
            _queueIndex.value = 0
            
            playInternal(song)

            // Automix: fetch up next tracks via YouTube.next
            fetchUpNext(song.videoId)
        }
    }

    private fun fetchUpNext(videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nextData = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()
                val nextSongs = nextData?.items?.filterIsInstance<SongItem>()?.map { songItem ->
                    RoxySearchResult(
                        title = songItem.title,
                        artist = songItem.artists?.joinToString(", ") { it.name } ?: "Unknown",
                        videoId = songItem.id,
                        thumbnailUrl = songItem.thumbnail ?: ""
                    )
                }
                
                if (!nextSongs.isNullOrEmpty()) {
                    // Update queue with automix data
                    withContext(Dispatchers.Main) {
                        // The original song played might be at index 0 of nextData, or we just append it
                        val currentQueue = _upNextQueue.value.toMutableList()
                        // Ensure we don't duplicate the currently playing song heavily if it's the first element 
                        val filteredSongs = nextSongs.filter { it.videoId != videoId }
                        currentQueue.addAll(filteredSongs)
                        _upNextQueue.value = currentQueue.distinctBy { it.videoId }
                    }
                }
            } catch (e: Exception) {
                // Silent fail for next, queue will just be empty
            }
        }
    }

    private suspend fun playInternal(song: RoxySearchResult) {
        _currentPlaying.value = song
        _isStreamLoading.value = true
        _streamError.value = null

        // Navigate to player immediately
        withContext(Dispatchers.Main) {
            onSongStarted?.invoke()
        }

        try {
                val streamUrl = withContext(Dispatchers.IO) {
                    val apiClient = RoxyInnerTubeApiClient()
                    val resolver = RoxyStreamResolver(apiClient, RoxyNewPipeDecipherStub())
                    resolver.resolveStreamUrl(
                        videoId = song.videoId,
                        visitorData = YouTube.visitorData ?: "CgtEVG1fM1ZoY2VZOCIYEAA="
                    )
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

    fun playNext() {
        val nextIdx = _queueIndex.value + 1
        val queue = _upNextQueue.value
        if (nextIdx < queue.size) {
            _queueIndex.value = nextIdx
            viewModelScope.launch {
                playInternal(queue[nextIdx])
            }
        } else {
            // Queue exhausted
            _isPlaying.value = false
            player.stop()
        }
    }

    fun playPrevious() {
        if (player.currentPosition > 3000) {
            // If already playing past 3s, just seek to 0
            seekTo(0)
        } else {
            val prevIdx = _queueIndex.value - 1
            if (prevIdx >= 0) {
                val queue = _upNextQueue.value
                _queueIndex.value = prevIdx
                viewModelScope.launch {
                    playInternal(queue[prevIdx])
                }
            } else {
                seekTo(0)
            }
        }
    }

    fun clearError() {
        _streamError.value = null
    }
}
