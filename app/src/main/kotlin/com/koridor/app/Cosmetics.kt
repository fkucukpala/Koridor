package com.koridor.app

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.koridor.game.KeyValueStore

/**
 * Tahta görünümü: hem 2D hem 3D çizimde kullanılan renkler.
 * [tileAlt] verilirse kareler dama deseninde ([checker]) ya da şerit halinde boyanır.
 */
data class BoardTheme(
    val id: String,
    val name: String,
    val background: Color,
    val base: Color,
    val tile: Color,
    val tileAlt: Color = tile,
    val wall: Color,
    val checker: Boolean = false,
) {
    fun tileColor(x: Int, y: Int): Color = when {
        tileAlt == tile -> tile
        checker -> if ((x + y) % 2 == 0) tile else tileAlt
        else -> if (y % 2 == 0) tile else tileAlt
    }

    companion object {
        val WALNUT = BoardTheme(
            "board_walnut", "Ceviz",
            background = Color(0xFF1B1410), base = Color(0xFF3D2414),
            tile = Color(0xFF80502B), wall = Color(0xFFEDD199),
        )
        val GRASS = BoardTheme(
            "board_grass", "Çimen",
            background = Color(0xFF0E1A12), base = Color(0xFF5B3A1E),
            tile = Color(0xFF4F9A3C), tileAlt = Color(0xFF428533), wall = Color(0xFFF6F3EA),
        )
        val MARBLE = BoardTheme(
            "board_marble", "Mermer",
            background = Color(0xFF16181C), base = Color(0xFF2E3138),
            tile = Color(0xFFE4E1DA), tileAlt = Color(0xFFB9B5AD), wall = Color(0xFFD9A43A),
            checker = true,
        )
        val ICE = BoardTheme(
            "board_ice", "Buz",
            background = Color(0xFF0B1621), base = Color(0xFF7FA6C6),
            tile = Color(0xFFC6E7F7), tileAlt = Color(0xFFA9D6EE), wall = Color(0xFF12305A),
            checker = true,
        )
        val NEON = BoardTheme(
            "board_neon", "Neon Gece",
            background = Color(0xFF07060F), base = Color(0xFF15122B),
            tile = Color(0xFF2A2352), tileAlt = Color(0xFF231D45), wall = Color(0xFF35F0E2),
            checker = true,
        )

        val all = listOf(WALNUT, GRASS, MARBLE, ICE, NEON)
        fun forId(id: String?) = all.firstOrNull { it.id == id } ?: WALNUT
    }
}

enum class PawnStyle(val id: String, val title: String) {
    CLASSIC("pawn_classic", "Klasik"),
    CHECKER("pawn_checker", "Dama Taşı"),
    ROOK("pawn_rook", "Kale"),
    CRYSTAL("pawn_crystal", "Kristal");

    companion object {
        fun forId(id: String?) = entries.firstOrNull { it.id == id } ?: CLASSIC
    }
}

/** Oyun motorundaki [KeyValueStore]'u SharedPreferences ile karşılar (puan, açılan görünümler, ayarlar). */
class PrefsStore(context: Context) : KeyValueStore {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
    override fun getInt(key: String, default: Int) = prefs.getInt(key, default)
    override fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    override fun getString(key: String, default: String) = prefs.getString(key, default) ?: default
    override fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
}

fun Color.rgb() = floatArrayOf(red, green, blue)
