package roxy.music.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import roxy.music.app.MusicViewModel

@Composable
fun PlayerScreen(viewModel: MusicViewModel) {
    val currentSong by viewModel.currentPlaying.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val isStreamLoading by viewModel.isStreamLoading.collectAsState()
    val streamError by viewModel.streamError.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val totalDuration by viewModel.totalDuration.collectAsState()

    var isLiked by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }

    val backgroundGradient = Brush.verticalGradient(
        0.0f to Color(0xFF0D2137),
        0.6f to Color(0xFF071423),
        1.0f to Color(0xFF050E18)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(bottom = 80.dp), // Clear bottom nav
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (currentSong == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No song playing",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        } else {
            currentSong?.let { song ->
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* pop back if mapped */ }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NOW PLAYING",
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = song.title, // or album
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                    }
                }

                // Album art
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 32.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .shadow(24.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                    )

                    if (isBuffering || isStreamLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                        }
                    }
                }

                // Error message
                streamError?.let { error ->
                    Text(
                        text = error,
                        color = Color(0xFFFF8A80),
                        modifier = Modifier.padding(horizontal = 24.dp),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Song info + like
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song.artist,
                            color = Color(0xFFAAAAAA),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { isLiked = !isLiked }) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = if (isLiked) Color(0xFFFF4081) else Color(0xFFAAAAAA),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress bar
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    val progressRatio = if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat() else 0f
                    
                    Slider(
                        value = progressRatio,
                        onValueChange = { fraction ->
                            if (totalDuration > 0) viewModel.seekTo((fraction * totalDuration).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().offset(y = (-8).dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = formatTime(currentPosition), color = Color(0xFFAAAAAA), fontSize = 12.sp)
                        Text(text = formatTime(totalDuration), color = Color(0xFFAAAAAA), fontSize = 12.sp)
                    }
                }

                // Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isShuffle = !isShuffle }) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (isShuffle) Color(0xFF00D4FF) else Color(0xFFAAAAAA))
                    }
                    IconButton(onClick = { viewModel.playPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    
                    FloatingActionButton(
                        onClick = { viewModel.togglePlayPause() },
                        shape = CircleShape,
                        containerColor = Color.White,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    IconButton(onClick = { viewModel.playNext() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    IconButton(onClick = { isRepeat = !isRepeat }) {
                        Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = if (isRepeat) Color(0xFF00D4FF) else Color(0xFFAAAAAA))
                    }
                }

                // Bottom Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFFAAAAAA))
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Playlist", tint = Color(0xFFAAAAAA))
                    }
                }

                // Lyrics Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A3A5C))
                        .clickable { }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, contentDescription = "Lyrics", tint = Color(0xFF00D4FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lyrics", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("Show", color = Color(0xFFAAAAAA), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
