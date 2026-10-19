package com.koridor.game

/** Tahta ölçüleri: 9x9 kare, duvarlar 8x8 kesişim noktasına yerleşir. */
object Board {
    const val SIZE = 9
    const val SLOTS = SIZE - 1
}

data class Cell(val x: Int, val y: Int) {
    val isOnBoard: Boolean get() = x in 0 until Board.SIZE && y in 0 until Board.SIZE
    operator fun plus(d: Direction) = Cell(x + d.dx, y + d.dy)
}

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

    val perpendicular: List<Direction>
        get() = if (dx == 0) listOf(LEFT, RIGHT) else listOf(UP, DOWN)
}

enum class Orientation {
    HORIZONTAL, VERTICAL;

    fun toggled() = if (this == HORIZONTAL) VERTICAL else HORIZONTAL
}

/**
 * Duvar, (x, y) kesişim noktasının merkezine oturur ve iki kare uzunluğundadır.
 * Yatay duvar y ile y+1 satırları arasında, x ve x+1 sütunlarını kapatır.
 * Dikey duvar x ile x+1 sütunları arasında, y ve y+1 satırlarını kapatır.
 */
data class Wall(val x: Int, val y: Int, val orientation: Orientation) {
    val isOnBoard: Boolean get() = x in 0 until Board.SLOTS && y in 0 until Board.SLOTS
}

/** Oyuncunun başladığı kenar; hedefi karşı kenardır. */
enum class Side(val start: Cell) {
    SOUTH(Cell(4, 8)),
    NORTH(Cell(4, 0)),
    WEST(Cell(0, 4)),
    EAST(Cell(8, 4));

    fun isGoal(c: Cell): Boolean = when (this) {
        SOUTH -> c.y == 0
        NORTH -> c.y == Board.SIZE - 1
        WEST -> c.x == Board.SIZE - 1
        EAST -> c.x == 0
    }
}

sealed interface Move {
    data class Step(val to: Cell) : Move
    data class PlaceWall(val wall: Wall) : Move
}
