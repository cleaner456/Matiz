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
                primary = Color(0xFFFFFFFF),
                onPrimary = Color.Black,
                secondary = Color(0xFFB388FF),
                background = Color(0xFF0A0A0A),
                surface = Color(0xFF111111),
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
                    containerColor = Color(0xFF0A0A0A),
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
                                containerColor = Color(0xFF111111),
                                contentColor = Color.White,
                                tonalElevation = 0.dp,
                                modifier = Modifier.height(64.dp)
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home", fontSize = 11.sp, fontWeight = if (currentRoute == "home") FontWeight.Bold else FontWeight.Normal) },
                                    selected = currentRoute == "home",
                                    onClick = {
                                        navController.navigate("home") { launchSingleTop = true }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = Color.White,
                                        unselectedIconColor = Color(0xFF888888),
                                        unselectedTextColor = Color(0xFF888888),
                                        indicatorColor = Color(0x26FFFFFF) // rgba(255,255,255,0.15)
                                    )
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                    label = { Text("Search", fontSize = 11.sp, fontWeight = if (currentRoute == "search") FontWeight.Bold else FontWeight.Normal) },
                                    selected = currentRoute == "search",
                                    onClick = {
                                        navController.navigate("search") { launchSingleTop = true }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = Color.White,
                                        unselectedIconColor = Color(0xFF888888),
                                        unselectedTextColor = Color(0xFF888888),
                                        indicatorColor = Color(0x26FFFFFF)
                                    )
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                                    label = { Text("Library", fontSize = 11.sp, fontWeight = if (currentRoute == "library") FontWeight.Bold else FontWeight.Normal) },
                                    selected = currentRoute == "library",
                                    onClick = {
                                        navController.navigate("library") { launchSingleTop = true }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = Color.White,
                                        unselectedIconColor = Color(0xFF888888),
                                        unselectedTextColor = Color(0xFF888888),
                                        indicatorColor = Color(0x26FFFFFF)
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
                                    viewModel = viewModel,
                                    onNavigateToSearch = {
                                        navController.navigate("search") { launchSingleTop = true }
                                    },
                                    onNavigateToPlayer = {
                                        navController.navigate("player") { launchSingleTop = true }
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
                            composable("library") {
                                roxy.music.app.ui.screens.LibraryScreen()
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color(0xFF888888),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play/Pause button
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
