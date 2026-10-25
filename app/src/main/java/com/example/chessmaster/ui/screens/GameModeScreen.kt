package com.example.chessmaster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.chessmaster.GameMode

@Composable
fun GameModeScreen(
    selectedMode: GameMode,
    onModeSelected: (GameMode) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Midnight)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 20.dp,
                vertical = 22.dp
            ),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        BrandHeader()

        Spacer(Modifier.height(30.dp))

        Text(
            text = "CHOOSE YOUR BATTLE",
            color = Ivory,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "How do you want to play today?",
            color = Muted,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(25.dp))

        ModeCard(
            icon = "⚡",
            title = "Quick Match",
            description = "Jump straight into a match.",
            badge = "FAST PLAY",
            selected = selectedMode == GameMode.QUICK_MATCH,
            onClick = {
                onModeSelected(GameMode.QUICK_MATCH)
            }
        )

        Spacer(Modifier.height(13.dp))

        ModeCard(
            icon = "♟",
            title = "Play vs Computer",
            description = "Challenge an AI opponent.",
            badge = "AI CHALLENGE",
            selected = selectedMode == GameMode.COMPUTER,
            onClick = {
                onModeSelected(GameMode.COMPUTER)
            }
        )

        Spacer(Modifier.height(13.dp))

        ModeCard(
            icon = "♔",
            title = "Local 2 Players",
            description = "Share the board with a friend.",
            badge = "PASS & PLAY",
            selected = selectedMode == GameMode.LOCAL,
            onClick = {
                onModeSelected(GameMode.LOCAL)
            }
        )

        Spacer(Modifier.height(25.dp))

        PrimaryButton(
            text = if (selectedMode == GameMode.COMPUTER) {
                "CHOOSE OPPONENT  →"
            } else {
                "CONTINUE  →"
            },
            onClick = onContinue
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "YOUR GAME. YOUR STRATEGY.",
            color = Muted,
            fontSize = 10.sp,
            letterSpacing = 1.6.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}