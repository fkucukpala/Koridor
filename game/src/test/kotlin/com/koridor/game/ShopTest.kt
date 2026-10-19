package com.koridor.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopTest {

    private val grass = Catalog.byId("board_grass")!!

    @Test
    fun freeItemsAreUnlockedAndSelectedByDefault() {
        val w = Wallet(InMemoryStore())
        assertEquals(0, w.points)
        assertTrue(w.isUnlocked(Catalog.defaultBoard))
        assertEquals(Catalog.defaultBoard, w.selected(ItemKind.BOARD))
        assertEquals(Catalog.defaultPawn, w.selected(ItemKind.PAWN))
        assertFalse(w.select(grass))
    }

    @Test
    fun buyingNeedsEnoughPoints() {
        val w = Wallet(InMemoryStore())
        w.addPoints(grass.price - 1)
        assertFalse(w.buy(grass))
        w.addPoints(1)
        assertTrue(w.buy(grass))
        assertEquals(0, w.points)
        assertTrue(w.isUnlocked(grass))
        assertFalse("ikinci kez satın alınmamalı", w.buy(grass))
        assertTrue(w.select(grass))
        assertEquals(grass, w.selected(ItemKind.BOARD))
    }

    @Test
    fun watchingAdsUnlocks() {
        val w = Wallet(InMemoryStore())
        repeat(grass.adsRequired - 1) { assertFalse(w.recordAd(grass)) }
        assertEquals(grass.adsRequired - 1, w.adsWatched(grass))
        assertFalse(w.isUnlocked(grass))
        assertTrue(w.recordAd(grass))
        assertTrue(w.isUnlocked(grass))
        assertEquals(0, w.points)
    }

    @Test
    fun stateIsPersistedInStore() {
        val store = InMemoryStore()
        Wallet(store).apply { addPoints(500); buy(grass); select(grass) }
        val again = Wallet(store)
        assertEquals(500 - grass.price, again.points)
        assertEquals(grass, again.selected(ItemKind.BOARD))
    }

    @Test
    fun rewards() {
        val vsHard = mapOf(1 to Difficulty.HARD)
        assertEquals(60, Rewards.forGame(vsHard, 2, winner = 0))
        assertEquals(5, Rewards.forGame(vsHard, 2, winner = 1))
        assertEquals(15, Rewards.forGame(mapOf(1 to Difficulty.EASY), 2, 0))
        assertEquals(10, Rewards.forGame(emptyMap(), 2, 1))
        assertEquals(40, Rewards.forGame((1..3).associateWith { Difficulty.MEDIUM }, 4, 0))
    }

    @Test
    fun catalogIdsAreUnique() {
        assertEquals(Catalog.items.size, Catalog.items.map { it.id }.toSet().size)
    }
}
