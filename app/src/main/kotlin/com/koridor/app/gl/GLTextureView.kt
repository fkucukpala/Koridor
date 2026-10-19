package com.koridor.app.gl

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.TextureView

/**
 * GLSurfaceView yerine TextureView üzerinde çalışan basit GL ES 2.0 görünümü.
 * TextureView, Compose arayüzünün içinde diğer öğelerle doğru katmanlanır.
 * Kendi iş parçacığında yaklaşık 60 FPS ile sürekli çizim yapar.
 */
open class GLTextureView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {

    private var renderer: GLSurfaceView.Renderer? = null
    private var thread: RenderThread? = null
    private var paused = false

    init {
        surfaceTextureListener = this
        isOpaque = true
    }

    fun setRenderer(r: GLSurfaceView.Renderer) {
        renderer = r
    }

    fun onResume() {
        paused = false
        thread?.paused = false
    }

    fun onPause() {
        paused = true
        thread?.paused = true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        val r = renderer ?: return
        thread = RenderThread(surface, r, width, height).also {
            it.paused = paused
            it.start()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        thread?.resize(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        thread?.finish()
        thread = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    private class RenderThread(
        private val texture: SurfaceTexture,
        private val renderer: GLSurfaceView.Renderer,
        @Volatile private var width: Int,
        @Volatile private var height: Int,
    ) : Thread("KoridorGL") {
        @Volatile var paused = false
        @Volatile private var running = true
        @Volatile private var sizeChanged = true

        private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

        fun resize(w: Int, h: Int) {
            width = w
            height = h
            sizeChanged = true
        }

        fun finish() {
            running = false
            join(500)
        }

        override fun run() {
            try {
                initEgl()
                renderer.onSurfaceCreated(null, null)
                while (running) {
                    val start = System.nanoTime()
                    if (!paused) {
                        if (sizeChanged) {
                            sizeChanged = false
                            renderer.onSurfaceChanged(null, width, height)
                        }
                        renderer.onDrawFrame(null)
                        EGL14.eglSwapBuffers(display, eglSurface)
                    }
                    val elapsedMs = (System.nanoTime() - start) / 1_000_000
                    val wait = (if (paused) 100 else FRAME_MS) - elapsedMs
                    if (wait > 0) sleep(wait)
                }
            } catch (e: InterruptedException) {
                // Görünüm kapanıyor.
            } catch (e: RuntimeException) {
                Log.e("GLTextureView", "GL çizim hatası", e)
            } finally {
                releaseEgl()
            }
        }

        private fun initEgl() {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            check(EGL14.eglInitialize(display, version, 0, version, 1)) { "EGL başlatılamadı" }
            val config = chooseConfig(4) ?: chooseConfig(0) ?: error("Uygun EGL yapılandırması yok")
            eglContext = EGL14.eglCreateContext(
                display, config, EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0,
            )
            eglSurface = EGL14.eglCreateWindowSurface(display, config, texture, intArrayOf(EGL14.EGL_NONE), 0)
            check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)) { "EGL bağlamı etkinleştirilemedi" }
        }

        /** [samples] > 0 ise kenar yumuşatma (MSAA) ister; desteklenmezse null döner. */
        private fun chooseConfig(samples: Int): EGLConfig? {
            val attrs = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 16,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SAMPLE_BUFFERS, if (samples > 0) 1 else 0,
                EGL14.EGL_SAMPLES, samples,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            if (!EGL14.eglChooseConfig(display, attrs, 0, configs, 0, 1, count, 0) || count[0] == 0) return null
            return configs[0]
        }

        private fun releaseEgl() {
            if (display == EGL14.EGL_NO_DISPLAY) return
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext)
            // eglTerminate çağrılmaz: aynı ekran bağlantısını Android'in kendi çizicisi de kullanır.
            display = EGL14.EGL_NO_DISPLAY
        }

        private companion object {
            const val FRAME_MS = 16L
        }
    }
}
