package com.example.chessmaster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OpponentSelectionScreen(
    selectedOpponent: Int,
    onOpponentSelected: (Int) -> Unit,
    onStartMatch: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Midnight)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 20.dp,
                vertical = 22.dp
            )
    ) {

        BrandHeader()

        Spacer(Modifier.height(28.dp))

        Text(
            text = "SELECT YOUR RIVAL",
            color = Ivory,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Choose the challenge that suits your skill.",
            color = Muted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(23.dp))

        chessOpponents.forEachIndexed { index, opponent ->

            OpponentCard(
                opponent = opponent,
                selected = index == selectedOpponent,
                onClick = {
                    onOpponentSelected(index)
                }
            )

            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(12.dp))

        // Start the match using the selected opponent.
        PrimaryButton(
            text = "START MATCH  ♟",
            onClick = onStartMatch
        )

        Spacer(Modifier.height(10.dp))

        // Return to the game mode screen.
        OutlineButton(
            text = "←  BACK TO GAME MODES",
            onClick = onBack
        )
    }
}