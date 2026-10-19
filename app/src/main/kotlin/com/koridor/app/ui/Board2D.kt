package com.koridor.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import com.koridor.app.BoardScene
import com.koridor.app.PawnStyle
import com.koridor.app.Players
import com.koridor.game.Board
import com.koridor.game.Cell
import com.koridor.game.Orientation
import com.koridor.game.Side
import com.koridor.game.Wall
import kotlin.math.abs
import kotlin.math.min

private val ValidColor = Color(0xB34CF273)
private val InvalidColor = Color(0xB3F24040)

private const val MARGIN = 0.4f
private const val GAP = 0.08f

/** Tahtanın kuşbakışı düz çizimi. 9x9 birimlik tahta, kenarlarda [MARGIN] kadar boşlukla kareye oturur. */
@Composable
fun Board2D(scene: BoardScene, onTap: (Float, Float) -> Unit, modifier: Modifier = Modifier) {
    val state = scene.state
    val theme = scene.theme

    // Piyonlar yeni karelerine kayarak gider.
    val pawnPositions: List<State<Offset>> = (0 until state.playerCount).map { p ->
        val c = state.position(p)
        animateOffsetAsState(Offset(c.x.toFloat(), c.y.toFloat()), tween(260), label = "pawn$p")
    }

    // Yeni duvarlar büyüyerek belirir; ekran ilk açıldığında var olanlar animasyonsuz gelir.
    val wallAnims = remember { mutableStateMapOf<Wall, Animatable<Float, AnimationVector1D>>() }
    val seeded = remember { mutableStateOf(false) }
    LaunchedEffect(state.walls) {
        val current = state.walls.toSet()
        wallAnims.keys.retainAll(current)
        for (w in current) {
            if (w !in wallAnims) {
                val anim = Animatable(if (seeded.value) 0f else 1f)
                wallAnims[w] = anim
                launch { anim.animateTo(1f, tween(320)) }
            }
        }
        seeded.value = true
    }

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

        drawRoundRect(theme.base, cornerRadius = CornerRadius(pitch * 0.3f))

        // Hedef kenarları oyuncu renginde.
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
            val tile = theme.tileColor(x, y)
            val color = if (Cell(x, y) in highlighted) lerp(tile, hi, 0.55f) else tile
            drawRoundRect(
                color,
                topLeft = at(x + GAP, y + GAP),
                size = Size((1 - 2 * GAP) * pitch, (1 - 2 * GAP) * pitch),
                cornerRadius = CornerRadius(pitch * 0.1f),
            )
        }

        for (w in state.walls) {
            val t = wallAnims[w]?.value ?: 1f
            drawWall(w, theme.wall.copy(alpha = t), pitch, at, grow = t)
        }
        scene.preview?.let { drawWall(it, if (scene.previewValid) ValidColor else InvalidColor, pitch, at) }

        for (p in 0 until state.playerCount) {
            val pos = pawnPositions[p].value
            val target = state.position(p)
            // Kayarken biraz büyüyüp küçülerek zıplama hissi verir.
            val remaining = min(1f, abs(target.x - pos.x) + abs(target.y - pos.y))
            val hop = 1f + 0.7f * remaining * (1f - remaining)
            val center = at(pos.x + 0.5f, pos.y + 0.5f)
            if (p == state.currentPlayer && !state.isOver) {
                drawCircle(Color.White.copy(alpha = 0.7f), pitch * 0.44f, center, style = Stroke(pitch * 0.06f))
            }
            drawPawn(scene.pawnStyle, Players.color(state.playerCount, p), center, pitch * 0.34f * hop)
        }
    }
}

/** Bir piyonu kuşbakışı çizer; [radius] piyonun dış yarıçapıdır. Görünüm ekranındaki önizlemeler de bunu kullanır. */
fun DrawScope.drawPawn(style: PawnStyle, color: Color, center: Offset, radius: Float) {
    val dark = lerp(color, Color.Black, 0.3f)
    val light = lerp(color, Color.White, 0.4f)
    when (style) {
        PawnStyle.CLASSIC -> {
            drawCircle(dark, radius, center)
            drawCircle(color, radius * 0.82f, center)
            drawCircle(Color.White.copy(alpha = 0.35f), radius * 0.3f, center + Offset(-radius * 0.24f, -radius * 0.24f))
        }
        PawnStyle.CHECKER -> {
            drawCircle(dark, radius * 1.05f, center)
            drawCircle(color, radius * 0.95f, center)
            drawCircle(light, radius * 0.68f, center, style = Stroke(radius * 0.08f))
            drawCircle(light, radius * 0.45f, center, style = Stroke(radius * 0.06f))
        }
        PawnStyle.ROOK -> {
            val r = radius * 0.92f
            drawRoundRect(dark, center - Offset(r, r), Size(2 * r, 2 * r), CornerRadius(r * 0.25f))
            val inner = r * 0.78f
            drawRoundRect(color, center - Offset(inner, inner), Size(2 * inner, 2 * inner), CornerRadius(r * 0.15f))
            // Köşelerdeki mazgallar
            val t = r * 0.42f
            for ((sx, sy) in listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)) {
                val c = center + Offset(sx * (inner - t / 2), sy * (inner - t / 2))
                drawRect(light, c - Offset(t / 2, t / 2), Size(t, t))
            }
        }
        PawnStyle.CRYSTAL -> {
            fun diamond(s: Float) = Path().apply {
                moveTo(center.x, center.y - s)
                lineTo(center.x + s, center.y)
                lineTo(center.x, center.y + s)
                lineTo(center.x - s, center.y)
                close()
            }
            drawPath(diamond(radius * 1.1f), dark)
            drawPath(diamond(radius * 0.92f), color)
            // Parlak yüzey
            drawPath(
                Path().apply {
                    moveTo(center.x, center.y - radius * 0.92f)
                    lineTo(center.x + radius * 0.92f, center.y)
                    lineTo(center.x, center.y)
                    close()
                },
                light.copy(alpha = 0.7f),
            )
        }
    }
}

private fun DrawScope.drawWall(w: Wall, color: Color, pitch: Float, at: (Float, Float) -> Offset, grow: Float = 1f) {
    val cx = w.x + 1f
    val cy = w.y + 1f
    val half = (1f - GAP) * grow
    val thick = 0.09f
    if (w.orientation == Orientation.HORIZONTAL) {
        drawRoundRect(color, at(cx - half, cy - thick), Size(2 * half * pitch, 2 * thick * pitch), CornerRadius(pitch * 0.06f))
    } else {
        drawRoundRect(color, at(cx - thick, cy - half), Size(2 * thick * pitch, 2 * half * pitch), CornerRadius(pitch * 0.06f))
    }
}
