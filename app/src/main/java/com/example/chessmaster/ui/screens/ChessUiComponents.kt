package com.example.chessmaster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Midnight = Color(0xFF0B1020)
internal val Panel = Color(0xFF151D30)
internal val PanelLight = Color(0xFF202B40)
internal val Gold = Color(0xFFD8B15A)
internal val Ivory = Color(0xFFF5F0E5)
internal val Muted = Color(0xFF9AA6BC)

internal data class ChessOpponent(
    val name: String,
    val title: String,
    val rating: String,
    val symbol: String
)

internal val chessOpponents = listOf(
    ChessOpponent(
        "Beginner Bot",
        "Easy · Learn the game",
        "800 ELO",
        "♟"
    ),
    ChessOpponent(
        "The Strategist",
        "Medium · Think carefully",
        "1200 ELO",
        "♞"
    ),
    ChessOpponent(
        "Grandmaster",
        "Hard · Test your skills",
        "1800 ELO",
        "♜"
    ),
    ChessOpponent(
        "Chess Legend",
        "Expert · A serious challenge",
        "2200 ELO",
        "♛"
    )
)

@Composable
internal fun BrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    PanelLight,
                    RoundedCornerShape(15.dp)
                )
                .border(
                    1.dp,
                    Gold.copy(alpha = 0.45f),
                    RoundedCornerShape(15.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "♞",
                fontSize = 31.sp,
                color = Gold
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "CHESSMASTER",
                color = Ivory,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Text(
                text = "THINK AHEAD. PLAY SMART.",
                color = Gold,
                fontSize = 9.sp,
                letterSpacing = 1.5.sp
            )
        }

        Text(
            text = "♔",
            color = Gold,
            fontSize = 29.sp
        )
    }
}

@Composable
internal fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Gold,
            contentColor = Midnight
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.1.sp
        )
    }
}

@Composable
internal fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .border(
                1.dp,
                Gold.copy(alpha = 0.55f),
                RoundedCornerShape(16.dp)
            )
            .background(
                Panel,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Gold,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
internal fun ModeCard(
    icon: String,
    title: String,
    description: String,
    badge: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Gold else Color(0xFF29344A),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                Color(0xFF202B40)
            } else {
                Panel
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        if (selected) Gold.copy(alpha = 0.15f) else PanelLight,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    color = Gold,
                    fontSize = 30.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Ivory,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = description,
                    color = Muted,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = badge,
                    color = Gold,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = if (selected) "✓" else "›",
                color = if (selected) Gold else Muted,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun OpponentCard(
    opponent: ChessOpponent,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (selected) Gold else Color(0xFF29344A),
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                Color(0xFF202B40)
            } else {
                Panel
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(55.dp)
                    .background(PanelLight, CircleShape)
                    .border(
                        1.dp,
                        Gold.copy(alpha = 0.45f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = opponent.symbol,
                    color = Gold,
                    fontSize = 31.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = opponent.name,
                    color = Ivory,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = opponent.title,
                    color = Muted,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    text = opponent.rating,
                    color = Gold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (selected) {
                Text(
                    text = "✓",
                    color = Gold,
                    fontSize = 23.sp
                )
            }
        }
    }
}