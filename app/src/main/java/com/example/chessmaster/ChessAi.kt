
package com.example.chessmaster

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * ChessMaster computer opponent.
 *
 * EASY:
 *   Chooses among promising moves with a shallow search.
 *
 * MEDIUM:
 *   Searches several moves ahead using minimax and alpha-beta pruning.
 *
 * HARD:
 *   Searches deeper to find stronger tactical moves.
 *
 * The AI analyses a separate Board instance, so it does not
 * modify the player's live game while thinking.
 */
class ChessAI {

    private data class ScoredMove(
        val move: Move,
        val score: Int
    )

    /**
     * Returns the AI's selected legal move.
     *
     * Call this from a coroutine. The calculation runs on a
     * background dispatcher so the UI remains responsive.
     */
    suspend fun chooseMove(
        fen: String,
        difficulty: AIDifficulty
    ): Move? = withContext(Dispatchers.Default) {

        val board = Board()

        try {
            board.loadFromFen(fen)
        } catch (_: Exception) {
            return@withContext null
        }

        val legalMoves = board.legalMoves().toList()

        if (legalMoves.isEmpty()) {
            return@withContext null
        }

        val maximizingSide = board.sideToMove
        val depth = difficulty.searchDepth

        val scoredMoves = mutableListOf<ScoredMove>()

        for (move in legalMoves) {
            val previousFen = board.fen

            if (!board.doMove(move)) {
                continue
            }

            val score = minimax(
                board = board,
                depth = depth - 1,
                alpha = Int.MIN_VALUE + 1,
                beta = Int.MAX_VALUE,
                maximizingSide = maximizingSide
            )

            board.loadFromFen(previousFen)

            scoredMoves.add(
                ScoredMove(
                    move = move,
                    score = score
                )
            )
        }

        if (scoredMoves.isEmpty()) {
            return@withContext legalMoves.firstOrNull()
        }

        val bestScore = scoredMoves.maxOf { it.score }

        val bestMoves = scoredMoves.filter {
            it.score == bestScore
        }


        return@withContext when (difficulty) {
            AIDifficulty.EASY -> bestMoves.random().move

            AIDifficulty.MEDIUM,
            AIDifficulty.HARD -> bestMoves.first().move
        }


    }

    private fun minimax(
        board: Board,
        depth: Int,
        alpha: Int,
        beta: Int,
        maximizingSide: Side
    ): Int {

        if (board.isMated()) {
            return if (board.sideToMove == maximizingSide) {
                -MATE_SCORE - depth
            } else {
                MATE_SCORE + depth
            }
        }

        if (board.isStaleMate() || depth <= 0) {
            return evaluatePosition(board, maximizingSide)
        }

        val moves = board.legalMoves().toList()

        if (moves.isEmpty()) {
            return evaluatePosition(board, maximizingSide)
        }

        var a = alpha
        var b = beta

        val maximizing = board.sideToMove == maximizingSide

        if (maximizing) {
            var best = Int.MIN_VALUE + 1

            for (move in moves) {
                val previousFen = board.fen

                if (!board.doMove(move)) {
                    continue
                }

                val score = minimax(
                    board = board,
                    depth = depth - 1,
                    alpha = a,
                    beta = b,
                    maximizingSide = maximizingSide
                )

                board.loadFromFen(previousFen)

                best = max(best, score)
                a = max(a, best)

                if (b <= a) {
                    break
                }
            }

            return best
        } else {
            var best = Int.MAX_VALUE

            for (move in moves) {
                val previousFen = board.fen

                if (!board.doMove(move)) {
                    continue
                }

                val score = minimax(
                    board = board,
                    depth = depth - 1,
                    alpha = a,
                    beta = b,
                    maximizingSide = maximizingSide
                )

                board.loadFromFen(previousFen)

                best = min(best, score)
                b = min(b, best)

                if (b <= a) {
                    break
                }
            }

            return best
        }
    }

    /**
     * Evaluates the position using material balance.
     *
     * Positive scores favor the maximizing side.
     */
    private fun evaluatePosition(
        board: Board,
        maximizingSide: Side
    ): Int {

        val fen = board.fen
        val placement = fen.substringBefore(' ')
        var whiteScore = 0

        for (piece in placement) {
            whiteScore += when (piece) {
                'P' -> PAWN_VALUE
                'N' -> KNIGHT_VALUE
                'B' -> BISHOP_VALUE
                'R' -> ROOK_VALUE
                'Q' -> QUEEN_VALUE
                'K' -> 0

                'p' -> -PAWN_VALUE
                'n' -> -KNIGHT_VALUE
                'b' -> -BISHOP_VALUE
                'r' -> -ROOK_VALUE
                'q' -> -QUEEN_VALUE
                'k' -> 0

                else -> 0
            }
        }

        // A small bonus for having the move.
        val tempoBonus = if (board.sideToMove == Side.WHITE) 8 else -8

        whiteScore += tempoBonus

        return if (maximizingSide == Side.WHITE) {
            whiteScore
        } else {
            -whiteScore
        }
    }

    companion object {
        private const val PAWN_VALUE = 100
        private const val KNIGHT_VALUE = 320
        private const val BISHOP_VALUE = 330
        private const val ROOK_VALUE = 500
        private const val QUEEN_VALUE = 900

        private const val MATE_SCORE = 100_000
    }
}

