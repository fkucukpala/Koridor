package com.koridor.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameStateTest {

    private fun GameState.step(x: Int, y: Int) = apply(Move.Step(Cell(x, y)))
    private fun GameState.wall(x: Int, y: Int, o: Orientation) = apply(Move.PlaceWall(Wall(x, y, o)))

    @Test
    fun newGameHasPlayersOnStartCells() {
        val g = GameState.newGame(2)
        assertEquals(Cell(4, 8), g.position(0))
        assertEquals(Cell(4, 0), g.position(1))
        assertEquals(10, g.wallsLeft(0))
        assertEquals(8, g.shortestPathLength(0))

        val g4 = GameState.newGame(4)
        assertEquals(5, g4.wallsLeft(3))
        assertEquals(Cell(0, 4), g4.position(1))
    }

    @Test
    fun stepsFromStart() {
        val g = GameState.newGame(2)
        assertEquals(setOf(Cell(4, 7), Cell(3, 8), Cell(5, 8)), g.legalSteps().toSet())
    }

    @Test
    fun wallBlocksStep() {
        val g = GameState.newGame(2).wall(4, 7, Orientation.HORIZONTAL)
        // Sıra kuzeyde; güneyin önü kapalı olmalı.
        assertFalse(Cell(4, 7) in g.legalSteps(0))
        assertTrue(g.isBlocked(5, 8, Direction.UP))
        assertFalse(g.isBlocked(3, 8, Direction.UP))
        assertEquals(9, g.wallsLeft(0))
    }

    @Test
    fun straightJumpOverPawn() {
        var g = GameState.newGame(2)
        g = g.step(4, 7).step(4, 1).step(4, 6).step(4, 2).step(4, 5).step(4, 3)
        // Güney (4,5), kuzey (4,3); güney (4,4)'e gelince kuzey üstünden atlayabilmeli.
        g = g.step(4, 4)
        assertTrue(Cell(4, 5) in g.legalSteps())
        assertTrue(Cell(4, 3) !in g.legalSteps())
        // Kuzey (3,3)'e kaçar, güney (4,3)'e gelir: kuzey yan taraftan (5,3)'e atlayabilir.
        g = g.step(3, 3).step(4, 3)
        assertTrue(Cell(5, 3) in g.legalSteps())
        assertTrue(Cell(4, 3) !in g.legalSteps())
    }

    @Test
    fun diagonalWhenJumpBlockedByWall() {
        var g = GameState.newGame(2)
        g = g.step(4, 7).step(4, 1).step(4, 6).step(4, 2).step(4, 5).step(4, 3).step(4, 4)
        // Güney (4,4), kuzey (4,3). Kuzey kendi arkasına duvar koyar.
        g = g.wall(4, 2, Orientation.HORIZONTAL)
        val steps = g.legalSteps().toSet()
        assertEquals(setOf(Cell(3, 3), Cell(5, 3), Cell(3, 4), Cell(5, 4), Cell(4, 5)), steps)
    }

    @Test
    fun overlappingAndCrossingWallsRejected() {
        val g = GameState.newGame(2).wall(3, 3, Orientation.HORIZONTAL)
        assertFalse(g.canPlaceWall(Wall(3, 3, Orientation.HORIZONTAL)))
        assertFalse(g.canPlaceWall(Wall(3, 3, Orientation.VERTICAL)))
        assertFalse(g.canPlaceWall(Wall(2, 3, Orientation.HORIZONTAL)))
        assertFalse(g.canPlaceWall(Wall(4, 3, Orientation.HORIZONTAL)))
        assertTrue(g.canPlaceWall(Wall(5, 3, Orientation.HORIZONTAL)))
        assertTrue(g.canPlaceWall(Wall(3, 2, Orientation.VERTICAL)))
        assertFalse(g.canPlaceWall(Wall(8, 0, Orientation.VERTICAL)))
    }

    @Test
    fun cannotSealPlayerIn() {
        // Güney (4,8): üstü ve sağı kapatılır, sol taraf kapanırsa yol kalmaz.
        val g = GameState.newGame(2)
            .wall(3, 7, Orientation.HORIZONTAL)
            .wall(4, 7, Orientation.VERTICAL)
        assertTrue(g.shortestPathLength(0) > 0)
        assertFalse(g.canPlaceWall(Wall(2, 7, Orientation.VERTICAL)))
        assertTrue(g.canPlaceWall(Wall(1, 7, Orientation.VERTICAL)))
        for (m in g.legalMoves()) {
            if (m is Move.PlaceWall) {
                val after = g.apply(m)
                assertTrue(after.shortestPathLength(0) >= 0 && after.shortestPathLength(1) >= 0)
            }
        }
    }

    @Test
    fun noWallsLeftMeansNoWallMoves() {
        var g = GameState.newGame(4)
        // Her oyuncu sırayla 5 duvarını koyar; 8. sütun açık kaldığı için yollar kapanmaz.
        for (row in 0 until 5) for (p in 0 until 4) {
            g = g.wall(2 * p, row, Orientation.HORIZONTAL)
        }
        assertEquals(0, g.wallsLeft(0))
        while (g.currentPlayer != 0) g = g.apply(g.legalMoves().first { it is Move.Step })
        assertTrue(g.legalMoves().all { it is Move.Step })
        assertNotNull(g.legalMoves().firstOrNull())
    }

    @Test
    fun winningEndsGame() {
        var g = GameState.newGame(2)
        val moves = listOf(4 to 7, 3 to 0, 4 to 6, 2 to 0, 4 to 5, 3 to 0, 4 to 4, 2 to 0,
            4 to 3, 3 to 0, 4 to 2, 2 to 0, 4 to 1, 3 to 0)
        for ((x, y) in moves) g = g.step(x, y)
        g = g.step(4, 0)
        assertEquals(0, g.winner)
        assertTrue(g.legalMoves().isEmpty())
    }

    @Test
    fun aiGamesTerminateWithWinner() {
        for (seed in 0 until 4) {
            var g = GameState.newGame(2)
            val ais = listOf(Ai(Difficulty.MEDIUM, Random(seed)), Ai(Difficulty.EASY, Random(seed + 100)))
            var turns = 0
            while (!g.isOver && turns < 400) {
                val m = ais[g.currentPlayer].chooseMove(g)
                g = if (m == null) g.skipTurn() else g.apply(m)
                turns++
            }
            assertTrue("oyun bitmedi (seed=$seed)", g.isOver)
        }
    }

    @Test
    fun fourPlayerAiGameTerminates() {
        var g = GameState.newGame(4)
        val ai = Ai(Difficulty.MEDIUM, Random(7))
        var turns = 0
        while (!g.isOver && turns < 800) {
            val m = ai.chooseMove(g)
            g = if (m == null) g.skipTurn() else g.apply(m)
            turns++
        }
        assertTrue(g.isOver)
    }

    @Test
    fun hardAiIsFastEnough() {
        var g = GameState.newGame(2)
        val ai = Ai(Difficulty.HARD, Random(1))
        var worst = 0L
        repeat(12) {
            if (g.isOver) return@repeat
            val t = System.nanoTime()
            val m = ai.chooseMove(g)!!
            worst = maxOf(worst, (System.nanoTime() - t) / 1_000_000)
            g = g.apply(m)
        }
        println("Zor yapay zekâ en uzun hamle süresi: $worst ms")
        assertTrue("çok yavaş: $worst ms", worst < 5000)
    }

    @Test
    fun hardAiBeatsEasyAi() {
        var hardWins = 0
        for (seed in 0 until 3) {
            var g = GameState.newGame(2)
            val ais = listOf(Ai(Difficulty.HARD, Random(seed)), Ai(Difficulty.EASY, Random(seed)))
            var turns = 0
            while (!g.isOver && turns < 400) {
                val m = ais[g.currentPlayer].chooseMove(g)
                g = if (m == null) g.skipTurn() else g.apply(m)
                turns++
            }
            if (g.winner == 0) hardWins++
        }
        assertTrue("zor yapay zekâ yalnızca $hardWins/3 kazandı", hardWins >= 2)
    }
}
