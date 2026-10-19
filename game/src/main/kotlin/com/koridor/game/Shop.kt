package com.koridor.game

enum class ItemKind { BOARD, PAWN }

/**
 * Mağazadaki bir görünüm. [price] puanla, [adsRequired] kadar ödüllü reklam
 * izleyerek açılabilir; ikisi de 0 ise baştan açıktır.
 */
data class ShopItem(
    val id: String,
    val kind: ItemKind,
    val name: String,
    val price: Int,
    val adsRequired: Int,
) {
    val isFree: Boolean get() = price == 0 && adsRequired == 0
}

object Catalog {
    val items = listOf(
        ShopItem("board_walnut", ItemKind.BOARD, "Ceviz", 0, 0),
        ShopItem("board_grass", ItemKind.BOARD, "Çimen", 120, 2),
        ShopItem("board_marble", ItemKind.BOARD, "Mermer", 180, 3),
        ShopItem("board_ice", ItemKind.BOARD, "Buz", 220, 3),
        ShopItem("board_neon", ItemKind.BOARD, "Neon Gece", 300, 4),
        ShopItem("pawn_classic", ItemKind.PAWN, "Klasik", 0, 0),
        ShopItem("pawn_checker", ItemKind.PAWN, "Dama Taşı", 100, 2),
        ShopItem("pawn_rook", ItemKind.PAWN, "Kale", 160, 3),
        ShopItem("pawn_crystal", ItemKind.PAWN, "Kristal", 250, 4),
    )

    val defaultBoard = items.first { it.kind == ItemKind.BOARD && it.isFree }
    val defaultPawn = items.first { it.kind == ItemKind.PAWN && it.isFree }

    fun byId(id: String): ShopItem? = items.firstOrNull { it.id == id }
    fun ofKind(kind: ItemKind) = items.filter { it.kind == kind }
}

/** Kalıcı depolama soyutlaması; Android'de SharedPreferences ile karşılanır. */
interface KeyValueStore {
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
}

class InMemoryStore : KeyValueStore {
    private val map = HashMap<String, Any>()
    override fun getInt(key: String, default: Int) = map[key] as? Int ?: default
    override fun putInt(key: String, value: Int) { map[key] = value }
    override fun getString(key: String, default: String) = map[key] as? String ?: default
    override fun putString(key: String, value: String) { map[key] = value }
}

/** Bitmiş bir oyunun puan karşılığı. */
object Rewards {
    fun forGame(config: Map<Int, Difficulty>, playerCount: Int, winner: Int): Int {
        val humanWon = winner !in config
        val humans = (0 until playerCount).count { it !in config }
        return when {
            humans == 0 -> 0
            config.isEmpty() -> if (playerCount == 4) 15 else 10 // arkadaşla oyun: kazanan fark etmez
            !humanWon -> 5 // kaybetsen de az bir puan
            playerCount == 4 -> 40
            else -> when (config.values.maxOrNull()) {
                Difficulty.EASY -> 15
                Difficulty.MEDIUM -> 30
                Difficulty.HARD -> 60
                null -> 0
            }
        }
    }
}

/** Puan, açılmış görünümler, reklam ilerlemesi ve seçili görünümler. */
class Wallet(private val store: KeyValueStore) {

    val points: Int get() = store.getInt(KEY_POINTS, 0)

    fun addPoints(amount: Int) {
        if (amount > 0) store.putInt(KEY_POINTS, points + amount)
    }

    fun isUnlocked(item: ShopItem): Boolean =
        item.isFree || item.id in unlockedIds()

    fun adsWatched(item: ShopItem): Int = store.getInt(KEY_ADS + item.id, 0)

    fun canAfford(item: ShopItem) = points >= item.price

    /** Puanla satın alır; başarılıysa true. */
    fun buy(item: ShopItem): Boolean {
        if (isUnlocked(item) || !canAfford(item)) return false
        store.putInt(KEY_POINTS, points - item.price)
        unlock(item)
        return true
    }

    /** Ödüllü reklam izlendi; yeterli sayıya ulaşınca görünüm açılır. Açıldıysa true. */
    fun recordAd(item: ShopItem): Boolean {
        if (isUnlocked(item)) return false
        val watched = adsWatched(item) + 1
        store.putInt(KEY_ADS + item.id, watched)
        if (watched >= item.adsRequired) {
            unlock(item)
            return true
        }
        return false
    }

    fun selected(kind: ItemKind): ShopItem {
        val default = if (kind == ItemKind.BOARD) Catalog.defaultBoard else Catalog.defaultPawn
        val item = Catalog.byId(store.getString(KEY_SELECTED + kind.name, default.id))
        return if (item != null && item.kind == kind && isUnlocked(item)) item else default
    }

    fun select(item: ShopItem): Boolean {
        if (!isUnlocked(item)) return false
        store.putString(KEY_SELECTED + item.kind.name, item.id)
        return true
    }

    private fun unlockedIds(): Set<String> =
        store.getString(KEY_UNLOCKED, "").split(',').filter { it.isNotEmpty() }.toSet()

    private fun unlock(item: ShopItem) {
        store.putString(KEY_UNLOCKED, (unlockedIds() + item.id).joinToString(","))
    }

    private companion object {
        const val KEY_POINTS = "points"
        const val KEY_UNLOCKED = "unlocked"
        const val KEY_ADS = "ads_"
        const val KEY_SELECTED = "selected_"
    }
}
