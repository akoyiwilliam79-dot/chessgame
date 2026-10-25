package com.example.chessmaster

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.chessmaster.screens.SplashScreen
import com.example.chessmaster.screens.WelcomeScreen

import com.example.chessmaster.ui.screens.GameModeScreen
import com.example.chessmaster.ui.screens.OpponentSelectionScreen

enum class AppScreen {
    SPLASH,
    WELCOME,
    GAME_MODE,
    OPPONENT_SELECTION
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChessMasterNavigation()
        }
    }
}

@Composable
fun ChessMasterNavigation() {

    val context = LocalContext.current

    var currentScreen by remember {
        mutableStateOf(AppScreen.SPLASH)
    }

    var selectedMode by remember {
        mutableStateOf(GameMode.PLAYER_VS_COMPUTER)
    }

    var selectedOpponent by remember {
        mutableIntStateOf(1)
    }

    /*
     * Prevent the Back button from accidentally
     * taking the user out of the splash flow.
     */
    BackHandler(enabled = currentScreen != AppScreen.SPLASH) {

        currentScreen = when (currentScreen) {

            AppScreen.OPPONENT_SELECTION ->
                AppScreen.GAME_MODE

            AppScreen.GAME_MODE ->
                AppScreen.WELCOME

            AppScreen.WELCOME ->
                AppScreen.WELCOME

            AppScreen.SPLASH ->
                AppScreen.SPLASH
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(animationSpec = tween(350)) togetherWith
                    fadeOut(animationSpec = tween(250))
        },
        label = "ChessMasterNavigation"
    ) { screen ->

        when (screen) {

            // FIRST SCREEN: Splash
            AppScreen.SPLASH -> {
                SplashScreen(
                    onFinished = {
                        currentScreen = AppScreen.WELCOME
                    }
                )
            }

            // SECOND SCREEN: Welcome
            AppScreen.WELCOME -> {
                WelcomeScreen(
                    onPlay = {
                        currentScreen = AppScreen.GAME_MODE
                    }
                )
            }

            // THIRD SCREEN: Game modes
            AppScreen.GAME_MODE -> {
                GameModeScreen(
                    selectedMode = selectedMode,

                    onModeSelected = { mode ->

                        selectedMode = mode

                        when (mode) {

                            GameMode.PLAYER_VS_COMPUTER -> {
                                currentScreen =
                                    AppScreen.OPPONENT_SELECTION
                            }

                            GameMode.PLAYER_VS_PLAYER -> {
                                launchChessGame(
                                    context = context,
                                    mode = GameMode.PLAYER_VS_PLAYER,
                                    opponentIndex = selectedOpponent
                                )
                            }

                            else -> {
                                currentScreen =
                                    AppScreen.OPPONENT_SELECTION
                            }
                        }
                    },

                    ) {
                    currentScreen = AppScreen.WELCOME
                }
            }

            // FOURTH SCREEN: Computer opponent selection
            AppScreen.OPPONENT_SELECTION -> {
                OpponentSelectionScreen(
                    selectedOpponent = selectedOpponent,

                    onOpponentSelected = { opponent ->
                        selectedOpponent = opponent
                    },

                    onStartMatch = {
                        launchChessGame(
                            context = context,
                            mode = selectedMode,
                            opponentIndex = selectedOpponent
                        )
                    },

                    onBack = {
                        currentScreen = AppScreen.GAME_MODE
                    }
                )
            }
        }
    }
}

private fun launchChessGame(
    context: android.content.Context,
    mode: GameMode,
    opponentIndex: Int
) {
    val intent = Intent(context, Chess3DActivity::class.java).apply {
        putExtra("GAME_MODE", mode.name)
        putExtra("OPPONENT_INDEX", opponentIndex)
    }

    context.startActivity(intent)
}