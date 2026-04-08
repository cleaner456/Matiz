package roxy.music.app.playback.queues

import androidx.media3.common.MediaItem
import roxy.music.app.YouTube
import roxy.music.app.models.WatchEndpoint
import roxy.music.app.extensions.toMediaItem
import roxy.music.app.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubeQueue(
    private var endpoint: WatchEndpoint,
    override val preloadItem: MediaMetadata? = null,
    private val followAutomixPreview: Boolean = false,
) : Queue {
    private var continuation: String? = null

    override suspend fun getInitialStatus(): Queue.Status {
        val nextResult =
            withContext(IO) {
                YouTube.next(
                    endpoint = endpoint,
                    continuation = continuation,
                    followAutomixPreview = followAutomixPreview,
                ).getOrThrow()
            }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation
        return Queue.Status(
            title = nextResult.title,
            items = nextResult.items.map { it.toMediaItem() },
            mediaItemIndex = nextResult.currentIndex ?: 0,
        )
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<MediaItem> {
        val nextResult =
            withContext(IO) {
                YouTube.next(
                    endpoint = endpoint,
                    continuation = continuation,
                    followAutomixPreview = followAutomixPreview,
                ).getOrThrow()
            }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation
        return nextResult.items.map { it.toMediaItem() }
    }

    companion object {
        fun radio(song: MediaMetadata) = YouTubeQueue(WatchEndpoint(song.id), song)
    }
}
