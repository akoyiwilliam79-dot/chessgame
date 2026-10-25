package com.example.chessmaster.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF0A0F15)
private val Gold = Color(0xFFE5BE69)
private val Muted = Color(0xFF9BA9B8)
private val Panel = Color(0xFF151E28)

@Composable
fun WelcomeScreen(
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))

            Text(
                text = "CHESSMASTER",
                color = Gold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.5.sp
            )

            Text(
                text = "YOUR NEXT MOVE STARTS HERE",
                color = Muted,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.weight(1f))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Panel,
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 22.dp,
                        vertical = 30.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "♜   ♞   ♝",
                        color = Gold,
                        fontSize = 35.sp
                    )

                    Spacer(Modifier.height(22.dp))

                    Text(
                        text = "MASTER THE BOARD",
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Enter a world of strategy, precision, " +
                                "and immersive 3D chess. Challenge " +
                                "the computer or play with a friend.",
                        color = Muted,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FeatureItem("♟", "3D BOARD")
                        FeatureItem("⚔", "AI BATTLES")
                        FeatureItem("♛", "STRATEGY")
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Background
                )
            ) {
                Text(
                    text = "LET'S PLAY  →",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.padding(top = 12.dp)
                .height(8.dp))

            Text(
                text = "BUILT FOR THINKERS",
                color = Muted,
                fontSize = 15.sp,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun FeatureItem(
    symbol: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = symbol,
            color = Gold,
            fontSize = 24.sp
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = label,
            color = Muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}