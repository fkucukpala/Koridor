package com.koridor.app

import androidx.compose.ui.graphics.Color

/** Oyuncu renkleri; 3D çizici aynı değerleri [rgb] ile kullanır. */
object Players {
    val names = listOf("Kırmızı", "Mavi", "Yeşil", "Sarı")
    val colors = listOf(
        Color(0xFFE53935),
        Color(0xFF1E88E5),
        Color(0xFF43A047),
        Color(0xFFFDD835),
    )

    /** 4 kişilik oyunda sıra alt, sol, üst, sağ; renkler kenarlara göre sabit kalsın. */
    fun colorIndex(playerCount: Int, player: Int): Int =
        if (playerCount == 2) player else intArrayOf(0, 2, 1, 3)[player]

    fun color(playerCount: Int, player: Int) = colors[colorIndex(playerCount, player)]
    fun name(playerCount: Int, player: Int) = names[colorIndex(playerCount, player)]

    fun rgb(playerCount: Int, player: Int): FloatArray {
        val c = color(playerCount, player)
        return floatArrayOf(c.red, c.green, c.blue)
    }
}
