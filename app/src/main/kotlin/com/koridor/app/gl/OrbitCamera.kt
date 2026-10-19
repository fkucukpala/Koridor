package com.koridor.app.gl

import android.opengl.Matrix
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Tahtanın merkezine bakan, etrafında 360° dönebilen kamera.
 * Arayüz ve GL iş parçacığı birlikte kullandığı için alanlar volatile.
 */
class OrbitCamera {
    @Volatile var yaw = DEFAULT_YAW
    @Volatile var pitch = DEFAULT_PITCH
    @Volatile var distance = DEFAULT_DISTANCE

    fun reset() {
        yaw = DEFAULT_YAW
        pitch = DEFAULT_PITCH
        distance = DEFAULT_DISTANCE
    }

    fun topView() {
        pitch = MAX_PITCH
    }

    fun rotate(dYaw: Float, dPitch: Float) {
        var y = (yaw + dYaw) % 360f
        if (y < 0f) y += 360f
        yaw = y
        pitch = (pitch + dPitch).coerceIn(MIN_PITCH, MAX_PITCH)
    }

    fun zoom(factor: Float) {
        distance = (distance / factor).coerceIn(MIN_DISTANCE, MAX_DISTANCE)
    }

    fun eye(): FloatArray {
        val yr = Math.toRadians(yaw.toDouble())
        val pr = Math.toRadians(pitch.toDouble())
        val d = distance.toDouble()
        return floatArrayOf(
            (d * cos(pr) * sin(yr)).toFloat(),
            (d * sin(pr)).toFloat(),
            (d * cos(pr) * cos(yr)).toFloat(),
        )
    }

    fun viewMatrix(out: FloatArray) {
        val e = eye()
        // Tam tepeden bakarken "yukarı" vektörü bakış yönüyle çakışmasın diye yaw'a göre seçilir.
        val yr = Math.toRadians(yaw.toDouble())
        val upX = (-sin(yr) * 0.001).toFloat()
        val upZ = (-cos(yr) * 0.001).toFloat()
        Matrix.setLookAtM(out, 0, e[0], e[1], e[2], 0f, 0f, 0f, upX, 1f, upZ)
    }

    fun projectionMatrix(out: FloatArray, aspect: Float) {
        // Dikey ekranda tahtanın yanlardan taşmaması için yatay görüş açısı sabitlenir.
        val fovY = if (aspect < 1f) {
            Math.toDegrees(2.0 * atan(tan(Math.toRadians(HORIZONTAL_FOV / 2.0)) / aspect)).toFloat()
        } else {
            VERTICAL_FOV
        }
        Matrix.perspectiveM(out, 0, fovY, aspect, 0.5f, 100f)
    }

    /**
     * Ekrandaki (sx, sy) noktasından çıkan ışını y = [planeY] düzlemiyle kesiştirir.
     * Tahta koordinatı (u, v) döner; 0..9 aralığı tahtanın kendisidir.
     */
    fun pick(sx: Float, sy: Float, width: Int, height: Int, planeY: Float): FloatArray? {
        if (width == 0 || height == 0) return null
        val view = FloatArray(16)
        val proj = FloatArray(16)
        val vp = FloatArray(16)
        val inv = FloatArray(16)
        viewMatrix(view)
        projectionMatrix(proj, width.toFloat() / height)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
        if (!Matrix.invertM(inv, 0, vp, 0)) return null
        val nx = 2f * sx / width - 1f
        val ny = 1f - 2f * sy / height
        val near = FloatArray(4)
        val far = FloatArray(4)
        Matrix.multiplyMV(near, 0, inv, 0, floatArrayOf(nx, ny, -1f, 1f), 0)
        Matrix.multiplyMV(far, 0, inv, 0, floatArrayOf(nx, ny, 1f, 1f), 0)
        for (i in 0..2) {
            near[i] /= near[3]
            far[i] /= far[3]
        }
        val dy = far[1] - near[1]
        if (dy == 0f) return null
        val t = (planeY - near[1]) / dy
        if (t < 0f) return null
        val x = near[0] + t * (far[0] - near[0])
        val z = near[2] + t * (far[2] - near[2])
        return floatArrayOf(x + HALF, z + HALF)
    }

    companion object {
        const val HALF = 4.5f
        const val DEFAULT_YAW = 0f
        const val DEFAULT_PITCH = 58f
        const val DEFAULT_DISTANCE = 17f
        const val MIN_PITCH = 12f
        const val MAX_PITCH = 89.5f
        const val MIN_DISTANCE = 7f
        const val MAX_DISTANCE = 32f
        const val HORIZONTAL_FOV = 52.0
        const val VERTICAL_FOV = 42f
    }
}
