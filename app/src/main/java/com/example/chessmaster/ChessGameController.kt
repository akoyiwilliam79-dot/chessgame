package com.example.chessmaster

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

enum class MatchOutcome {
    IN_PROGRESS,
    WHITE_WINS,
    BLACK_WINS,
    DRAW
}

data class MatchResult(
    val outcome: MatchOutcome,
    val title: String,
    val score: String,
    val description: String
)

data class RecordedMove(
    val number: Int,
    val player: String,
    val from: String,
    val to: String
) {
    fun display(): String = "$number. $player: $from → $to"
}

class ChessGameController {

    private val board = Board()

    /*
     * Position history:
     * positions[0] is the initial position.
     * Every successfully played move adds one position.
     */
    private val positions = mutableListOf<String>()

    private val moveHistory = mutableListOf<RecordedMove>()

    /*
     * Exact moves corresponding to moveHistory.
     * This preserves promotion information.
     */
    private val playedMoves = mutableListOf<Move>()

    /*
     * The last move undone is the next move available for redo.
     */
    private val redoHistory = mutableListOf<Move>()

    private var resignedBy: Side? = null
    private var agreedDraw = false
    private var drawOfferedBy: Side? = null

    init {
        positions.add(board.fen)
    }

    fun getFen(): String = board.fen

    fun getTurn(): String =
        if (board.sideToMove == Side.WHITE) "White" else "Black"

    fun getSideToMove(): Side = board.sideToMove

    fun getLegalMoves(): List<Move> =
        if (isGameOver()) emptyList()
        else board.legalMoves().toList()

    fun getLegalMovesFrom(square: Square): List<Move> =
        getLegalMoves().filter { it.from == square }

    fun isLegalMove(from: Square, to: Square): Boolean =
        getLegalMoves().any {
            it.from == from && it.to == to
        }

    fun findLegalMove(from: Square, to: Square): Move? =
        getLegalMoves().firstOrNull {
            it.from == from && it.to == to
        }

    fun playMove(from: Square, to: Square): Boolean {
        val move = findLegalMove(from, to) ?: return false
        return playMove(move)
    }

    fun playMove(move: Move): Boolean {
        if (isGameOver()) return false

        val legalMove = board.legalMoves().firstOrNull {
            it.from == move.from &&
                    it.to == move.to &&
                    it.promotion == move.promotion
        } ?: return false

        val movingSide = board.sideToMove

        if (!board.doMove(legalMove)) return false

        // A new move creates a new branch.
        redoHistory.clear()

        positions.add(board.fen)
        playedMoves.add(legalMove)

        moveHistory.add(
            RecordedMove(
                number = moveHistory.size / 2 + 1,
                player = if (movingSide == Side.WHITE) "White" else "Black",
                from = legalMove.from.name,
                to = legalMove.to.name
            )
        )

        drawOfferedBy = null

        return true
    }

    fun canUndo(): Boolean =
        positions.size > 1 &&
                playedMoves.isNotEmpty() &&
                moveHistory.isNotEmpty() &&
                resignedBy == null &&
                !agreedDraw

    fun undoMove(): Boolean {
        if (!canUndo()) return false

        val previousFen = positions[positions.lastIndex - 1]
        val undoneMove = playedMoves.last()

        /*
         * Restore the board before changing any history.
         * If loading fails, leave the histories untouched.
         */
        val restored = runCatching {
            board.loadFromFen(previousFen)
            board.fen == previousFen
        }.getOrDefault(false)

        if (!restored) return false

        positions.removeAt(positions.lastIndex)
        playedMoves.removeAt(playedMoves.lastIndex)
        moveHistory.removeAt(moveHistory.lastIndex)

        redoHistory.add(undoneMove)
        drawOfferedBy = null

        return true
    }

    fun canRedo(): Boolean =
        redoHistory.isNotEmpty() &&
                resignedBy == null &&
                !agreedDraw &&
                !board.isMated() &&
                !board.isStaleMate() &&
                !isInsufficientMaterial()

    fun redoMove(): Boolean {
        if (!canRedo()) return false

        val move = redoHistory.last()

        val legalMove = board.legalMoves().firstOrNull {
            it.from == move.from &&
                    it.to == move.to &&
                    it.promotion == move.promotion
        } ?: return false

        val movingSide = board.sideToMove

        if (!board.doMove(legalMove)) return false

        redoHistory.removeAt(redoHistory.lastIndex)

        positions.add(board.fen)
        playedMoves.add(legalMove)

        moveHistory.add(
            RecordedMove(
                number = moveHistory.size / 2 + 1,
                player = if (movingSide == Side.WHITE) "White" else "Black",
                from = legalMove.from.name,
                to = legalMove.to.name
            )
        )

        drawOfferedBy = null

        return true
    }

    fun getMoveHistory(): List<RecordedMove> =
        moveHistory.toList()

    fun getMoveCount(): Int = moveHistory.size

    fun offerDraw(): Boolean {
        if (isGameOver() || drawOfferedBy != null) return false

        drawOfferedBy = board.sideToMove
        return true
    }

    fun hasDrawOffer(): Boolean = drawOfferedBy != null

    fun getDrawOfferFrom(): String? =
        when (drawOfferedBy) {
            Side.WHITE -> "White"
            Side.BLACK -> "Black"
            null -> null
        }

    fun canAcceptDraw(): Boolean =
        !isGameOver() &&
                drawOfferedBy != null &&
                drawOfferedBy != board.sideToMove

    fun acceptDraw(): Boolean {
        if (!canAcceptDraw()) return false

        agreedDraw = true
        drawOfferedBy = null

        return true
    }

    fun declineDraw(): Boolean {
        if (drawOfferedBy == null) return false

        drawOfferedBy = null
        return true
    }

    fun resign(): Boolean {
        if (isGameOver()) return false

        resignedBy = board.sideToMove
        drawOfferedBy = null

        return true
    }

    fun isCheckmate(): Boolean = board.isMated()

    fun isStalemate(): Boolean = board.isStaleMate()

    fun isInCheck(): Boolean = board.isKingAttacked()

    private fun isInsufficientMaterial(): Boolean {
        val placement = board.fen.substringBefore(" ")
        val pieces = placement.filter { it.isLetter() }

        if (pieces.all { it == 'K' || it == 'k' }) {
            return true
        }

        if (
            pieces.length == 3 &&
            pieces.count { it == 'K' || it == 'k' } == 2 &&
            pieces.any {
                it == 'B' || it == 'b' ||
                        it == 'N' || it == 'n'
            }
        ) {
            return true
        }

        return false
    }

    fun isGameOver(): Boolean =
        board.isMated() ||
                board.isStaleMate() ||
                isInsufficientMaterial() ||
                resignedBy != null ||
                agreedDraw

    fun getResult(): MatchResult {
        return when {
            resignedBy != null -> {
                val winner =
                    if (resignedBy == Side.WHITE) "BLACK" else "WHITE"

                MatchResult(
                    outcome = if (winner == "WHITE") {
                        MatchOutcome.WHITE_WINS
                    } else {
                        MatchOutcome.BLACK_WINS
                    },
                    title = "$winner WINS!",
                    score = if (winner == "WHITE") "1 - 0" else "0 - 1",
                    description = "$winner wins by resignation."
                )
            }

            agreedDraw -> MatchResult(
                outcome = MatchOutcome.DRAW,
                title = "DRAW!",
                score = "½ - ½",
                description = "Both players agreed to a draw."
            )

            board.isMated() -> {
                val winner =
                    if (board.sideToMove == Side.WHITE) "BLACK" else "WHITE"

                MatchResult(
                    outcome = if (winner == "WHITE") {
                        MatchOutcome.WHITE_WINS
                    } else {
                        MatchOutcome.BLACK_WINS
                    },
                    title = "$winner WINS!",
                    score = if (winner == "WHITE") "1 - 0" else "0 - 1",
                    description = "CHECKMATE! $winner has won the match."
                )
            }

            board.isStaleMate() || isInsufficientMaterial() ->
                MatchResult(
                    outcome = MatchOutcome.DRAW,
                    title = "DRAW!",
                    score = "½ - ½",
                    description = if (isInsufficientMaterial()) {
                        "Draw by insufficient material."
                    } else {
                        "Draw by stalemate."
                    }
                )

            else -> MatchResult(
                outcome = MatchOutcome.IN_PROGRESS,
                title = getStatus(),
                score = "0 - 0",
                description = "${getTurn()} to move."
            )
        }
    }

    fun getStatus(): String =
        when {
            resignedBy != null -> "RESIGNATION"
            agreedDraw -> "DRAW AGREED"
            isCheckmate() -> "CHECKMATE"
            isStalemate() -> "STALEMATE"
            isInsufficientMaterial() -> "DRAW"
            isInCheck() -> "${getTurn().uppercase()} IN CHECK"
            else -> "${getTurn().uppercase()} TO MOVE"
        }

    fun restart() {
        board.loadFromFen(STARTING_FEN)

        positions.clear()
        positions.add(board.fen)

        moveHistory.clear()
        playedMoves.clear()
        redoHistory.clear()

        resignedBy = null
        agreedDraw = false
        drawOfferedBy = null
    }

    fun newGame() = restart()

    fun squareAt(file: Int, rank: Int): Square {
        require(file in 0..7) {
            "File must be between 0 and 7."
        }

        require(rank in 0..7) {
            "Rank must be between 0 and 7."
        }

        val fileLetter = ('A'.code + file).toChar()
        return Square.valueOf("$fileLetter${rank + 1}")
    }

    fun fileOf(square: Square): Int =
        square.name[0] - 'A'

    fun rankOf(square: Square): Int =
        square.name[1].digitToInt() - 1

    companion object {
        private const val STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    }
}

