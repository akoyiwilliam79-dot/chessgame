package com.example.chessmaster

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ChessColors {


    // Main backgrounds
    val Background = Color(0xFFF6F8F2)
    val BackgroundSecondary = Color(0xFFEDF3EA)

    // Cards and panels
    val Surface = Color(0xFFFFFFFF)
    val SurfaceElevated = Color(0xFFF0F6EF)

    // Green brand colors
    val Green = Color(0xFF087F5B)
    val GreenDark = Color(0xFF075B45)
    val GreenDeep = Color(0xFF064635)
    val GreenLight = Color(0xFFDDF5E8)
    val Mint = Color(0xFFB7E4C7)
    val Sage = Color(0xFF78A98B)

    // Accent
    val Gold = Color(0xFFD6AD52)
    val GoldLight = Color(0xFFF3D88B)

    // Text
    val TextPrimary = Color(0xFF18352B)
    val TextSecondary = Color(0xFF53675B)
    val TextMuted = Color(0xFF7C8C81)
    val TextOnGreen = Color(0xFFFFFFFF)

    // Chessboard
    val BoardLight = Color(0xFFF0E6D2)
    val BoardDark = Color(0xFF78A98B)
    val SelectedSquare = Color(0xFFFFD166)
    val LegalMove = Color(0x8050C878)
    val LastMove = Color(0x80F6D365)

    // Status colors
    val Success = Color(0xFF168653)
    val Danger = Color(0xFFD94848)
    val Warning = Color(0xFFD99A20)
    val Info = Color(0xFF2878C8)

    // Borders and overlays
    val Border = Color(0xFFDCE6DB)
    val GreenBorder = Color(0x6687B89A)
    val Overlay = Color(0x66000000)


}

private val ChessMasterLightScheme = lightColorScheme(


primary = ChessColors.Green,
onPrimary = ChessColors.TextOnGreen,

primaryContainer = ChessColors.GreenLight,
onPrimaryContainer = ChessColors.GreenDeep,

secondary = ChessColors.Gold,
onSecondary = ChessColors.TextPrimary,

secondaryContainer = ChessColors.Mint,
onSecondaryContainer = ChessColors.GreenDeep,

background = ChessColors.Background,
onBackground = ChessColors.TextPrimary,

surface = ChessColors.Surface,
onSurface = ChessColors.TextPrimary,

surfaceVariant = ChessColors.SurfaceElevated,
onSurfaceVariant = ChessColors.TextSecondary,

outline = ChessColors.Border,
outlineVariant = ChessColors.GreenBorder,

error = ChessColors.Danger,
onError = ChessColors.TextOnGreen


)

@Composable
fun ChessMasterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ChessMasterLightScheme,
        content = content
    )
}
