package com.example.chessmaster

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator

class ChessGameManager {

    private var board = Board()

    // Store previous positions so moves can be undone.
    private val history = mutableListOf<String>()

    fun newGame() {
        board = Board()
        history.clear()
    }

    fun getBoard(): Board = board

    fun getFen(): String = board.fen

    fun isWhiteTurn(): Boolean {
        return board.sideToMove == Side.WHITE
    }

    fun getLegalMoves(): List<Move> {
        return MoveGenerator.generateLegalMoves(board)
    }

    fun getLegalMovesFrom(squareName: String): List<Move> {
        val square = parseSquare(squareName) ?: return emptyList()

        return getLegalMoves().filter { move ->
            move.from == square
        }
    }

    fun makeMove(from: String, to: String): Boolean {
        val fromSquare = parseSquare(from) ?: return false
        val toSquare = parseSquare(to) ?: return false

        if (isGameOver()) return false

        val matchingMoves = getLegalMoves().filter { move ->
            move.from == fromSquare && move.to == toSquare
        }

        if (matchingMoves.isEmpty()) return false

        // Automatically promote a pawn to a queen when possible.
        val selectedMove = matchingMoves.firstOrNull {
            it.promotion.toString().equals("QUEEN", ignoreCase = true)
        } ?: matchingMoves.first()

        history.add(board.fen)
        board.doMove(selectedMove)

        return true
    }

    fun undoMove(): Boolean {
        if (history.isEmpty()) return false

        val previousPosition = history.removeAt(history.lastIndex)
        board.loadFromFen(previousPosition)

        return true
    }

    fun isCheck(): Boolean = board.isKingAttacked()

    fun isCheckmate(): Boolean = board.isMated()

    fun isStalemate(): Boolean = board.isStaleMate()

    fun isDraw(): Boolean = board.isDraw()

    fun isGameOver(): Boolean {
        return isCheckmate() || isStalemate() || isDraw()
    }

    fun getGameStatus(): String {
        return when {
            isCheckmate() && isWhiteTurn() ->
                "CHECKMATE — BLACK WINS!"

            isCheckmate() ->
                "CHECKMATE — WHITE WINS!"

            isStalemate() ->
                "DRAW — STALEMATE"

            isDraw() ->
                "DRAW"

            isCheck() && isWhiteTurn() ->
                "WHITE IS IN CHECK!"

            isCheck() ->
                "BLACK IS IN CHECK!"

            isWhiteTurn() ->
                "WHITE TO MOVE"

            else ->
                "BLACK TO MOVE"
        }
    }

    private fun parseSquare(name: String): Square? {
        return try {
            Square.valueOf(name.uppercase())
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
