
package com.example.chessmaster

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var chessBoard: GridLayout
    private lateinit var tvGameStatus: TextView
    private lateinit var btnUndo: Button
    private lateinit var btnNewGame: Button

    private val board = Array(8) { Array(8) { "" } }
    private val squares = Array(8) { arrayOfNulls<TextView>(8) }

    private var selectedRow = -1
    private var selectedCol = -1
    private var whiteTurn = true

    private val legalMoves = mutableSetOf<Pair<Int, Int>>()

    private var lastFromRow = -1
    private var lastFromCol = -1
    private var lastToRow = -1
    private var lastToCol = -1

    private class GameSnapshot(
        val position: Array<Array<String>>,
        val whiteTurn: Boolean,
        val lastFromRow: Int,
        val lastFromCol: Int,
        val lastToRow: Int,
        val lastToCol: Int
    )

    private val moveHistory = mutableListOf<GameSnapshot>()

    private val whitePieces = mapOf(
        "K" to "\u2654",
        "Q" to "\u2655",
        "R" to "\u2656",
        "B" to "\u2657",
        "N" to "\u2658",
        "P" to "\u2659"
    )

    private val blackPieces = mapOf(
        "k" to "\u265A",
        "q" to "\u265B",
        "r" to "\u265C",
        "b" to "\u265D",
        "n" to "\u265E",
        "p" to "\u265F"
    )

    private val lightSquareColor by lazy {
        ContextCompat.getColor(this, R.color.chess_light_square)
    }

    private val darkSquareColor by lazy {
        ContextCompat.getColor(this, R.color.chess_dark_square)
    }

    private val selectedSquareColor by lazy {
        ContextCompat.getColor(this, R.color.chess_selected_square)
    }

    private val lastMoveColor by lazy {
        ContextCompat.getColor(this, R.color.chess_last_move)
    }

    private val legalMoveColor by lazy {
        ContextCompat.getColor(this, R.color.chess_legal_move)
    }

    private val whitePieceColor by lazy {
        ContextCompat.getColor(this, R.color.chess_white_piece)
    }

    private val blackPieceColor by lazy {
        ContextCompat.getColor(this, R.color.chess_black_piece)
    }

    private val statusColor by lazy {
        ContextCompat.getColor(this, R.color.chess_gold)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chessBoard = findViewById(R.id.chessBoard)
        tvGameStatus = findViewById(R.id.tvGameStatus)
        btnUndo = findViewById(R.id.btnUndo)
        btnNewGame = findViewById(R.id.btnNewGame)

        setupBoard()
        initializePieces()
        renderBoard()

        btnNewGame.setOnClickListener {
            startNewGame()
        }

        btnUndo.setOnClickListener {
            undoMove()
        }
    }

    // --------------------------------------------------
    // BOARD SETUP
    // --------------------------------------------------

    private fun setupBoard() {
        chessBoard.removeAllViews()
        chessBoard.columnCount = 8
        chessBoard.rowCount = 8

        for (row in 0..7) {
            for (col in 0..7) {

                val square = TextView(this).apply {
                    gravity = Gravity.CENTER
                    textSize = 30f
                    includeFontPadding = false
                    setPadding(0, 0, 0, 0)
                    isClickable = true
                    isFocusable = true

                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        rowSpec = GridLayout.spec(row, 1f)
                        columnSpec = GridLayout.spec(col, 1f)
                        setMargins(0, 0, 0, 0)
                    }

                    setOnClickListener {
                        handleSquareClick(row, col)
                    }
                }

                squares[row][col] = square
                chessBoard.addView(square)
            }
        }

        chessBoard.post {
            val availableWidth = chessBoard.width -
                    chessBoard.paddingLeft -
                    chessBoard.paddingRight

            if (availableWidth <= 0) return@post

            val cellSize = availableWidth / 8
            val boardSize = cellSize * 8

            chessBoard.layoutParams =
                chessBoard.layoutParams.apply {
                    height = boardSize
                }

            for (row in 0..7) {
                for (col in 0..7) {
                    val square = squares[row][col] ?: continue

                    val params =
                        square.layoutParams as GridLayout.LayoutParams

                    params.width = cellSize
                    params.height = cellSize
                    square.layoutParams = params
                }
            }

            chessBoard.requestLayout()
            renderBoard()
        }
    }

    // --------------------------------------------------
    // INITIALIZE PIECES
    // --------------------------------------------------

    private fun initializePieces() {
        for (row in 0..7) {
            for (col in 0..7) {
                board[row][col] = ""
            }
        }

        val backRank = arrayOf(
            "r", "n", "b", "q", "k", "b", "n", "r"
        )

        for (col in 0..7) {
            board[0][col] = backRank[col]
            board[1][col] = "p"
            board[6][col] = "P"
            board[7][col] = backRank[col].uppercase()
        }

        whiteTurn = true

        clearSelection()

        lastFromRow = -1
        lastFromCol = -1
        lastToRow = -1
        lastToCol = -1

        moveHistory.clear()
    }

    // --------------------------------------------------
    // RENDER BOARD
    // --------------------------------------------------

    private fun renderBoard() {
        for (row in 0..7) {
            for (col in 0..7) {

                val square = squares[row][col] ?: continue
                val piece = board[row][col]

                val baseColor = if ((row + col) % 2 == 0) {
                    lightSquareColor
                } else {
                    darkSquareColor
                }

                square.setBackgroundColor(baseColor)

                // Highlight the last move.
                if (
                    (row == lastFromRow && col == lastFromCol) ||
                    (row == lastToRow && col == lastToCol)
                ) {
                    square.setBackgroundColor(lastMoveColor)
                }

                // Highlight legal destinations.
                if ((row to col) in legalMoves) {
                    square.setBackgroundColor(legalMoveColor)
                }

                // Highlight the selected piece.
                if (row == selectedRow && col == selectedCol) {
                    square.setBackgroundColor(selectedSquareColor)
                }

                square.text = when {
                    whitePieces.containsKey(piece) -> whitePieces[piece]
                    blackPieces.containsKey(piece) -> blackPieces[piece]
                    else -> ""
                }

                if (piece.isNotEmpty()) {
                    val isWhitePiece = piece[0].isUpperCase()

                    square.setTextColor(
                        if (isWhitePiece) whitePieceColor
                        else blackPieceColor
                    )

                    square.setShadowLayer(
                        1.5f,
                        0f,
                        1f,
                        if (isWhitePiece) Color.BLACK else Color.WHITE
                    )

                    square.textSize = 30f
                } else {
                    square.setTextColor(Color.BLACK)
                    square.setShadowLayer(
                        0f,
                        0f,
                        0f,
                        Color.TRANSPARENT
                    )
                }

                square.contentDescription = when {
                    piece.isEmpty() ->
                        "Empty square, row ${row + 1}, column ${col + 1}"

                    piece[0].isUpperCase() ->
                        "White piece $piece, row ${row + 1}, column ${col + 1}"

                    else ->
                        "Black piece $piece, row ${row + 1}, column ${col + 1}"
                }
            }
        }

        tvGameStatus.text = if (whiteTurn) {
            "♔  WHITE'S TURN"
        } else {
            "♚  BLACK'S TURN"
        }

        tvGameStatus.setTextColor(statusColor)
        tvGameStatus.textSize = 18f
        tvGameStatus.gravity = Gravity.CENTER

        btnUndo.isEnabled = moveHistory.isNotEmpty()
        btnUndo.alpha = if (moveHistory.isNotEmpty()) 1f else 0.6f
    }

    // --------------------------------------------------
    // PLAYER INPUT
    // --------------------------------------------------

    private fun handleSquareClick(row: Int, col: Int) {
        val clickedPiece = board[row][col]

        // Select a piece.
        if (selectedRow == -1) {
            if (clickedPiece.isEmpty()) return

            if (!belongsToCurrentPlayer(clickedPiece)) {
                showMessage("Select your own piece.")
                return
            }

            selectPiece(row, col)
            return
        }

        // Deselect the currently selected piece.
        if (row == selectedRow && col == selectedCol) {
            clearSelection()
            renderBoard()
            return
        }

        // Select a different piece belonging to the current player.
        if (
            clickedPiece.isNotEmpty() &&
            belongsToCurrentPlayer(clickedPiece)
        ) {
            selectPiece(row, col)
            return
        }

        val fromRow = selectedRow
        val fromCol = selectedCol
        val piece = board[fromRow][fromCol]

        if ((row to col) !in legalMoves) {
            showMessage("That is not a legal move for this piece.")
            return
        }

        saveSnapshot()

        board[row][col] = piece
        board[fromRow][fromCol] = ""

        // Automatic pawn promotion to queen.
        if (piece == "P" && row == 0) {
            board[row][col] = "Q"
            showMessage("White pawn promoted to queen!")
        } else if (piece == "p" && row == 7) {
            board[row][col] = "q"
            showMessage("Black pawn promoted to queen!")
        }

        lastFromRow = fromRow
        lastFromCol = fromCol
        lastToRow = row
        lastToCol = col

        clearSelection()
        whiteTurn = !whiteTurn

        renderBoard()
    }

    private fun selectPiece(row: Int, col: Int) {
        selectedRow = row
        selectedCol = col

        legalMoves.clear()

        for (targetRow in 0..7) {
            for (targetCol in 0..7) {
                if (
                    isLegalPieceMove(
                        row,
                        col,
                        targetRow,
                        targetCol
                    )
                ) {
                    legalMoves.add(targetRow to targetCol)
                }
            }
        }

        renderBoard()
    }

    private fun clearSelection() {
        selectedRow = -1
        selectedCol = -1
        legalMoves.clear()
    }

    private fun belongsToCurrentPlayer(piece: String): Boolean {
        return piece.isNotEmpty() &&
                piece[0].isUpperCase() == whiteTurn
    }

    // --------------------------------------------------
    // MOVEMENT RULES
    // --------------------------------------------------

    private fun isLegalPieceMove(
        fromRow: Int,
        fromCol: Int,
        toRow: Int,
        toCol: Int
    ): Boolean {

        if (
            !isInsideBoard(fromRow, fromCol) ||
            !isInsideBoard(toRow, toCol)
        ) {
            return false
        }

        if (fromRow == toRow && fromCol == toCol) {
            return false
        }

        val piece = board[fromRow][fromCol]
        val target = board[toRow][toCol]

        if (piece.isEmpty()) return false

        // Cannot capture your own piece.
        if (
            target.isNotEmpty() &&
            piece[0].isUpperCase() == target[0].isUpperCase()
        ) {
            return false
        }

        val rowDiff = toRow - fromRow
        val colDiff = toCol - fromCol

        return when (piece.uppercase()) {

            "P" -> isLegalPawnMove(
                piece,
                fromRow,
                fromCol,
                toRow,
                toCol
            )

            "N" ->
                (abs(rowDiff) == 2 && abs(colDiff) == 1) ||
                        (abs(rowDiff) == 1 && abs(colDiff) == 2)

            "B" ->
                abs(rowDiff) == abs(colDiff) &&
                        isPathClear(fromRow, fromCol, toRow, toCol)

            "R" ->
                (rowDiff == 0 || colDiff == 0) &&
                        isPathClear(fromRow, fromCol, toRow, toCol)

            "Q" ->
                (
                        rowDiff == 0 ||
                                colDiff == 0 ||
                                abs(rowDiff) == abs(colDiff)
                        ) && isPathClear(fromRow, fromCol, toRow, toCol)

            "K" ->
                abs(rowDiff) <= 1 && abs(colDiff) <= 1

            else -> false
        }
    }

    private fun isLegalPawnMove(
        piece: String,
        fromRow: Int,
        fromCol: Int,
        toRow: Int,
        toCol: Int
    ): Boolean {

        val direction = if (piece == "P") -1 else 1
        val startingRow = if (piece == "P") 6 else 1

        val rowDiff = toRow - fromRow
        val colDiff = toCol - fromCol
        val target = board[toRow][toCol]

        // Move one square forward.
        if (
            colDiff == 0 &&
            rowDiff == direction &&
            target.isEmpty()
        ) {
            return true
        }

        // Move two squares from the starting position.
        if (
            colDiff == 0 &&
            fromRow == startingRow &&
            rowDiff == 2 * direction &&
            target.isEmpty()
        ) {
            return board[fromRow + direction][fromCol].isEmpty()
        }

        // Capture diagonally.
        if (
            abs(colDiff) == 1 &&
            rowDiff == direction &&
            target.isNotEmpty() &&
            target[0].isUpperCase() != piece[0].isUpperCase()
        ) {
            return true
        }

        return false
    }

    private fun isPathClear(
        fromRow: Int,
        fromCol: Int,
        toRow: Int,
        toCol: Int
    ): Boolean {

        val rowStep = when {
            toRow > fromRow -> 1
            toRow < fromRow -> -1
            else -> 0
        }

        val colStep = when {
            toCol > fromCol -> 1
            toCol < fromCol -> -1
            else -> 0
        }

        var row = fromRow + rowStep
        var col = fromCol + colStep

        while (row != toRow || col != toCol) {
            if (board[row][col].isNotEmpty()) {
                return false
            }

            row += rowStep
            col += colStep
        }

        return true
    }

    private fun isInsideBoard(row: Int, col: Int): Boolean {
        return row in 0..7 && col in 0..7
    }

    // --------------------------------------------------
    // UNDO
    // --------------------------------------------------

    private fun saveSnapshot() {
        val savedBoard = Array(8) { row ->
            board[row].clone()
        }

        moveHistory.add(
            GameSnapshot(
                position = savedBoard,
                whiteTurn = whiteTurn,
                lastFromRow = lastFromRow,
                lastFromCol = lastFromCol,
                lastToRow = lastToRow,
                lastToCol = lastToCol
            )
        )
    }

    private fun undoMove() {
        if (moveHistory.isEmpty()) {
            showMessage("There are no moves to undo.")
            return
        }

        val previous = moveHistory.removeAt(moveHistory.lastIndex)

        for (row in 0..7) {
            for (col in 0..7) {
                board[row][col] = previous.position[row][col]
            }
        }

        whiteTurn = previous.whiteTurn

        lastFromRow = previous.lastFromRow
        lastFromCol = previous.lastFromCol
        lastToRow = previous.lastToRow
        lastToCol = previous.lastToCol

        clearSelection()
        renderBoard()
    }

    // --------------------------------------------------
    // NEW GAME
    // --------------------------------------------------

    private fun startNewGame() {
        initializePieces()
        renderBoard()
        showMessage("New match started!")
    }

    // --------------------------------------------------
    // MESSAGES
    // --------------------------------------------------

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}

