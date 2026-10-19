package com.koridor.app.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Konum + normal içeren basit üçgen ağı. Tüm şekiller birim boyutta, merkezleri orijinde. */
class Mesh(vertices: FloatArray, indices: ShortArray) {
    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(vertices); position(0) }
    private val indexBuffer: ShortBuffer = ByteBuffer.allocateDirect(indices.size * 2)
        .order(ByteOrder.nativeOrder()).asShortBuffer().apply { put(indices); position(0) }
    private val indexCount = indices.size

    fun draw(positionAttr: Int, normalAttr: Int) {
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(positionAttr, 3, GLES20.GL_FLOAT, false, STRIDE, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionAttr)
        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(normalAttr, 3, GLES20.GL_FLOAT, false, STRIDE, vertexBuffer)
        GLES20.glEnableVertexAttribArray(normalAttr)
        indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    }

    companion object {
        private const val STRIDE = 6 * 4

        /** -0.5..0.5 küp. */
        fun box(): Mesh {
            val v = ArrayList<Float>()
            val idx = ArrayList<Short>()
            // Her yüz: normal, iki kenar vektörü
            val faces = arrayOf(
                floatArrayOf(0f, 0f, 1f, 1f, 0f, 0f, 0f, 1f, 0f),
                floatArrayOf(0f, 0f, -1f, -1f, 0f, 0f, 0f, 1f, 0f),
                floatArrayOf(1f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f),
                floatArrayOf(-1f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 0f),
                floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, -1f),
                floatArrayOf(0f, -1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f),
            )
            for (f in faces) {
                val base = (v.size / 6).toShort()
                for ((a, b) in listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f)) {
                    for (k in 0..2) v += 0.5f * f[k] + 0.5f * a * f[3 + k] + 0.5f * b * f[6 + k]
                    for (k in 0..2) v += f[k]
                }
                idx += listOf(base, (base + 1).toShort(), (base + 2).toShort(), base, (base + 2).toShort(), (base + 3).toShort())
            }
            return Mesh(v.toFloatArray(), idx.toShortArray())
        }

        /** Yarıçapı 1, yüksekliği 1 (y: -0.5..0.5) silindir. */
        fun cylinder(segments: Int = 32): Mesh {
            val v = ArrayList<Float>()
            val idx = ArrayList<Short>()
            fun vert(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float) {
                v += listOf(x, y, z, nx, ny, nz)
            }
            // Yan yüzey
            for (i in 0..segments) {
                val a = 2.0 * PI * i / segments
                val x = cos(a).toFloat()
                val z = sin(a).toFloat()
                vert(x, -0.5f, z, x, 0f, z)
                vert(x, 0.5f, z, x, 0f, z)
            }
            for (i in 0 until segments) {
                val b = (i * 2).toShort()
                idx += listOf(b, (b + 1).toShort(), (b + 3).toShort(), b, (b + 3).toShort(), (b + 2).toShort())
            }
            // Kapaklar
            for ((y, n) in listOf(0.5f to 1f, -0.5f to -1f)) {
                val center = (v.size / 6).toShort()
                vert(0f, y, 0f, 0f, n, 0f)
                for (i in 0..segments) {
                    val a = 2.0 * PI * i / segments
                    vert(cos(a).toFloat(), y, sin(a).toFloat(), 0f, n, 0f)
                }
                for (i in 0 until segments) {
                    val p = (center + 1 + i).toShort()
                    val q = (center + 2 + i).toShort()
                    if (n > 0) idx += listOf(center, q, p) else idx += listOf(center, p, q)
                }
            }
            return Mesh(v.toFloatArray(), idx.toShortArray())
        }

        /** Köşeleri eksenler üzerinde (±1) olan, düz yüzlü sekiz yüzlü (kristal). */
        fun octahedron(): Mesh {
            val v = ArrayList<Float>()
            val idx = ArrayList<Short>()
            val top = floatArrayOf(0f, 1f, 0f)
            val bottom = floatArrayOf(0f, -1f, 0f)
            val ring = listOf(
                floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 0f, 1f),
                floatArrayOf(-1f, 0f, 0f), floatArrayOf(0f, 0f, -1f),
            )
            for (i in 0 until 4) {
                val a = ring[i]
                val b = ring[(i + 1) % 4]
                for (tip in listOf(top, bottom)) {
                    val tri = if (tip === top) listOf(a, tip, b) else listOf(a, b, tip)
                    // Yüz normali: üç köşenin ortalaması (bu şekil için yüze diktir).
                    val n = FloatArray(3) { k -> (tri[0][k] + tri[1][k] + tri[2][k]) / 3f }
                    val len = kotlin.math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2])
                    val base = (v.size / 6).toShort()
                    for (p in tri) {
                        v += listOf(p[0], p[1], p[2], n[0] / len, n[1] / len, n[2] / len)
                    }
                    idx += listOf(base, (base + 1).toShort(), (base + 2).toShort())
                }
            }
            return Mesh(v.toFloatArray(), idx.toShortArray())
        }

        /** Yarıçapı 1 küre. */
        fun sphere(stacks: Int = 16, slices: Int = 32): Mesh {
            val v = ArrayList<Float>()
            val idx = ArrayList<Short>()
            for (i in 0..stacks) {
                val phi = PI * i / stacks
                for (j in 0..slices) {
                    val theta = 2.0 * PI * j / slices
                    val x = (sin(phi) * cos(theta)).toFloat()
                    val y = cos(phi).toFloat()
                    val z = (sin(phi) * sin(theta)).toFloat()
                    v += listOf(x, y, z, x, y, z)
                }
            }
            for (i in 0 until stacks) {
                for (j in 0 until slices) {
                    val a = (i * (slices + 1) + j).toShort()
                    val b = (a + slices + 1).toShort()
                    idx += listOf(a, (a + 1).toShort(), b, b, (a + 1).toShort(), (b + 1).toShort())
                }
            }
            return Mesh(v.toFloatArray(), idx.toShortArray())
        }
    }
}
