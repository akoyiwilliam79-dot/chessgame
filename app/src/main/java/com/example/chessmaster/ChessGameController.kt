package com.example.chessmaster

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

/**

 * Stage 4: chess rules controller.
 *
 * Chesslib handles legal moves and the rules of chess.
 * The 3D activity will use this controller to select pieces,
 * display legal destinations, and play moves.
 */
class ChessGameController {

    private val board = Board()

    /**

     * Returns the current position in FEN format.
     */
    fun getFen(): String = board.fen

    /**

     * Returns whose turn it is.
     */
    fun getTurn(): String =
        if (board.sideToMove == Side.WHITE) "White" else "Black"

    /**

     * Returns all legal moves available in the current position.
     */
    fun getLegalMoves(): List<Move> =
        board.legalMoves().toList()

    /**

     * Returns legal moves from a selected square.
     */
    fun getLegalMovesFrom(square: Square): List<Move> =
        getLegalMoves().filter { it.from == square }

    /**

     * Checks whether a move is legal.
     */
    fun isLegalMove(from: Square, to: Square): Boolean =
        getLegalMoves().any {
            it.from == from && it.to == to
        }

    /**

     * Plays a legal move.
     *
     * Returns true if the move was played successfully.
     * Promotion is handled separately in a later stage.
     */
    fun playMove(from: Square, to: Square): Boolean {
        val move = getLegalMoves().firstOrNull {
            it.from == from && it.to == to
        } ?: return false

        board.doMove(move)
        return true
    }

    /**

     * Detects checkmate.
     */
    fun isCheckmate(): Boolean = board.isMated()

    /**

     * Detects stalemate.
     */
    fun isStalemate(): Boolean = board.isStaleMate()

    /**

     * Detects whether the side to move is in check.
     */
    fun isInCheck(): Boolean = board.isKingAttacked()

    /**

     * Returns a status suitable for the game interface.
     */
    fun getStatus(): String {
        return when {
            isCheckmate() -> "CHECKMATE"
            isStalemate() -> "STALEMATE"
            isInCheck() -> "${getTurn().uppercase()} IN CHECK"
            else -> "${getTurn().uppercase()} TO MOVE"
        }
    }

    /**

     * Restarts the game from the standard starting position.
     */
    fun restart() {
        board.loadFromFen(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        )
    }


    /**

     * Converts a 3D board coordinate into a chess square.
     *
     * file: 0 = a, 1 = b ... 7 = h
     * rank: 0 = rank 1 ... 7 = rank 8
     */
    fun squareAt(file: Int, rank: Int): Square {
        require(file in 0..7) { "File must be between 0 and 7." }
        require(rank in 0..7) { "Rank must be between 0 and 7." }

        val fileLetter = ('A'.code + file).toChar()
        return Square.valueOf("$fileLetter${rank + 1}")
    }

    /**

     * Returns the file coordinate of a chess square.
     */
    fun fileOf(square: Square): Int =
        square.name[0] - 'A'

    /**

     * Returns the rank coordinate of a chess square.
     */
    fun rankOf(square: Square): Int =
        square.name[1].digitToInt() - 1

    /**

     * Whether the game has ended.
     */
    fun isGameOver(): Boolean =
        isCheckmate() || isStalemate()

    /**

     * Starts a new game.
     */
    fun newGame() {
        restart()
    }
}
