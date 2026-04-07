package roxy.music.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import roxy.music.app.ui.screens.HomeScreen
import roxy.music.app.ui.screens.PlayerScreen
import roxy.music.app.ui.screens.SearchScreen

class MainActivity : ComponentActivity() {
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var viewModel: MusicViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exoPlayer = ExoPlayer.Builder(this).build()
        viewModel = MusicViewModel(exoPlayer)

        setContent {
            val darkScheme = darkColorScheme(
                primary = Color(0xFF7C4DFF),
                onPrimary = Color.White,
                secondary = Color(0xFFB388FF),
                background = Color(0xFF0D0D1A),
                surface = Color(0xFF151530),
                onBackground = Color.White,
                onSurface = Color.White
            )

            MaterialTheme(colorScheme = darkScheme) {
                val navController = rememberNavController()
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route ?: "home"
                val currentSong by viewModel.currentPlaying.collectAsState()
                val isPlaying by viewModel.isPlaying.collectAsState()

                Scaffold(
                    containerColor = Color(0xFF0D0D1A),
                    bottomBar = {
                        Column {
                            // Mini player bar
                            AnimatedVisibility(
                                visible = currentSong != null && currentRoute != "player",
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                MiniPlayerBar(
                                    song = currentSong,
                                    isPlaying = isPlaying,
                                    onTap = {
                                        navController.navigate("player") {
                                            launchSingleTop = true
                                        }
                                    },
                                    onPlayPause = { viewModel.togglePlayPause() }
                                )
                            }

                            // Navigation bar
                            NavigationBar(
                                containerColor = Color(0xFF0A0A18),
                                contentColor = Color.White,
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home", fontSize = 11.sp) },
                                    selected = currentRoute == "home",
                                    onClick = {
                                        navController.navigate("home") { launchSingleTop = true }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFFB388FF),
                                        selectedTextColor = Color(0xFFB388FF),
                                        unselectedIconColor = Color(0xFF6B6B8D),
                                        unselectedTextColor = Color(0xFF6B6B8D),
                                        indicatorColor = Color(0xFF1A1A3A)
                                    )
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                    label = { Text("Search", fontSize = 11.sp) },
                                    selected = currentRoute == "search",
                                    onClick = {
                                        navController.navigate("search") { launchSingleTop = true }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFFB388FF),
                                        selectedTextColor = Color(0xFFB388FF),
                                        unselectedIconColor = Color(0xFF6B6B8D),
                                        unselectedTextColor = Color(0xFF6B6B8D),
                                        indicatorColor = Color(0xFF1A1A3A)
                                    )
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.PlayCircle, contentDescription = "Player") },
                                    label = { Text("Player", fontSize = 11.sp) },
                                    selected = currentRoute == "player",
                                    onClick = {
                                        navController.navigate("player") { launchSingleTop = true }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFFB388FF),
                                        selectedTextColor = Color(0xFFB388FF),
                                        unselectedIconColor = Color(0xFF6B6B8D),
                                        unselectedTextColor = Color(0xFF6B6B8D),
                                        indicatorColor = Color(0xFF1A1A3A)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") {
                                HomeScreen(
                                    onNavigateToSearch = {
                                        navController.navigate("search") { launchSingleTop = true }
                                    }
                                )
                            }
                            composable("search") {
                                SearchScreen(
                                    viewModel = viewModel,
                                    onNavigateToPlayer = {
                                        navController.navigate("player") { launchSingleTop = true }
                                    }
                                )
                            }
                            composable("player") { PlayerScreen(viewModel = viewModel) }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }
}

@Composable
private fun MiniPlayerBar(
    song: RoxySearchResult?,
    isPlaying: Boolean,
    onTap: () -> Unit,
    onPlayPause: () -> Unit
) {
    if (song == null) return

    val barGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF1A0A2E), Color(0xFF200E3A))
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(barGradient)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color(0xFF8888AA),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play/Pause button
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7C4DFF).copy(alpha = 0.3f))
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
