package com.example.chessmaster

import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.delay
import kotlin.math.abs

class ChessMoveAnimator {

    companion object {
        const val MOVE_DURATION_MS = 240L
        const val FRAME_DELAY_MS = 16L
    }

    suspend fun animate(
        node: ModelNode,
        from: Position,
        to: Position,
        durationMs: Long = MOVE_DURATION_MS
    ) {
        val duration = durationMs.coerceAtLeast(1L)
        val frames = (duration / FRAME_DELAY_MS)
            .toInt()
            .coerceAtLeast(1)

        for (frame in 1..frames) {
            val progress = frame.toFloat() / frames

            // Smooth ease-in/ease-out interpolation.
            val eased = progress * progress *
                    (3f - 2f * progress)

            node.position = Position(
                x = from.x + (to.x - from.x) * eased,
                y = from.y + (to.y - from.y) * eased,
                z = from.z + (to.z - from.z) * eased
            )

            delay(FRAME_DELAY_MS)
        }

        // Ensure the model ends precisely on the destination square.
        node.position = to
    }

    fun positionFor(
        file: Int,
        rank: Int,
        isPawn: Boolean
    ): Position {
        return Position(
            x = file - 3.5f,
            y = 0.06f + if (isPawn) 0.39f else 0.54f,
            z = rank - 3.5f
        )
    }
}