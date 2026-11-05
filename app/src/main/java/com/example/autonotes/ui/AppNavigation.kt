package com.example.autonotes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.autonotes.auth.SessionState
import com.example.autonotes.ui.auth.AccountScreen
import com.example.autonotes.ui.auth.AuthScreen
import com.example.autonotes.ui.auth.SessionViewModel
import com.example.autonotes.ui.camera.CameraScreen
import com.example.autonotes.ui.player.PlayerScreen
import com.example.autonotes.ui.recordings.RecordingsScreen

@Composable
fun AppNavigation(sessionViewModel: SessionViewModel = hiltViewModel()) {
    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()

    when (sessionState) {
        SessionState.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        SessionState.SIGNED_OUT -> AuthScreen()
        SessionState.SIGNED_IN -> MainNavigation()
    }
}

@Composable
private fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "recordings") {
        composable("recordings") {
            RecordingsScreen(
                onOpenRecording = { id -> navController.navigate("player/$id") },
                onOpenCamera = { navController.navigate("camera") },
                onOpenAccount = { navController.navigate("account") }
            )
        }
        composable("camera") {
            CameraScreen(onDone = { navController.popBackStack() })
        }
        composable("account") {
            AccountScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "player/{recordingId}",
            arguments = listOf(navArgument("recordingId") { type = NavType.LongType })
        ) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }
    }
}