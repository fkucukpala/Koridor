package com.koridor.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import com.koridor.app.BoardScene
import com.koridor.app.Players
import com.koridor.game.Board
import com.koridor.game.Cell
import com.koridor.game.Orientation
import com.koridor.game.Side
import com.koridor.game.Wall

private val BaseColor = Color(0xFF3D2414)
private val TileColor = Color(0xFF80502B)
private val WallColor = Color(0xFFEDD199)
private val ValidColor = Color(0xB34CF273)
private val InvalidColor = Color(0xB3F24040)

private const val MARGIN = 0.4f
private const val GAP = 0.08f

/** Tahtanın kuşbakışı düz çizimi. 9x9 birimlik tahta, kenarlarda [MARGIN] kadar boşlukla kareye oturur. */
@Composable
fun Board2D(scene: BoardScene, onTap: (Float, Float) -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val pitch = size.width / (Board.SIZE + 2 * MARGIN)
                    onTap(pos.x / pitch - MARGIN, pos.y / pitch - MARGIN)
                }
            },
    ) {
        val pitch = size.width / (Board.SIZE + 2 * MARGIN)
        val at = { u: Float, v: Float -> Offset((u + MARGIN) * pitch, (v + MARGIN) * pitch) }

        drawRoundRect(BaseColor, cornerRadius = CornerRadius(pitch * 0.3f))

        // Hedef kenarları oyuncu renginde.
        val state = scene.state
        val strip = pitch * 0.12f
        val length = Board.SIZE * pitch
        for (p in 0 until state.playerCount) {
            val c = Players.color(state.playerCount, p)
            when (state.sides[p]) {
                Side.SOUTH -> drawRect(c, at(0f, -0.3f), Size(length, strip))
                Side.NORTH -> drawRect(c, at(0f, Board.SIZE + 0.18f), Size(length, strip))
                Side.WEST -> drawRect(c, at(Board.SIZE + 0.18f, 0f), Size(strip, length))
                Side.EAST -> drawRect(c, at(-0.3f, 0f), Size(strip, length))
            }
        }

        val hi = Players.color(state.playerCount, state.currentPlayer)
        val highlighted = scene.highlights.toSet()
        for (y in 0 until Board.SIZE) for (x in 0 until Board.SIZE) {
            val color = if (Cell(x, y) in highlighted) lerp(TileColor, hi, 0.55f) else TileColor
            drawRoundRect(
                color,
                topLeft = at(x + GAP, y + GAP),
                size = Size((1 - 2 * GAP) * pitch, (1 - 2 * GAP) * pitch),
                cornerRadius = CornerRadius(pitch * 0.1f),
            )
        }

        for (w in state.walls) drawWall(w, WallColor, pitch, at)
        scene.preview?.let { drawWall(it, if (scene.previewValid) ValidColor else InvalidColor, pitch, at) }

        for (p in 0 until state.playerCount) {
            val c = state.position(p)
            val center = at(c.x + 0.5f, c.y + 0.5f)
            val color = Players.color(state.playerCount, p)
            if (p == state.currentPlayer && !state.isOver) {
                drawCircle(Color.White.copy(alpha = 0.7f), pitch * 0.42f, center, style = Stroke(pitch * 0.06f))
            }
            drawCircle(lerp(color, Color.Black, 0.3f), pitch * 0.34f, center)
            drawCircle(color, pitch * 0.28f, center)
            drawCircle(Color.White.copy(alpha = 0.35f), pitch * 0.1f, center + Offset(-pitch * 0.08f, -pitch * 0.08f))
        }
    }
}

private fun DrawScope.drawWall(w: Wall, color: Color, pitch: Float, at: (Float, Float) -> Offset) {
    val cx = w.x + 1f
    val cy = w.y + 1f
    val half = 1f - GAP
    val thick = 0.09f
    if (w.orientation == Orientation.HORIZONTAL) {
        drawRoundRect(color, at(cx - half, cy - thick), Size(2 * half * pitch, 2 * thick * pitch), CornerRadius(pitch * 0.06f))
    } else {
        drawRoundRect(color, at(cx - thick, cy - half), Size(2 * thick * pitch, 2 * half * pitch), CornerRadius(pitch * 0.06f))
    }
}
