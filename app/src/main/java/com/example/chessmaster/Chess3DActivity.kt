
package com.example.chessmaster

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



// ============================================================
// CHESSMASTER DESIGN SYSTEM
// ============================================================

private val BoardLight = Color(0xFFE6D4B5)
private val BoardDark = Color(0xFF345548)

private val AppBackground = Color(0xFF111823)
private val Gold = Color(0xFFE5BE69)
private val Panel = Color(0xFF151E28)
private val Muted = Color(0xFF9BA9B8)
private val ErrorRed = Color(0xFFFF8C85)

private val MoveHighlight = Color(0xAAE5BE69)
private val SelectedHighlight = Color(0xAA55C7A2)
private val CheckHighlight = Color(0xAAFF5D62)

private val ButtonSurface = Color(0xFF22313F)
private val ButtonBorder = Color(0xFF34495A)
private val Green = Color(0xFF83D6A0)

private const val BOARD_TOP_Y = 0.06f
private const val BOARD_THICKNESS = 0.12f

// ============================================================
// ACTIVITY
// ============================================================

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

// ============================================================
// DATA MODELS
// ============================================================

private data class ChessPiece(
    val name: String,
    val side: String,
    val file: Int,
    val rank: Int,
    val modelFile: String,
    val isPawn: Boolean = false
)

private data class PieceVisual(
    val name: String,
    val side: String,
    val node: ModelNode,
    val isPawn: Boolean
)

private data class VisualSnapshot(
    val pieces: Map<Square, PieceVisual>,
    val positions: Map<Square, Position>,
    val whiteLoaded: Int,
    val blackLoaded: Int
)

// ============================================================
// PIECE MODELS
// ============================================================

private fun modelFileFor(
    piece: String,
    side: String
): String {
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

        pieces += ChessPiece(
            name = name,
            side = "White",
            file = file,
            rank = 0,
            modelFile = modelFileFor(name, "White")
        )

        pieces += ChessPiece(
            name = "pawn",
            side = "White",
            file = file,
            rank = 1,
            modelFile = modelFileFor("pawn", "White"),
            isPawn = true
        )

        pieces += ChessPiece(
            name = name,
            side = "Black",
            file = file,
            rank = 7,
            modelFile = modelFileFor(name, "Black")
        )

        pieces += ChessPiece(
            name = "pawn",
            side = "Black",
            file = file,
            rank = 6,
            modelFile = modelFileFor("pawn", "Black"),
            isPawn = true
        )
    }

    return pieces
}

// ============================================================
// MAIN SCREEN
// ============================================================

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun Chess3DScreen() {

    val context = LocalContext.current
    val launchIntent = (context as? android.app.Activity)?.intent

    val hasLaunchSettings =
        launchIntent?.hasExtra("GAME_MODE") == true

    val initialGameMode = remember {
        launchIntent
            ?.getStringExtra("GAME_MODE")
            ?.let { modeName ->
                runCatching {
                    GameMode.valueOf(modeName)
                }.getOrNull()
            } ?: GameMode.PLAYER_VS_COMPUTER
    }

    // Four opponent cards can map onto three AI difficulty levels.
    val initialOpponentIndex = remember {
        launchIntent
            ?.getIntExtra("OPPONENT_INDEX", 1)
            ?.coerceIn(0, 3) ?: 1
    }

    val initialDifficulty = remember {
        when (initialOpponentIndex) {
            0 -> AIDifficulty.EASY
            1 -> AIDifficulty.MEDIUM
            else -> AIDifficulty.HARD
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val moveAnimator = remember {
        ChessMoveAnimator()
    }

    val soundManager = remember(context) {
        ChessSoundManager(context.applicationContext)
    }

    DisposableEffect(soundManager) {
        onDispose {
            soundManager.release()
        }
    }

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val modelLoader = rememberModelLoader(engine)
    val nodes = rememberNodes()

    val controller = remember {
        ChessGameController()
    }

    val chessAI = remember {
        ChessAI()
    }

    // ========================================================
    // SETTINGS
    // ========================================================

    var gameSettings by remember {
        mutableStateOf(
            GameSettings(
                mode = initialGameMode,
                playerColor = PlayerColor.WHITE,
                difficulty = initialDifficulty
            )
        )
    }

    var selectedGameMode by remember {
        mutableStateOf(initialGameMode)
    }

    var selectedPlayerColor by remember {
        mutableStateOf(PlayerColor.WHITE)
    }

    var selectedDifficulty by remember {
        mutableStateOf(initialDifficulty)
    }

    var showGameSetup by remember {
        mutableStateOf(!hasLaunchSettings)
    }

    // ========================================================
    // GAME STATE
    // ========================================================

    var isAnimating by remember {
        mutableStateOf(false)
    }

    var aiThinking by remember {
        mutableStateOf(false)
    }

    var aiJob by remember {
        mutableStateOf<Job?>(null)
    }

    var selectedSquare by remember {
        mutableStateOf<Square?>(null)
    }

    var legalTargets by remember {
        mutableStateOf<Set<Square>>(emptySet())
    }

    var turn by remember {
        mutableStateOf("White")
    }

    var gameStatus by remember {
        mutableStateOf("WHITE TO MOVE")
    }

    var message by remember {
        mutableStateOf("Choose a match mode to begin.")
    }

    var whiteLoaded by remember {
        mutableIntStateOf(16)
    }

    var blackLoaded by remember {
        mutableIntStateOf(16)
    }

    var moveCount by remember {
        mutableIntStateOf(0)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var loadError by remember {
        mutableStateOf("")
    }

    // ========================================================
    // DIALOGS
    // ========================================================

    var showHistory by remember {
        mutableStateOf(false)
    }

    var showResignConfirmation by remember {
        mutableStateOf(false)
    }

    var showRestartConfirmation by remember {
        mutableStateOf(false)
    }

    var showDrawConfirmation by remember {
        mutableStateOf(false)
    }

    // ========================================================
    // SCENE STATE
    // ========================================================

    var boardRevision by remember {
        mutableIntStateOf(0)
    }

    var historyRevision by remember {
        mutableIntStateOf(0)
    }

    var latestNodeTap by remember {
        mutableStateOf<(Any?) -> Unit>({})
    }

    val squareNodes = remember {
        mutableMapOf<Square, CubeNode>()
    }

    val pieceNodes = remember {
        mutableMapOf<Square, PieceVisual>()
    }

    val highlightNodes = remember {
        mutableListOf<CubeNode>()
    }

    val highlightSquares = remember {
        mutableMapOf<CubeNode, Square>()
    }

    // Each snapshot represents the position before a move.
    val visualHistory = remember {
        mutableListOf<VisualSnapshot>()
    }

    // Snapshots of positions after moves that were undone.
    // The last item is the next visual position to redo.
    val redoVisualHistory = remember {
        mutableListOf<VisualSnapshot>()
    }

    // ========================================================
    // GENERAL HELPERS
    // ========================================================

    fun refreshStatus() {
        turn = controller.getTurn()
        gameStatus = controller.getStatus()
        moveCount = controller.getMoveCount()
    }

    fun clearHighlights() {
        highlightNodes.toList().forEach { node ->
            nodes.remove(node)
        }

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

    fun highlightCheckedKing() {
        if (!controller.isInCheck()) return

        val checkedSide = controller.getTurn()

        val kingSquare = pieceNodes.entries.firstOrNull { (_, piece) ->
            piece.name.equals("king", ignoreCase = true) &&
                    piece.side == checkedSide
        }?.key

        kingSquare?.let {
            addHighlight(it, CheckHighlight)
        }
    }

    fun captureVisualSnapshot(): VisualSnapshot {
        return VisualSnapshot(
            pieces = pieceNodes.toMap(),
            positions = pieceNodes.mapValues { (_, visual) ->
                visual.node.position
            },
            whiteLoaded = whiteLoaded,
            blackLoaded = blackLoaded
        )
    }

    fun saveVisualSnapshot() {
        visualHistory.add(captureVisualSnapshot())
    }

    fun restoreVisualSnapshot(snapshot: VisualSnapshot) {
        pieceNodes.values.forEach { visual ->
            nodes.remove(visual.node)
        }

        pieceNodes.clear()

        snapshot.pieces.forEach { (square, visual) ->
            snapshot.positions[square]?.let { savedPosition ->
                visual.node.position = savedPosition
            }

            nodes.add(visual.node)
            pieceNodes[square] = visual
        }

        whiteLoaded = snapshot.whiteLoaded
        blackLoaded = snapshot.blackLoaded
    }

    fun isUserTurn(): Boolean {
        return !gameSettings.isComputerTurn(controller.getTurn())
    }

    fun clearSelection() {
        selectedSquare = null
        legalTargets = emptySet()
        clearHighlights()
    }

    // ========================================================
    // PIECE SELECTION
    // ========================================================

    fun selectPiece(square: Square) {
        if (
            loading ||
            isAnimating ||
            aiThinking ||
            showGameSetup ||
            controller.isGameOver()
        ) {
            return
        }

        if (!isUserTurn()) {
            message = "ChessMaster is thinking. Wait for its move."
            return
        }

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

        legalTargets.forEach {
            addHighlight(it, MoveHighlight)
        }

        message = if (legalTargets.isEmpty()) {
            "This piece has no legal moves."
        } else {
            "${visual.name.replaceFirstChar { it.uppercase() }} selected. " +
                    "Choose a highlighted destination."
        }
    }

    // ========================================================
    // CAPTURE FEEDBACK
    // ========================================================

    suspend fun showCaptureEffect(square: Square) {
        val effect = CubeNode(
            engine = engine,
            size = Size(0.72f, 0.035f, 0.72f),
            center = Position(
                x = controller.fileOf(square) - 3.5f,
                y = BOARD_TOP_Y + 0.04f,
                z = controller.rankOf(square) - 3.5f
            ),
            materialInstance = materialLoader.createColorInstance(
                Color(0xCCFF8C45)
            )
        )

        nodes.add(effect)

        try {
            delay(180L)
        } finally {
            nodes.remove(effect)
        }
    }

    // ========================================================
    // PAWN PROMOTION VISUAL
    // ========================================================

    suspend fun promotePawnIfNeeded(square: Square) {
        val pawn = pieceNodes[square] ?: return

        if (!pawn.isPawn) return

        val rank = controller.rankOf(square)

        if (rank != 0 && rank != 7) return

        try {
            val queenModelFile = modelFileFor("queen", pawn.side)

            val instance = modelLoader.loadModelInstance(
                fileLocation = "models/$queenModelFile"
            )

            if (instance == null) {
                message = "Could not load the queen model for promotion."
                return
            }

            val queenNode = ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.08f
            )

            queenNode.position = pawn.node.position

            nodes.remove(pawn.node)
            nodes.add(queenNode)

            pieceNodes[square] = PieceVisual(
                name = "queen",
                side = pawn.side,
                node = queenNode,
                isPawn = false
            )
        } catch (exception: Exception) {
            message = "Promotion visual error: " +
                    (exception.message ?: "Unknown error")
        }
    }

    // ========================================================
    // EXECUTE MOVE
    // ========================================================

    fun executeMove(
        from: Square,
        to: Square,
        forcedMove: Move? = null
    ) {
        if (
            loading ||
            isAnimating ||
            controller.isGameOver()
        ) {
            return
        }

        val visual = pieceNodes[from]

        if (visual == null) {
            message = "Could not find the piece on ${from.name}."
            return
        }

        val move = if (forcedMove != null) {
            controller.getLegalMoves().firstOrNull {
                it.from == forcedMove.from &&
                        it.to == forcedMove.to &&
                        it.promotion == forcedMove.promotion
            }
        } else {
            controller.findLegalMove(from, to)
        }

        if (move == null) {
            message = "That move is not legal."
            return
        }

        val movingPieceName = visual.name

        val fromFile = controller.fileOf(from)
        val fromRank = controller.rankOf(from)
        val toFile = controller.fileOf(to)
        val toRank = controller.rankOf(to)

        val isEnPassant =
            movingPieceName.equals("pawn", ignoreCase = true) &&
                    fromFile != toFile &&
                    pieceNodes[to] == null

        val capturedSquare = if (isEnPassant) {
            controller.squareAt(toFile, fromRank)
        } else {
            to
        }

        val captured = pieceNodes[capturedSquare]

        val isCastling =
            movingPieceName.equals("king", ignoreCase = true) &&
                    kotlin.math.abs(toFile - fromFile) == 2

        var rookFrom: Square? = null
        var rookTo: Square? = null
        var rookVisual: PieceVisual? = null

        if (isCastling) {
            rookFrom = controller.squareAt(
                if (toFile > fromFile) 7 else 0,
                fromRank
            )

            rookTo = controller.squareAt(
                if (toFile > fromFile) 5 else 3,
                fromRank
            )

            rookVisual = pieceNodes[rookFrom]
        }

        saveVisualSnapshot()

        if (!controller.playMove(move)) {
            visualHistory.removeAt(visualHistory.lastIndex)
            message = "The chess rules rejected that move."
            return
        }

        // A new move creates a new branch; old redo moves are discarded.
        redoVisualHistory.clear()

        isAnimating = true

        pieceNodes.remove(from)
        pieceNodes[to] = visual

        if (captured != null) {
            pieceNodes.remove(capturedSquare)
            nodes.remove(captured.node)

            if (captured.side == "White") {
                whiteLoaded = (whiteLoaded - 1).coerceAtLeast(0)
            } else {
                blackLoaded = (blackLoaded - 1).coerceAtLeast(0)
            }
        }

        if (rookFrom != null && rookTo != null && rookVisual != null) {
            pieceNodes.remove(rookFrom)
            pieceNodes[rookTo] = rookVisual
        }

        clearSelection()
        highlightCheckedKing()

        refreshStatus()
        historyRevision++

        val result = controller.getResult()

        message = when {
            result.outcome != MatchOutcome.IN_PROGRESS ->
                result.description

            controller.isInCheck() ->
                "${controller.getTurn()} is in check!"

            captured != null ->
                "$movingPieceName captured a piece. " +
                        "${controller.getTurn()} to move."

            else ->
                "$movingPieceName moved from ${from.name} to ${to.name}. " +
                        "${controller.getTurn()} to move."
        }

        coroutineScope.launch {
            try {
                val startPosition = visual.node.position

                val destinationPosition = moveAnimator.positionFor(
                    file = toFile,
                    rank = toRank,
                    isPawn = visual.isPawn
                )

                val animationJobs = mutableListOf<Job>()

                animationJobs += launch {
                    moveAnimator.animate(
                        node = visual.node,
                        from = startPosition,
                        to = destinationPosition
                    )
                }

                if (rookVisual != null && rookFrom != null && rookTo != null) {
                    val rookStart = rookVisual.node.position

                    val rookDestination = moveAnimator.positionFor(
                        file = controller.fileOf(rookTo),
                        rank = controller.rankOf(rookTo),
                        isPawn = false
                    )

                    animationJobs += launch {
                        moveAnimator.animate(
                            node = rookVisual.node,
                            from = rookStart,
                            to = rookDestination
                        )
                    }
                }

                animationJobs.forEach { it.join() }

                promotePawnIfNeeded(to)

                if (captured != null) {
                    soundManager.playCapture()
                    showCaptureEffect(capturedSquare)
                } else {
                    soundManager.playMove()
                }

                if (controller.isInCheck()) {
                    delay(120L)
                    soundManager.playCheck()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                message = "Move completed, but an animation effect failed: " +
                        (exception.message ?: "Unknown error")
            } finally {
                isAnimating = false
                refreshStatus()
                historyRevision++
            }
        }
    }

    // ========================================================
    // BOARD INPUT
    // ========================================================

    fun handleSquareTap(square: Square) {
        if (
            loading ||
            isAnimating ||
            aiThinking ||
            showGameSetup
        ) {
            return
        }

        if (!isUserTurn()) {
            message = "Wait for ChessMaster to finish its move."
            return
        }

        if (controller.isGameOver()) {
            val result = controller.getResult()
            gameStatus = result.title
            message = "${result.description}\nFinal Score: ${result.score}"
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

    fun handleNodeTap(node: Any?) {
        if (
            node == null ||
            loading ||
            isAnimating ||
            aiThinking ||
            showGameSetup
        ) {
            return
        }

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

            if (
                selectedSquare != null &&
                tappedSquare in legalTargets
            ) {
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

    latestNodeTap = { tappedNode ->
        handleNodeTap(tappedNode)
    }

    // ========================================================
    // UNDO
    // ========================================================

    fun undoLastMove() {
        if (loading || isAnimating || aiThinking) {
            message = "Wait until the current move finishes."
            return
        }

        if (!controller.canUndo()) {
            message = "There are no moves available to undo."
            return
        }

        clearSelection()

        val movesToUndo = if (
            gameSettings.mode == GameMode.PLAYER_VS_COMPUTER &&
            controller.getMoveCount() >= 2
        ) {
            2
        } else {
            1
        }

        var undone = 0

        repeat(movesToUndo) {
            if (!controller.canUndo()) return@repeat

            // Save the current visual position before undoing.
            val afterMoveSnapshot = captureVisualSnapshot()

            if (!controller.undoMove()) {
                return@repeat
            }

            redoVisualHistory.add(afterMoveSnapshot)

            val previousSnapshot = visualHistory.removeLastOrNull()

            if (previousSnapshot != null) {
                restoreVisualSnapshot(previousSnapshot)
            }

            undone++
        }

        clearHighlights()
        refreshStatus()
        historyRevision++

        message = if (undone > 0) {
            "$undone move(s) undone. ${controller.getTurn()} to move."
        } else {
            "Unable to undo the move."
        }
    }

    // ========================================================
    // REDO
    // ========================================================

    fun redoLastMove() {
        if (loading || isAnimating || aiThinking) {
            message = "Wait until the current move finishes."
            return
        }

        if (!controller.canRedo() || redoVisualHistory.isEmpty()) {
            message = "There are no moves available to redo."
            return
        }

        clearSelection()

        val movesToRedo = if (
            gameSettings.mode == GameMode.PLAYER_VS_COMPUTER &&
            redoVisualHistory.size >= 2 &&
            controller.canRedo()
        ) {
            2
        } else {
            1
        }

        var redone = 0

        repeat(movesToRedo) {
            if (!controller.canRedo() || redoVisualHistory.isEmpty()) {
                return@repeat
            }

            // Preserve the position before this redo so it can be undone again.
            val beforeMoveSnapshot = captureVisualSnapshot()

            if (!controller.redoMove()) {
                return@repeat
            }

            visualHistory.add(beforeMoveSnapshot)

            // Snapshots are stored in undo order, so the last item is next.
            val afterMoveSnapshot = redoVisualHistory.removeAt(
                redoVisualHistory.lastIndex
            )

            restoreVisualSnapshot(afterMoveSnapshot)
            redone++
        }

        clearHighlights()
        refreshStatus()
        historyRevision++

        message = if (redone > 0) {
            "$redone move(s) restored. ${controller.getTurn()} to move."
        } else {
            "Unable to redo the move."
        }
    }

    // ========================================================
    // RESTART
    // ========================================================

    fun restartGame() {
        if (isAnimating) {
            message = "Wait for the current animation to finish."
            return
        }

        aiJob?.cancel()
        aiThinking = false

        controller.restart()

        clearSelection()

        visualHistory.clear()
        redoVisualHistory.clear()

        clearHighlights()

        pieceNodes.values.forEach { visual ->
            nodes.remove(visual.node)
        }

        pieceNodes.clear()

        whiteLoaded = 0
        blackLoaded = 0
        moveCount = 0

        refreshStatus()

        loading = true
        boardRevision++

        message = "Preparing your new match..."
    }

    // ========================================================
    // LOAD 3D BOARD AND PIECES
    // ========================================================

    LaunchedEffect(
        engine,
        modelLoader,
        materialLoader,
        loading,
        boardRevision
    ) {
        if (!loading) return@LaunchedEffect

        loadError = ""
        whiteLoaded = 0
        blackLoaded = 0

        if (squareNodes.isEmpty()) {
            for (rank in 0..7) {
                for (file in 0..7) {
                    val square = controller.squareAt(file, rank)

                    val color = if ((file + rank) % 2 == 0) {
                        BoardLight
                    } else {
                        BoardDark
                    }

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
                    name = piece.name,
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
                loadError += "${piece.modelFile}: " +
                        "${exception.message ?: "Loading failed"}\n"
            }
        }

        refreshStatus()
        loading = false

        message = if (loadError.isBlank()) {
            "3D board ready. ${controller.getTurn()} to move."
        } else {
            "Board loaded with some asset errors. Check the details below."
        }
    }

    // ========================================================
    // COMPUTER OPPONENT
    // ========================================================

    LaunchedEffect(
        turn,
        loading,
        isAnimating,
        showGameSetup,
        gameSettings
    ) {
        if (loading || isAnimating || showGameSetup) {
            return@LaunchedEffect
        }

        if (controller.isGameOver()) {
            aiThinking = false
            return@LaunchedEffect
        }

        if (!gameSettings.isComputerTurn(controller.getTurn())) {
            aiThinking = false
            return@LaunchedEffect
        }

        if (aiThinking) return@LaunchedEffect

        aiThinking = true
        message = "ChessMaster is analysing the position..."

        try {
            val fen = controller.getFen()
            val difficulty = gameSettings.difficulty

            val computerMove = chessAI.chooseMove(
                fen = fen,
                difficulty = difficulty
            )

            if (computerMove == null) {
                refreshStatus()
                message = controller.getResult().description
                return@LaunchedEffect
            }

            if (
                !controller.isGameOver() &&
                !loading &&
                !showGameSetup
            ) {
                executeMove(
                    from = computerMove.from,
                    to = computerMove.to,
                    forcedMove = computerMove
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            message = "Computer opponent error: " +
                    (exception.message ?: "Unknown error")
        } finally {
            aiThinking = false
        }
    }

    // ========================================================
    // CAMERA
    // ========================================================

    val cameraManipulator = rememberCameraManipulator(
        orbitHomePosition = Position(0f, 9f, 10f),
        targetPosition = Position(0f, 0f, 0f)
    )

    // ========================================================
    // MAIN USER INTERFACE
    // ========================================================

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        val compactLayout = maxWidth < 380.dp || maxHeight < 700.dp

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // HEADER

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 18.dp,
                        end = 18.dp,
                        top = 30.dp,
                        bottom = 8.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            modifier = Modifier
                                .padding(top = 15.dp),
                            text = "CHESSMASTER",
                            color = Gold,
                            fontSize = if (compactLayout) 21.sp else 25.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )

                        Text(
                            text = "THE 3D CHESS EXPERIENCE",
                            color = Muted,
                            fontSize = 9.sp,
                            letterSpacing = 1.8.sp
                        )
                    }

                    Surface(
                        color = ButtonSurface,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, ButtonBorder)
                    ) {
                        Text(
                            text = when {
                                loading -> "LOADING"
                                aiThinking -> "AI THINKING"
                                isAnimating -> "ANIMATING"
                                controller.isGameOver() -> "FINISHED"
                                else -> "● LIVE MATCH"
                            },
                            color = when {
                                aiThinking -> Gold
                                loading || isAnimating -> Muted
                                controller.isGameOver() -> ErrorRed
                                else -> Green
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 7.dp
                            )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Panel,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (
                                gameSettings.mode ==
                                GameMode.PLAYER_VS_COMPUTER
                            ) {
                                "PLAYER VS COMPUTER"
                            } else {
                                "PLAYER VS PLAYER"
                            },
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (
                                gameSettings.mode ==
                                GameMode.PLAYER_VS_COMPUTER
                            ) {
                                "${gameSettings.playerColor.title}  ·  " +
                                        gameSettings.difficulty.title
                            } else {
                                "Local multiplayer"
                            },
                            color = Muted,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "CURRENT TURN",
                            color = Muted,
                            fontSize = 8.sp,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = gameStatus,
                            color = when {
                                gameStatus.contains(
                                    "CHECKMATE",
                                    ignoreCase = true
                                ) -> ErrorRed

                                gameStatus.contains(
                                    "CHECK",
                                    ignoreCase = true
                                ) -> ErrorRed

                                else -> Gold
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3D BOARD

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp),
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
                            latestNodeTap(node)
                        }
                    )
                )

                if (loading) {
                    Surface(
                        color = Panel,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ButtonBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 25.dp)
                                .padding(
                                horizontal = 22.dp,
                                vertical = 16.dp
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PREPARING CHESSBOARD",
                                color = Gold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                text = "Loading 3D pieces...",
                                color = Muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }


// MATCH CONTROL PANEL

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .wrapContentHeight(Alignment.Top)
                    .verticalScroll(rememberScrollState())
                    .background(
                        Panel,
                        RoundedCornerShape(
                            topStart = 22.dp,
                            topEnd = 22.dp
                        )
                    )
                    .padding(
                        horizontal = 22.dp,
                        vertical = if (compactLayout) 150.dp else 160.dp
                    ),
                verticalArrangement = Arrangement.Top
            ) {



                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MATCH CONTROL",
                        color = Gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp
                    )

                    Text(
                        text = if (controller.isGameOver()) {
                            "GAME OVER"
                        } else {
                            "MOVE $moveCount"
                        },
                        color = Muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(7.dp))

                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(9.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            AppBackground,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusCounter(
                        label = "WHITE PIECES",
                        value = "$whiteLoaded / 16",
                        valueColor = Color.White
                    )

                    StatusCounter(
                        label = "BLACK PIECES",
                        value = "$blackLoaded / 16",
                        valueColor = Gold
                    )

                    StatusCounter(
                        label = "TOTAL MOVES",
                        value = moveCount.toString(),
                        valueColor = Green
                    )
                }

                Spacer(Modifier.height(9.dp))

                // ========================================================
                // MATCH CONTROLS
                // ========================================================

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    PremiumButton(
                        label = "UNDO",
                        modifier = Modifier.weight(1f),
                        enabled = !loading &&
                                !isAnimating &&
                                !aiThinking &&
                                controller.canUndo()
                    ) {
                        undoLastMove()
                    }

                    PremiumButton(
                        label = "REDO",
                        modifier = Modifier.weight(1f),
                        enabled = !loading &&
                                !isAnimating &&
                                !aiThinking &&
                                controller.canRedo()
                    ) {
                        redoLastMove()
                    }

                    PremiumButton(
                        label = "HISTORY",
                        modifier = Modifier.weight(1f),
                        enabled = !loading
                    ) {
                        showHistory = true
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    PremiumButton(
                        label = "HINT",
                        modifier = Modifier.weight(1f),
                        enabled = !loading &&
                                !isAnimating &&
                                !aiThinking &&
                                !controller.isGameOver()
                    ) {
                        val currentTurn = controller.getTurn()

                        if (!isUserTurn()) {
                            message = "Wait for ChessMaster to finish its move."
                        } else {
                            val legalMoves = controller.getLegalMoves()

                            if (legalMoves.isEmpty()) {
                                message = "No legal moves are available."
                            } else {
                                val hintMove = legalMoves.random()

                                val from = hintMove.from
                                val to = hintMove.to

                                message = "Hint: move from ${from.name} to ${to.name}."

                                selectedSquare = from

                                legalTargets = controller.getLegalMovesFrom(from)
                                    .map { it.to }
                                    .toSet()

                                clearHighlights()
                                addHighlight(from, SelectedHighlight)

                                legalTargets.forEach { target ->
                                    addHighlight(target, MoveHighlight)
                                }
                            }
                        }
                    }

                    PremiumButton(
                        label = "OFFER DRAW",
                        modifier = Modifier.weight(1.2f),
                        enabled = !loading &&
                                !isAnimating &&
                                !aiThinking &&
                                !controller.isGameOver() &&
                                gameSettings.mode == GameMode.PLAYER_VS_PLAYER
                    ) {
                        if (controller.offerDraw()) {
                            message = "Draw offered. Your opponent can accept it."
                        } else {
                            message = "Unable to offer a draw right now."
                        }
                    }

                    PremiumButton(
                        label = "ACCEPT DRAW",
                        modifier = Modifier.weight(1.2f),
                        enabled = !loading &&
                                !isAnimating &&
                                !aiThinking &&
                                controller.canAcceptDraw()
                    ) {
                        showDrawConfirmation = true
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    PremiumButton(
                        label = "RESIGN",
                        modifier = Modifier.weight(1f),
                        enabled = !loading &&
                                !isAnimating &&
                                !aiThinking &&
                                !controller.isGameOver(),
                        danger = true
                    ) {
                        showResignConfirmation = true
                    }

                    PremiumButton(
                        label = "RESTART",
                        modifier = Modifier.weight(1f),
                        enabled = !loading && !isAnimating
                    ) {
                        showRestartConfirmation = true
                    }

                    PremiumButton(
                        label = "NEW SETTINGS",
                        modifier = Modifier.weight(1.3f),
                        enabled = !loading && !isAnimating
                    ) {
                        selectedGameMode = gameSettings.mode
                        selectedPlayerColor = gameSettings.playerColor
                        selectedDifficulty = gameSettings.difficulty
                        showGameSetup = true
                    }
                }
            }
        }
    }

    // ========================================================
    // MATCH SETUP DIALOG
    // ========================================================

    if (showGameSetup) {
        AlertDialog(
            onDismissRequest = {
                // Setup stays open until the player starts a match.
            },
            containerColor = Panel,
            titleContentColor = Gold,
            textContentColor = Color.White,
            title = {
                Column {
                    Text(
                        text = "CHESSMASTER",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )

                    Text(
                        text = "CONFIGURE YOUR MATCH",
                        color = Muted,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "GAME MODE",
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    GameMode.values().forEach { mode ->
                        SetupChoice(
                            title = mode.title,
                            description = mode.description,
                            selected = selectedGameMode == mode,
                            onClick = {
                                selectedGameMode = mode
                            }
                        )
                    }

                    if (
                        selectedGameMode ==
                        GameMode.PLAYER_VS_COMPUTER
                    ) {
                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "YOUR COLOR",
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        PlayerColor.values().forEach { color ->
                            SetupChoice(
                                title = color.title,
                                description = color.description,
                                selected = selectedPlayerColor == color,
                                onClick = {
                                    selectedPlayerColor = color
                                }
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "COMPUTER DIFFICULTY",
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        AIDifficulty.values().forEach { difficulty ->
                            SetupChoice(
                                title = difficulty.title,
                                description = difficulty.description,
                                selected = selectedDifficulty == difficulty,
                                onClick = {
                                    selectedDifficulty = difficulty
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        aiJob?.cancel()
                        aiThinking = false

                        gameSettings = GameSettings(
                            mode = selectedGameMode,
                            playerColor = selectedPlayerColor,
                            difficulty = selectedDifficulty
                        )

                        showGameSetup = false
                        restartGame()
                    },
                    enabled = !loading && !isAnimating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = AppBackground
                    )
                ) {
                    Text(
                        text = "START MATCH",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                if (!loading) {
                    TextButton(
                        onClick = {
                            selectedGameMode = GameMode.PLAYER_VS_PLAYER
                            selectedPlayerColor = PlayerColor.WHITE
                            selectedDifficulty = AIDifficulty.MEDIUM

                            gameSettings = GameSettings(
                                mode = GameMode.PLAYER_VS_PLAYER,
                                playerColor = PlayerColor.WHITE,
                                difficulty = AIDifficulty.MEDIUM
                            )

                            showGameSetup = false
                            restartGame()
                        }
                    ) {
                        Text("QUICK START", color = Muted)
                    }
                }
            }
        )
    }

    // ========================================================
    // MOVE HISTORY
    // ========================================================

    if (showHistory) {
        AlertDialog(
            onDismissRequest = {
                showHistory = false
            },
            containerColor = Panel,
            titleContentColor = Gold,
            textContentColor = Color.White,
            title = {
                Column {
                    Text(
                        text = "MOVE HISTORY",
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = "$moveCount half-moves played",
                        color = Muted,
                        fontSize = 11.sp
                    )
                }
            },
            text = {
                key(historyRevision) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val history = controller.getMoveHistory()

                        if (history.isEmpty()) {
                            Text(
                                text = "Your match history will appear here.",
                                color = Muted
                            )
                        } else {
                            history.forEachIndexed { index, move ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    color = if (index % 2 == 0) {
                                        ButtonSurface
                                    } else {
                                        Panel
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 8.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}.",
                                            color = Gold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(Modifier.width(10.dp))

                                        Text(
                                            text = move.display(),
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHistory = false
                    }
                ) {
                    Text("CLOSE", color = Gold)
                }
            }
        )
    }

    // ========================================================
    // RESIGN CONFIRMATION
    // ========================================================

    if (showResignConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showResignConfirmation = false
            },
            containerColor = Panel,
            title = {
                Text(
                    text = "RESIGN MATCH?",
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to resign?",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResignConfirmation = false

                        if (controller.resign()) {
                            val result = controller.getResult()

                            gameStatus = result.title
                            message = "${result.description}\n" +
                                    "Final Score: ${result.score}"

                            clearSelection()
                            refreshStatus()
                        }
                    }
                ) {
                    Text("RESIGN", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResignConfirmation = false
                    }
                ) {
                    Text("CANCEL", color = Muted)
                }
            }
        )
    }

    // ========================================================
    // ACCEPT DRAW CONFIRMATION
    // ========================================================

    if (showDrawConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showDrawConfirmation = false
            },
            containerColor = Panel,
            title = {
                Text(
                    text = "ACCEPT DRAW?",
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Accepting this offer will end the game as a draw.",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDrawConfirmation = false

                        if (controller.acceptDraw()) {
                            val result = controller.getResult()

                            gameStatus = result.title
                            message = "${result.description}\n" +
                                    "Final Score: ${result.score}"

                            clearSelection()
                            refreshStatus()
                        } else {
                            message = "The draw offer is no longer available."
                        }
                    }
                ) {
                    Text("ACCEPT DRAW", color = Gold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDrawConfirmation = false
                    }
                ) {
                    Text("CANCEL", color = Muted)
                }
            }
        )
    }

    // ========================================================
    // RESTART CONFIRMATION
    // ========================================================

    if (showRestartConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showRestartConfirmation = false
            },
            containerColor = Panel,
            title = {
                Text(
                    text = "RESTART GAME?",
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "The current match and its move history will be cleared.",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestartConfirmation = false
                        restartGame()
                    }
                ) {
                    Text("RESTART", color = Gold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestartConfirmation = false
                    }
                ) {
                    Text("CANCEL", color = Muted)
                }
            }
        )
    }
}

// ============================================================
// MATCH SETUP OPTION
// ============================================================

@Composable
private fun SetupChoice(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        color = if (selected) Color(0xFF303C40) else ButtonSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Gold else ButtonBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = if (selected) Gold else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = description,
                    color = Muted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }

            if (selected) {
                Text(
                    text = "✓",
                    color = Gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================================================
// PREMIUM BUTTON
// ============================================================

@Composable
private fun PremiumButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (danger) {
                Color(0xFF512C32)
            } else {
                ButtonSurface
            },
            contentColor = if (danger) ErrorRed else Color.White,
            disabledContainerColor = Color(0xFF1B252E),
            disabledContentColor = Color(0xFF65727D)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (danger) Color(0xFF754047) else ButtonBorder
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 0.dp
        ),
        contentPadding = PaddingValues(
            horizontal = 4.dp,
            vertical = 4.dp
        )
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================
// STATUS COUNTER
// ============================================================

@Composable
private fun StatusCounter(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            color = Muted,
            fontSize = 8.sp,
            letterSpacing = 0.3.sp
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

