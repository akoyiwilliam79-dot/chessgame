package com.example.chessmaster


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.bhlangonijr.chesslib.Square
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener

private val BoardLight = Color(0xFFE6D4B5)
private val BoardDark = Color(0xFF345548)
private val AppBackground = Color(0xFF0A0F15)
private val Gold = Color(0xFFE5BE69)
private val Panel = Color(0xFF151E28)
private val Muted = Color(0xFF9BA9B8)
private val ErrorRed = Color(0xFFFF8C85)
private val MoveHighlight = Color(0xAAE5BE69)
private val SelectedHighlight = Color(0xAA55C7A2)

private const val BOARD_TOP_Y = 0.06f
private const val BOARD_THICKNESS = 0.12f

class Chess3DActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Chess3DScreen()
            }
        }
    }
}

private data class ChessPiece(
    val name: String,
    val side: String,
    val file: Int,
    val rank: Int,
    val modelFile: String,
    val isPawn: Boolean = false
)

private data class PieceVisual(
    val side: String,
    val node: ModelNode,
    val isPawn: Boolean
)

private fun modelFileFor(piece: String, side: String): String {
    return when (piece.lowercase()) {
        "king" ->
            if (side == "White") "01_king_light.glb"
            else "02_king_dark.glb"

        "queen" ->
            if (side == "White") "03_queen_light.glb"
            else "04_queen_dark.glb"

        "bishop" ->
            if (side == "White") "05_bishop_light.glb"
            else "06_bishop_dark.glb"

        "knight" ->
            if (side == "White") "07_knight_light.glb"
            else "08_knight_dark.glb"

        "rook" ->
            if (side == "White") "09_rook_light.glb"
            else "10_rook_dark.glb"

        "pawn" ->
            if (side == "White") "11_pawn_light.glb"
            else "12_pawn_dark.glb"

        else -> error("Unknown chess piece: $piece")
    }
}

private fun createStartingPieces(): List<ChessPiece> {
    val pieces = mutableListOf<ChessPiece>()

    val backRank = listOf(
        "rook", "knight", "bishop", "queen",
        "king", "bishop", "knight", "rook"
    )

    for (file in 0..7) {
        val name = backRank[file]

        pieces.add(
            ChessPiece(
                name = name,
                side = "White",
                file = file,
                rank = 0,
                modelFile = modelFileFor(name, "White")
            )
        )

        pieces.add(
            ChessPiece(
                name = "pawn",
                side = "White",
                file = file,
                rank = 1,
                modelFile = modelFileFor("pawn", "White"),
                isPawn = true
            )
        )

        pieces.add(
            ChessPiece(
                name = name,
                side = "Black",
                file = file,
                rank = 7,
                modelFile = modelFileFor(name, "Black")
            )
        )

        pieces.add(
            ChessPiece(
                name = "pawn",
                side = "Black",
                file = file,
                rank = 6,
                modelFile = modelFileFor("pawn", "Black"),
                isPawn = true
            )
        )
    }

    return pieces
}

@Composable
private fun Chess3DScreen() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val modelLoader = rememberModelLoader(engine)
    val nodes = rememberNodes()
    val controller = remember { ChessGameController() }

    val squareNodes = remember { mutableMapOf<Square, CubeNode>() }
    val pieceNodes = remember { mutableMapOf<Square, PieceVisual>() }

    val highlightNodes = remember { mutableListOf<CubeNode>() }
    val highlightSquares = remember { mutableMapOf<CubeNode, Square>() }

    var selectedSquare by remember { mutableStateOf<Square?>(null) }
    var legalTargets by remember { mutableStateOf<Set<Square>>(emptySet()) }

    var turn by remember { mutableStateOf("White") }
    var gameStatus by remember { mutableStateOf("WHITE TO MOVE") }
    var message by remember {
        mutableStateOf("Select a piece to see its legal moves.")
    }

    var whiteLoaded by remember { mutableIntStateOf(0) }
    var blackLoaded by remember { mutableIntStateOf(0) }
    var moveCount by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf("") }

    fun clearHighlights() {
        highlightNodes.forEach { nodes.remove(it) }
        highlightNodes.clear()
        highlightSquares.clear()
    }

    fun addHighlight(square: Square, color: Color) {
        val file = controller.fileOf(square)
        val rank = controller.rankOf(square)

        val highlight = CubeNode(
            engine = engine,
            size = Size(0.88f, 0.018f, 0.88f),
            center = Position(
                x = file - 3.5f,
                y = BOARD_TOP_Y + 0.015f,
                z = rank - 3.5f
            ),
            materialInstance = materialLoader.createColorInstance(color)
        )

        nodes.add(highlight)
        highlightNodes.add(highlight)
        highlightSquares[highlight] = square
    }

    fun refreshStatus() {
        turn = controller.getTurn()

        gameStatus = when {
            controller.isCheckmate() -> "CHECKMATE"
            controller.isStalemate() -> "STALEMATE"
            controller.isInCheck() -> "${turn.uppercase()} IN CHECK"
            else -> "${turn.uppercase()} TO MOVE"
        }
    }

    /*
     * FIX:
     * Always use the chess engine's current side to move and current square.
     * Clear old targets before creating the new destination highlights.
     */
    fun selectPiece(square: Square) {
        val visual = pieceNodes[square]

        if (visual == null) {
            message = "No piece found on ${square.name}."
            return
        }

        val currentTurn = controller.getTurn()

        if (visual.side != currentTurn) {
            message = "It's $currentTurn's turn."
            return
        }

        selectedSquare = square

        legalTargets = controller.getLegalMovesFrom(square)
            .map { it.to }
            .toSet()

        clearHighlights()

        addHighlight(square, SelectedHighlight)

        legalTargets.forEach { destination ->
            addHighlight(destination, MoveHighlight)
        }

        message = if (legalTargets.isEmpty()) {
            "This piece has no legal moves."
        } else {
            "Selected ${square.name}. Tap a highlighted destination."
        }
    }

    fun executeMove(from: Square, to: Square) {
        val visual = pieceNodes[from]

        if (visual == null) {
            message = "Could not find the selected piece."
            return
        }

        /*
         * Recheck legality directly against the controller.
         * This prevents stale highlight information from allowing a move.
         */
        val currentlyLegal = controller.getLegalMovesFrom(from)
            .any { it.to == to }

        if (!currentlyLegal) {
            message = "That destination is not a legal move."
            return
        }

        val captured = pieceNodes[to]
        val previousTurn = controller.getTurn()

        if (!controller.playMove(from, to)) {
            message = "The chess rules rejected that move."
            return
        }

        // Remove the captured model from the 3D scene.
        if (captured != null) {
            nodes.remove(captured.node)
            pieceNodes.remove(to)
        }

        // Update the model's world position.
        visual.node.position = Position(
            x = controller.fileOf(to) - 3.5f,
            y = BOARD_TOP_Y + if (visual.isPawn) 0.39f else 0.54f,
            z = controller.rankOf(to) - 3.5f
        )

        /*
         * FIX:
         * Move the visual mapping from its old square to its new square.
         * This is important when the same piece is selected again later.
         */
        pieceNodes.remove(from)
        pieceNodes[to] = visual

        selectedSquare = null
        legalTargets = emptySet()
        clearHighlights()

        moveCount++

        refreshStatus()

        message = when {
            gameStatus == "CHECKMATE" ->
                "Checkmate! $previousTurn wins."

            gameStatus == "STALEMATE" ->
                "Stalemate. The game is a draw."

            gameStatus.endsWith("IN CHECK") ->
                "$turn is in check."

            captured != null ->
                "Capture completed. $turn to move."

            else ->
                "Move played. $turn to move."
        }
    }

    fun handleSquareTap(square: Square) {
        if (loading) return

        if (controller.isGameOver()) {
            message = "Game over. Press Restart to play again."
            return
        }

        val currentSelection = selectedSquare

        if (currentSelection != null && square in legalTargets) {
            executeMove(currentSelection, square)
            return
        }

        val visual = pieceNodes[square]

        if (visual != null && visual.side == controller.getTurn()) {
            selectPiece(square)
        } else {
            message = if (currentSelection != null) {
                "Choose a highlighted destination."
            } else {
                "Select a ${controller.getTurn()} piece."
            }
        }
    }

    /*
     * FIX:
     * Resolve taps by the actual node object and its CURRENT square mapping.
     * Destination highlights are checked before pieces and board squares.
     */
    fun handleNodeTap(node: Any?) {
        if (node == null || loading) return

        val highlightedSquare = highlightSquares.entries
            .firstOrNull { it.key === node }
            ?.value

        if (highlightedSquare != null) {
            handleSquareTap(highlightedSquare)
            return
        }

        val tappedPieceEntry = pieceNodes.entries
            .firstOrNull { it.value.node === node }

        if (tappedPieceEntry != null) {
            val tappedSquare = tappedPieceEntry.key
            val tappedPiece = tappedPieceEntry.value

            if (selectedSquare != null && tappedSquare in legalTargets) {
                executeMove(selectedSquare!!, tappedSquare)
                return
            }

            if (tappedPiece.side == controller.getTurn()) {
                selectPiece(tappedSquare)
            } else {
                message = if (selectedSquare != null) {
                    "Choose a highlighted destination."
                } else {
                    "It's ${controller.getTurn()}'s turn."
                }
            }

            return
        }

        val tappedSquare = squareNodes.entries
            .firstOrNull { it.value === node }
            ?.key

        if (tappedSquare != null) {
            handleSquareTap(tappedSquare)
        }
    }

    fun restartGame() {
        controller.restart()

        selectedSquare = null
        legalTargets = emptySet()
        moveCount = 0

        clearHighlights()

        pieceNodes.values.forEach { nodes.remove(it.node) }
        pieceNodes.clear()

        refreshStatus()

        loading = true
        message = "Restarting. White to move."
    }

    LaunchedEffect(engine, modelLoader, materialLoader, loading) {
        if (!loading) return@LaunchedEffect

        loadError = ""
        whiteLoaded = 0
        blackLoaded = 0

        if (squareNodes.isEmpty()) {
            for (rank in 0..7) {
                for (file in 0..7) {
                    val square = controller.squareAt(file, rank)

                    val color =
                        if ((file + rank) % 2 == 0) BoardLight
                        else BoardDark

                    val node = CubeNode(
                        engine = engine,
                        size = Size(1f, BOARD_THICKNESS, 1f),
                        center = Position(
                            x = file - 3.5f,
                            y = 0f,
                            z = rank - 3.5f
                        ),
                        materialInstance =
                            materialLoader.createColorInstance(color)
                    )

                    squareNodes[square] = node
                    nodes.add(node)
                }
            }
        }

        for (piece in createStartingPieces()) {
            val square = controller.squareAt(piece.file, piece.rank)

            if (pieceNodes.containsKey(square)) continue

            try {
                val instance = modelLoader.loadModelInstance(
                    fileLocation = "models/${piece.modelFile}"
                )

                if (instance == null) {
                    loadError += "Could not load ${piece.modelFile}\n"
                    continue
                }

                val size = if (piece.isPawn) 0.78f else 1.08f

                val modelNode = ModelNode(
                    modelInstance = instance,
                    scaleToUnits = size
                )

                modelNode.position = Position(
                    x = piece.file - 3.5f,
                    y = BOARD_TOP_Y + size * 0.5f,
                    z = piece.rank - 3.5f
                )

                nodes.add(modelNode)

                pieceNodes[square] = PieceVisual(
                    side = piece.side,
                    node = modelNode,
                    isPawn = piece.isPawn
                )

                if (piece.side == "White") {
                    whiteLoaded++
                } else {
                    blackLoaded++
                }
            } catch (exception: Exception) {
                loadError +=
                    "${piece.modelFile}: " +
                            "${exception.message ?: "Loading failed"}\n"
            }
        }

        refreshStatus()
        loading = false
    }

    val cameraManipulator = rememberCameraManipulator(
        orbitHomePosition = Position(0f, 9f, 10f),
        targetPosition = Position(0f, 0f, 0f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "CHESSMASTER",
                color = Gold,
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "THE 3D CHESS EXPERIENCE",
                color = Muted,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CLASSIC MATCH",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Surface(
                    color = Panel,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = gameStatus,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Scene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                childNodes = nodes,
                cameraManipulator = cameraManipulator,
                onGestureListener = rememberOnGestureListener(
                    onSingleTapConfirmed = { _, node ->
                        handleNodeTap(node)
                    }
                )
            )

            if (loading) {
                Surface(
                    color = Panel,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Preparing 3D board...",
                        color = Gold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(
                            horizontal = 18.dp,
                            vertical = 12.dp
                        )
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Panel)
                .padding(16.dp)
        ) {
            Text(
                text = "LIVE MATCH",
                color = Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = message,
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusCounter(
                    label = "WHITE",
                    value = "$whiteLoaded / 16",
                    valueColor = Color.White
                )

                StatusCounter(
                    label = "BLACK",
                    value = "$blackLoaded / 16",
                    valueColor = Gold
                )

                StatusCounter(
                    label = "MOVES",
                    value = moveCount.toString(),
                    valueColor = Color(0xFF83D6A0)
                )
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = { restartGame() },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = AppBackground
                )
            ) {
                Text(
                    text = "RESTART GAME",
                    fontWeight = FontWeight.Bold
                )
            }

            if (loadError.isNotBlank()) {
                Spacer(Modifier.height(10.dp))

                Text(
                    text = loadError,
                    color = ErrorRed,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun StatusCounter(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            color = Muted,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = value,
            color = valueColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

