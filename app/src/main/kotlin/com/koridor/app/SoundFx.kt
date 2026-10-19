package com.koridor.app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class Sfx { STEP, JUMP, WALL, INVALID, WIN, LOSE, COIN, CLICK }

/**
 * Ses efektleri. Ses dosyası taşımamak için sesler ilk açılışta
 * kod ile üretilip önbelleğe WAV olarak yazılır, sonra SoundPool ile çalınır.
 */
class SoundFx(context: Context) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids = HashMap<Sfx, Int>()

    init {
        val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
        for (s in Sfx.entries) {
            val file = File(dir, "${s.name.lowercase()}_v1.wav")
            if (!file.exists()) writeWav(file, synth(s))
            ids[s] = pool.load(file.path, 1)
        }
    }

    fun play(s: Sfx, volume: Float = 1f) {
        val id = ids[s] ?: return
        pool.play(id, volume, volume, 1, 0, 1f)
    }

    fun release() = pool.release()

    private fun synth(s: Sfx): FloatArray = when (s) {
        Sfx.STEP -> knock(620.0, 0.09, 38.0)
        Sfx.JUMP -> concat(knock(520.0, 0.07, 45.0), knock(780.0, 0.09, 40.0))
        Sfx.WALL -> mix(knock(150.0, 0.2, 18.0, noise = 0.5), knock(300.0, 0.08, 40.0, noise = 0.2))
        Sfx.INVALID -> buzz(120.0, 0.16)
        Sfx.WIN -> concat(*listOf(523.25, 659.25, 783.99, 1046.5).map { tone(it, 0.13, 9.0) }.toTypedArray(), tone(1046.5, 0.35, 5.0))
        Sfx.LOSE -> concat(*listOf(392.0, 329.63, 261.63).map { tone(it, 0.16, 7.0) }.toTypedArray(), tone(196.0, 0.4, 4.0))
        Sfx.COIN -> concat(tone(987.77, 0.07, 14.0), tone(1318.5, 0.25, 8.0))
        Sfx.CLICK -> knock(1200.0, 0.04, 80.0, noise = 0.1)
    }

    private companion object {
        const val RATE = 22050

        /** Tahta tıkırtısı: hızla sönen sinüs + biraz gürültü. */
        fun knock(freq: Double, seconds: Double, decay: Double, noise: Double = 0.35): FloatArray {
            val rnd = Random(freq.toInt())
            return FloatArray((RATE * seconds).toInt()) { i ->
                val t = i.toDouble() / RATE
                val env = exp(-t * decay)
                val v = sin(2 * PI * freq * t) * (1 - noise) + (rnd.nextDouble() * 2 - 1) * noise * exp(-t * decay * 3)
                (v * env * 0.9).toFloat()
            }
        }

        fun tone(freq: Double, seconds: Double, decay: Double): FloatArray =
            FloatArray((RATE * seconds).toInt()) { i ->
                val t = i.toDouble() / RATE
                val attack = (t / 0.008).coerceAtMost(1.0)
                val v = sin(2 * PI * freq * t) * 0.7 + sin(4 * PI * freq * t) * 0.15
                (v * attack * exp(-t * decay) * 0.6).toFloat()
            }

        fun buzz(freq: Double, seconds: Double): FloatArray =
            FloatArray((RATE * seconds).toInt()) { i ->
                val t = i.toDouble() / RATE
                val square = if (sin(2 * PI * freq * t) >= 0) 1.0 else -1.0
                (square * 0.25 * exp(-t * 10)).toFloat()
            }

        fun concat(vararg parts: FloatArray): FloatArray {
            val out = FloatArray(parts.sumOf { it.size })
            var pos = 0
            for (p in parts) {
                p.copyInto(out, pos)
                pos += p.size
            }
            return out
        }

        fun mix(a: FloatArray, b: FloatArray): FloatArray =
            FloatArray(maxOf(a.size, b.size)) { i -> (a.getOrElse(i) { 0f } + b.getOrElse(i) { 0f }).coerceIn(-1f, 1f) }

        fun writeWav(file: File, samples: FloatArray) {
            val dataSize = samples.size * 2
            val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
            buf.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVE".toByteArray())
            buf.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
                .putInt(RATE).putInt(RATE * 2).putShort(2).putShort(16)
            buf.put("data".toByteArray()).putInt(dataSize)
            for (s in samples) buf.putShort((s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
            FileOutputStream(file).use { it.write(buf.array()) }
        }
    }
}
