package com.example.chessmaster

enum class GameMode(val title: String, val description: String) {
    LOCAL(
        title = "Local Multiplayer",
        description = "Play chess against another person on the same device."
    ),
    QUICK_MATCH(
        title = "Quick Match",
        description = "Start a quick chess match."
    ),
    COMPUTER(
        title = "Play vs Computer",
        description = "Challenge the ChessMaster AI."
    ),
    PLAYER_VS_COMPUTER(
        title = "Player vs Computer",
        description = "Play against the computer at your chosen difficulty."
    ),
    PLAYER_VS_PLAYER(
        title = "Player vs Player",
        description = "Play against another person on this device."
    )
}
