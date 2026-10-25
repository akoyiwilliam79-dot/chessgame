package com.example.chessmaster

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext


import com.example.chessmaster.screens.WelcomeScreen
import com.example.chessmaster.ui.screens.GameModeScreen
import com.example.chessmaster.ui.screens.OpponentSelectionScreen
import com.example.chessmaster.ui.screens.SplashScreen


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

    // Opponent indexes are zero-based:
    // 0 = Easy, 1 = Medium, 2 = Hard.
    var selectedOpponent by remember {
        mutableIntStateOf(1)
    }

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
            fadeIn(
                animationSpec = tween(350)
            ) togetherWith fadeOut(
                animationSpec = tween(250)
            )
        },
        label = "ChessMasterNavigation"
    ) { screen ->

        when (screen) {

            AppScreen.SPLASH -> {

                SplashScreen() {
                    currentScreen = AppScreen.WELCOME
                }
            }

            AppScreen.WELCOME -> {

                WelcomeScreen(
                    onPlay = {
                        currentScreen = AppScreen.GAME_MODE
                    }
                )
            }

            AppScreen.GAME_MODE -> {

                GameModeScreen(
                    selectedMode = selectedMode,

                    onModeSelected = { mode ->
                        selectedMode = mode
                    },

                    onBack = {
                        currentScreen = AppScreen.WELCOME
                    },

                    onContinue = {

                        when (selectedMode) {

                            GameMode.PLAYER_VS_PLAYER,
                            GameMode.LOCAL -> {

                                launchChessGame(
                                    context = context,
                                    mode = selectedMode,
                                    opponentIndex = selectedOpponent
                                )
                            }

                            else -> {

                                currentScreen =
                                    AppScreen.OPPONENT_SELECTION
                            }
                        }
                    }
                )
            }

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
    context: Context,
    mode: GameMode,
    opponentIndex: Int
) {

    /*
     * The current chess engine supports two actual match types:
     * Player vs Computer and Player vs Player.
     *
     * Normalize the menu choices to those supported modes
     * so the AI doesn't accidentally remain inactive.
     */
    val engineMode = when (mode) {

        GameMode.PLAYER_VS_PLAYER,
        GameMode.LOCAL -> GameMode.PLAYER_VS_PLAYER

        else -> GameMode.PLAYER_VS_COMPUTER
    }

    val intent = Intent(
        context,
        Chess3DActivity::class.java
    ).apply {

        putExtra(
            "GAME_MODE",
            engineMode.name
        )

        putExtra(
            "OPPONENT_INDEX",
            opponentIndex.coerceIn(0, 2)
        )
    }

    context.startActivity(intent)
}