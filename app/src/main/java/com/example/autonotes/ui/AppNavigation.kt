package com.example.autonotes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.autonotes.ui.recordings.RecordingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "recordings") {
        composable("recordings") {
            RecordingsScreen(onOpenRecording = { id -> navController.navigate("player/$id") })
        }
        composable(
            route = "player/{recordingId}",
            arguments = listOf(navArgument("recordingId") { type = NavType.LongType })
        ) {
            // Заглушка: настоящий плеер сделаем на этапе 3Б
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Плеер появится на этапе 3Б")
            }
        }
    }
}