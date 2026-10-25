package com.example.chessmaster.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay


private val BackgroundColor = Color(0xFF10141C)
private val GoldColor = Color(0xFFFFC857)
private val MutedTextColor = Color(0xFFADB5C4)

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {

    // Automatically continue after 2.5 seconds.
    LaunchedEffect(Unit) {
        delay(2500L)
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(
        label = "SplashAnimation"
    )

    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "KnightScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "ChessMaster emblem",
                tint = GoldColor,
                modifier = Modifier
                    .size(100.dp)
                    .scale(iconScale)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "CHESSMASTER",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "THINK. PLAN. CONQUER.",
                color = GoldColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(42.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = GoldColor,
                strokeWidth = 2.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Preparing your battlefield...",
                color = MutedTextColor,
                fontSize = 12.sp
            )
        }

        Text(
            text = "A NEW CHESS EXPERIENCE",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .let { it },
            color = MutedTextColor,
            fontSize = 10.sp,
            letterSpacing = 2.sp
        )
    }
}