package com.koridor.app.gl

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.koridor.app.BoardScene

/**
 * 3D tahta görünümü. Tek parmakla sürükleyince tahta etrafında döner,
 * iki parmakla yakınlaşır/uzaklaşır, dokununca tahtadaki noktayı bildirir.
 */
@SuppressLint("ViewConstructor")
class BoardGLView(
    context: Context,
    private val camera: OrbitCamera,
) : GLTextureView(context) {

    private val renderer = BoardRenderer(camera)

    /** Tahta koordinatında (u, v) dokunma. */
    var onBoardTap: (Float, Float) -> Unit = { _, _ -> }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            camera.zoom(detector.scaleFactor)
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (e2.pointerCount > 1 || scaleDetector.isInProgress) return false
            val density = resources.displayMetrics.density
            camera.rotate(dx / density * 0.45f, -dy / density * 0.35f)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val hit = camera.pick(e.x, e.y, width, height, BoardRenderer.TILE_HEIGHT) ?: return false
            onBoardTap(hit[0], hit[1])
            return true
        }
    })

    init {
        setRenderer(renderer)
    }

    fun setScene(scene: BoardScene) {
        renderer.scene = scene
    }

    /** Emülatörde ya da fareli cihazda tekerlekle yakınlaştırma. */
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL) {
            val scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            if (scroll != 0f) {
                camera.zoomSmooth(if (scroll > 0) 1.15f else 1f / 1.15f)
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }
}
