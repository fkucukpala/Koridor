package com.koridor.game

import kotlin.random.Random

enum class Difficulty(val depth: Int) {
    EASY(1), MEDIUM(2), HARD(3)
}

/**
 * "Paranoyak" minimax: yapay zekâ kendi puanını en üst düzeye çıkarmaya,
 * diğer tüm oyuncular ise onu düşürmeye çalışır. Alfa-beta budaması ve
 * yalnızca rakip yollarını kesen duvarların denenmesiyle hızlı kalır.
 */
class Ai(
    private val difficulty: Difficulty,
    private val random: Random = Random.Default,
) {
    fun chooseMove(state: GameState): Move? {
        val me = state.currentPlayer
        val candidates = candidateMoves(state)
        if (candidates.isEmpty()) return null

        if (difficulty == Difficulty.EASY && random.nextFloat() < 0.5f) {
            // Kolay seviyede çoğu zaman sadece kendi yolunda ilerler.
            val path = state.shortestPath(me)
            if (path.size > 1) {
                val step = Move.Step(path[1])
                if (state.isLegal(step)) return step
            }
        }

        var best = Int.MIN_VALUE
        val bestMoves = ArrayList<Move>()
        var alpha = Int.MIN_VALUE + 1
        for (move in candidates) {
            val score = search(state.apply(move), difficulty.depth - 1, alpha, Int.MAX_VALUE, me)
            if (score > best) {
                best = score
                bestMoves.clear()
                bestMoves += move
            } else if (score == best) {
                bestMoves += move
            }
            if (best > alpha) alpha = best
        }
        // Eşit puanlarda piyon hamlesini tercih et, duvarları boşa harcama.
        val steps = bestMoves.filterIsInstance<Move.Step>()
        val pool = steps.ifEmpty { bestMoves }
        return pool[random.nextInt(pool.size)]
    }

    private fun search(state: GameState, depth: Int, alphaIn: Int, betaIn: Int, me: Int): Int {
        state.winner?.let { return if (it == me) WIN + depth else -WIN - depth }
        if (depth == 0) return evaluate(state, me)
        val moves = candidateMoves(state)
        if (moves.isEmpty()) return search(state.skipTurn(), depth - 1, alphaIn, betaIn, me)

        var alpha = alphaIn
        var beta = betaIn
        if (state.currentPlayer == me) {
            var value = Int.MIN_VALUE
            for (m in moves) {
                value = maxOf(value, search(state.apply(m), depth - 1, alpha, beta, me))
                alpha = maxOf(alpha, value)
                if (alpha >= beta) break
            }
            return value
        } else {
            var value = Int.MAX_VALUE
            for (m in moves) {
                value = minOf(value, search(state.apply(m), depth - 1, alpha, beta, me))
                beta = minOf(beta, value)
                if (alpha >= beta) break
            }
            return value
        }
    }

    private fun evaluate(state: GameState, me: Int): Int {
        val myDist = state.shortestPathLength(me)
        var oppDist = Int.MAX_VALUE
        var oppWalls = 0
        for (p in 0 until state.playerCount) {
            if (p == me) continue
            val d = state.shortestPathLength(p)
            if (d < oppDist) {
                oppDist = d
                oppWalls = state.wallsLeft(p)
            }
        }
        return (oppDist - myDist) * 10 + (state.wallsLeft(me) - oppWalls) * 3
    }

    /** Piyon adımları + rakiplerin en kısa yolunu kesen duvarlar. */
    private fun candidateMoves(state: GameState): List<Move> {
        if (state.isOver) return emptyList()
        val player = state.currentPlayer
        val moves = ArrayList<Move>()

        val myPath = state.shortestPath(player)
        val preferred = myPath.getOrNull(1)
        val steps = state.legalSteps()
        steps.sortedBy { if (it == preferred) 0 else 1 }.mapTo(moves) { Move.Step(it) }

        if (state.wallsLeft(player) > 0) {
            val walls = LinkedHashSet<Wall>()
            for (opp in 0 until state.playerCount) {
                if (opp == player) continue
                val path = state.shortestPath(opp)
                for (i in 0 until path.size - 1) {
                    blockingWalls(path[i], path[i + 1], walls)
                }
            }
            for (w in walls) {
                if (state.canPlaceWall(w)) moves += Move.PlaceWall(w)
            }
        }
        return moves
    }

    private fun blockingWalls(a: Cell, b: Cell, out: MutableSet<Wall>) {
        val h = Orientation.HORIZONTAL
        val v = Orientation.VERTICAL
        when {
            b.y == a.y + 1 -> { out += Wall(a.x, a.y, h); out += Wall(a.x - 1, a.y, h) }
            b.y == a.y - 1 -> { out += Wall(a.x, b.y, h); out += Wall(a.x - 1, b.y, h) }
            b.x == a.x + 1 -> { out += Wall(a.x, a.y, v); out += Wall(a.x, a.y - 1, v) }
            b.x == a.x - 1 -> { out += Wall(b.x, a.y, v); out += Wall(b.x, a.y - 1, v) }
        }
        out.removeAll { !it.isOnBoard }
    }

    private companion object {
        const val WIN = 100_000
    }
}
