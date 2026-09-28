package com.wsnow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wsnow.app.ui.create.CreateScreen
import com.wsnow.app.ui.game.GameScreen
import com.wsnow.app.ui.home.HomeScreen
import com.wsnow.app.ui.settings.SettingsScreen
import com.wsnow.app.ui.theme.WSnowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WSnowTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onOpen = { id -> nav.navigate("game/$id") },
                            onCreate = { nav.navigate("create") },
                            onSettings = { nav.navigate("settings") },
                        )
                    }
                    composable("create") {
                        CreateScreen(
                            onBack = { nav.popBackStack() },
                            onSettings = { nav.navigate("settings") },
                            onCreated = { id ->
                                nav.navigate("game/$id") { popUpTo("home") }
                            },
                        )
                    }
                    composable("game/{id}") { entry ->
                        GameScreen(
                            puzzleId = entry.arguments?.getString("id").orEmpty(),
                            onBack = { nav.popBackStack() },
                            onNewPuzzle = { nav.navigate("create") { popUpTo("home") } },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
