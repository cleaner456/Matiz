package roxy.music.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route ?: "home"

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") },
                                selected = currentRoute == "home",
                                onClick = { navController.navigate("home") }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                label = { Text("Search") },
                                selected = currentRoute == "search",
                                onClick = { navController.navigate("search") }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Player") },
                                label = { Text("Player") },
                                selected = currentRoute == "player",
                                onClick = { navController.navigate("player") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") { HomeScreen() }
                            composable("search") { SearchScreen(viewModel = viewModel) }
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
