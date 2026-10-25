package com.example.chessmaster

/**
 * The human player's preferred color.
 */
enum class PlayerColor(
    val title: String,
    val description: String
) {
    WHITE(
        title = "White Pieces",
        description = "Play first as White."
    ),

    BLACK(
        title = "Black Pieces",
        description = "Play as Black against the computer."
    )
}

/**
 * Computer opponent difficulty levels.
 *
 * searchDepth controls how deeply the AI searches
 * possible moves, provided ChessAI uses this value.
 */
enum class AIDifficulty(
    val title: String,
    val description: String,
    val searchDepth: Int
) {
    EASY(
        title = "Easy",
        description = "A relaxed game for beginners.",
        searchDepth = 1
    ),

    MEDIUM(
        title = "Medium",
        description = "A balanced challenge.",
        searchDepth = 3
    ),

    HARD(
        title = "Hard",
        description = "A tougher opponent.",
        searchDepth = 4
    )
}

/**
 * Settings for the current chess match.
 *
 * GameMode is declared separately in GameMode.kt.
 */
data class GameSettings(
    val mode: GameMode = GameMode.PLAYER_VS_COMPUTER,
    val playerColor: PlayerColor = PlayerColor.WHITE,
    val difficulty: AIDifficulty = AIDifficulty.MEDIUM
) {

    /**
     * Returns true when the computer controls the current side.
     */
    fun isComputerTurn(side: String): Boolean {
        if (mode != GameMode.PLAYER_VS_COMPUTER) {
            return false
        }

        return when (playerColor) {
            PlayerColor.WHITE ->
                side.equals("Black", ignoreCase = true)

            PlayerColor.BLACK ->
                side.equals("White", ignoreCase = true)
        }
    }
}