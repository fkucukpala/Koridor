package com.koridor.game

/**
 * Oyunun değişmez anlık durumu. Her hamle yeni bir [GameState] üretir,
 * bu sayede geri alma ve yapay zekâ araması kolaylaşır.
 */
class GameState private constructor(
    val sides: List<Side>,
    private val positions: IntArray,
    private val wallsLeft: IntArray,
    private val hWalls: BooleanArray,
    private val vWalls: BooleanArray,
    val currentPlayer: Int,
    val winner: Int?,
) {
    val playerCount: Int get() = sides.size
    val isOver: Boolean get() = winner != null

    fun position(player: Int): Cell = cellOf(positions[player])
    fun wallsLeft(player: Int): Int = wallsLeft[player]

    val walls: List<Wall>
        get() = buildList {
            for (i in hWalls.indices) {
                if (hWalls[i]) add(Wall(i % Board.SLOTS, i / Board.SLOTS, Orientation.HORIZONTAL))
                if (vWalls[i]) add(Wall(i % Board.SLOTS, i / Board.SLOTS, Orientation.VERTICAL))
            }
        }

    fun hasWall(wall: Wall): Boolean {
        if (!wall.isOnBoard) return false
        val i = wall.y * Board.SLOTS + wall.x
        return if (wall.orientation == Orientation.HORIZONTAL) hWalls[i] else vWalls[i]
    }

    private fun h(x: Int, y: Int) =
        x in 0 until Board.SLOTS && y in 0 until Board.SLOTS && hWalls[y * Board.SLOTS + x]

    private fun v(x: Int, y: Int) =
        x in 0 until Board.SLOTS && y in 0 until Board.SLOTS && vWalls[y * Board.SLOTS + x]

    /** (x, y) karesinden [d] yönüne geçiş tahta kenarı veya duvar yüzünden kapalı mı? */
    fun isBlocked(x: Int, y: Int, d: Direction): Boolean {
        val nx = x + d.dx
        val ny = y + d.dy
        if (nx !in 0 until Board.SIZE || ny !in 0 until Board.SIZE) return true
        return when (d) {
            Direction.DOWN -> h(x, y) || h(x - 1, y)
            Direction.UP -> h(x, y - 1) || h(x - 1, y - 1)
            Direction.RIGHT -> v(x, y) || v(x, y - 1)
            Direction.LEFT -> v(x - 1, y) || v(x - 1, y - 1)
        }
    }

    fun isBlocked(c: Cell, d: Direction) = isBlocked(c.x, c.y, d)

    private fun isOccupied(c: Cell): Boolean {
        val idx = c.y * Board.SIZE + c.x
        return positions.any { it == idx }
    }

    /** Piyonun gidebileceği kareler; piyon üstünden atlama ve çapraz geçiş dahil. */
    fun legalSteps(player: Int = currentPlayer): List<Cell> {
        val from = position(player)
        val result = LinkedHashSet<Cell>()
        for (d in Direction.entries) {
            if (isBlocked(from, d)) continue
            val next = from + d
            if (!isOccupied(next)) {
                result += next
                continue
            }
            val behind = next + d
            if (!isBlocked(next, d) && !isOccupied(behind)) {
                result += behind
            } else {
                for (p in d.perpendicular) {
                    val diag = next + p
                    if (!isBlocked(next, p) && !isOccupied(diag)) result += diag
                }
            }
        }
        return result.toList()
    }

    /** Duvar geometrik olarak yerleşebilir mi (sınır, çakışma, kesişme)? Yol kontrolü yapmaz. */
    private fun fits(w: Wall): Boolean {
        if (!w.isOnBoard) return false
        val (x, y) = w
        return if (w.orientation == Orientation.HORIZONTAL) {
            !h(x, y) && !v(x, y) && !h(x - 1, y) && !h(x + 1, y)
        } else {
            !v(x, y) && !h(x, y) && !v(x, y - 1) && !v(x, y + 1)
        }
    }

    fun canPlaceWall(w: Wall, player: Int = currentPlayer): Boolean {
        if (isOver || wallsLeft[player] <= 0 || !fits(w)) return false
        val trial = withWall(w)
        return sides.indices.all { trial.shortestPathLength(it) >= 0 }
    }

    private fun withWall(w: Wall): GameState {
        val i = w.y * Board.SLOTS + w.x
        val nh = hWalls.copyOf()
        val nv = vWalls.copyOf()
        if (w.orientation == Orientation.HORIZONTAL) nh[i] = true else nv[i] = true
        return GameState(sides, positions, wallsLeft, nh, nv, currentPlayer, winner)
    }

    /** Hedefe en kısa mesafe (piyonlar yok sayılır); yol yoksa -1. */
    fun shortestPathLength(player: Int): Int {
        val path = bfs(player) ?: return -1
        return path.size - 1
    }

    /** Başlangıç karesi dahil hedefe giden en kısa yol; yol yoksa boş liste. */
    fun shortestPath(player: Int): List<Cell> = bfs(player) ?: emptyList()

    private fun bfs(player: Int): List<Cell>? {
        val side = sides[player]
        val n = Board.SIZE * Board.SIZE
        val parent = IntArray(n) { -2 }
        val queue = IntArray(n)
        var head = 0
        var tail = 0
        val start = positions[player]
        parent[start] = -1
        queue[tail++] = start
        while (head < tail) {
            val cur = queue[head++]
            val c = cellOf(cur)
            if (side.isGoal(c)) {
                val path = ArrayList<Cell>()
                var k = cur
                while (k != -1) {
                    path += cellOf(k)
                    k = parent[k]
                }
                path.reverse()
                return path
            }
            for (d in Direction.entries) {
                if (isBlocked(c.x, c.y, d)) continue
                val nk = (c.y + d.dy) * Board.SIZE + (c.x + d.dx)
                if (parent[nk] == -2) {
                    parent[nk] = cur
                    queue[tail++] = nk
                }
            }
        }
        return null
    }

    fun isLegal(move: Move): Boolean = !isOver && when (move) {
        is Move.Step -> move.to in legalSteps()
        is Move.PlaceWall -> canPlaceWall(move.wall)
    }

    fun legalMoves(): List<Move> {
        if (isOver) return emptyList()
        val moves = ArrayList<Move>()
        legalSteps().mapTo(moves) { Move.Step(it) }
        if (wallsLeft[currentPlayer] > 0) {
            for (y in 0 until Board.SLOTS) for (x in 0 until Board.SLOTS) {
                for (o in Orientation.entries) {
                    val w = Wall(x, y, o)
                    if (canPlaceWall(w)) moves += Move.PlaceWall(w)
                }
            }
        }
        return moves
    }

    private fun nextPlayer() = (currentPlayer + 1) % playerCount

    fun apply(move: Move): GameState {
        require(isLegal(move)) { "Geçersiz hamle: $move" }
        return when (move) {
            is Move.Step -> {
                val np = positions.copyOf()
                np[currentPlayer] = move.to.y * Board.SIZE + move.to.x
                val won = sides[currentPlayer].isGoal(move.to)
                GameState(
                    sides, np, wallsLeft, hWalls, vWalls,
                    if (won) currentPlayer else nextPlayer(),
                    if (won) currentPlayer else null,
                )
            }
            is Move.PlaceWall -> {
                val placed = withWall(move.wall)
                val nw = wallsLeft.copyOf()
                nw[currentPlayer]--
                GameState(sides, positions, nw, placed.hWalls, placed.vWalls, nextPlayer(), null)
            }
        }
    }

    /** Oyuncu hiç hamle yapamıyorsa (4 kişilik oyunda nadiren olur) sıra geçer. */
    fun skipTurn(): GameState =
        GameState(sides, positions, wallsLeft, hWalls, vWalls, nextPlayer(), winner)

    companion object {
        private fun cellOf(i: Int) = Cell(i % Board.SIZE, i / Board.SIZE)

        fun newGame(playerCount: Int): GameState {
            require(playerCount == 2 || playerCount == 4) { "Oyuncu sayısı 2 veya 4 olmalı" }
            val sides = if (playerCount == 2) {
                listOf(Side.SOUTH, Side.NORTH)
            } else {
                // Saat yönünde sıra: alt, sol, üst, sağ
                listOf(Side.SOUTH, Side.WEST, Side.NORTH, Side.EAST)
            }
            val walls = if (playerCount == 2) 10 else 5
            return GameState(
                sides = sides,
                positions = IntArray(playerCount) { sides[it].start.let { c -> c.y * Board.SIZE + c.x } },
                wallsLeft = IntArray(playerCount) { walls },
                hWalls = BooleanArray(Board.SLOTS * Board.SLOTS),
                vWalls = BooleanArray(Board.SLOTS * Board.SLOTS),
                currentPlayer = 0,
                winner = null,
            )
        }
    }
}
