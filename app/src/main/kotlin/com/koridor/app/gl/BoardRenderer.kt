package com.koridor.app.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import com.koridor.app.BoardScene
import com.koridor.app.PawnStyle
import com.koridor.app.Players
import com.koridor.app.rgb
import com.koridor.game.Board
import com.koridor.game.Cell
import com.koridor.game.Orientation
import com.koridor.game.Side
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * Tahtayı OpenGL ES 2.0 ile çizer. Dünya koordinatında tahta merkezi orijindedir,
 * bir kare 1 birimdir; kare (x, y)'nin merkezi (x - 4, *, y - 4) noktasıdır.
 */
class BoardRenderer(private val camera: OrbitCamera) : GLSurfaceView.Renderer {

    @Volatile var scene: BoardScene? = null

    private lateinit var shader: Shader
    private lateinit var box: Mesh
    private lateinit var cylinder: Mesh
    private lateinit var sphere: Mesh
    private lateinit var octahedron: Mesh

    private val view = FloatArray(16)
    private val proj = FloatArray(16)
    private val vp = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)
    private var eye = FloatArray(3)
    private var aspect = 1f

    /** Piyonların ekranda görünen (animasyonlu) konumları: x, z çiftleri. */
    private var shown = FloatArray(0)
    private var lastFrame = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        shader = Shader()
        box = Mesh.box()
        cylinder = Mesh.cylinder()
        sphere = Mesh.sphere()
        octahedron = Mesh.octahedron()
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspect = if (height == 0) 1f else width.toFloat() / height
    }

    override fun onDrawFrame(gl: GL10?) {
        val s = scene
        val bg = s?.theme?.background
        if (bg != null) GLES20.glClearColor(bg.red, bg.green, bg.blue, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (s == null) return

        camera.viewMatrix(view)
        camera.projectionMatrix(proj, aspect)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
        eye = camera.eye()

        GLES20.glUseProgram(shader.program)
        GLES20.glUniform3f(shader.uLightDir, 0.35f, 1f, 0.55f)
        GLES20.glUniform3f(shader.uEye, eye[0], eye[1], eye[2])

        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrame == 0L) 0f else min((now - lastFrame) / 1000f, 0.1f)
        lastFrame = now

        drawBoard(s)
        drawWalls(s)
        drawPawns(s, dt, now)
        drawTransparent(s, now)
    }

    private fun drawBoard(s: BoardScene) {
        // Taban
        drawBox(0f, -0.2f, 0f, 10.1f, 0.4f, 10.1f, s.theme.base.rgb())

        // Hedef kenarları: her oyuncunun ulaşması gereken kenar onun renginde.
        val state = s.state
        for (p in 0 until state.playerCount) {
            val c = Players.rgb(state.playerCount, p)
            when (state.sides[p]) {
                Side.SOUTH -> drawBox(0f, 0.005f, -4.85f, 9.2f, 0.02f, 0.14f, c)
                Side.NORTH -> drawBox(0f, 0.005f, 4.85f, 9.2f, 0.02f, 0.14f, c)
                Side.WEST -> drawBox(4.85f, 0.005f, 0f, 0.14f, 0.02f, 9.2f, c)
                Side.EAST -> drawBox(-4.85f, 0.005f, 0f, 0.14f, 0.02f, 9.2f, c)
            }
        }

        // Kareler; gidilebilecek olanlar sıradaki oyuncunun rengine boyanır.
        val highlightColor = Players.rgb(state.playerCount, state.currentPlayer)
        val highlighted = HashSet(s.highlights)
        for (y in 0 until Board.SIZE) for (x in 0 until Board.SIZE) {
            val isHi = Cell(x, y) in highlighted
            val tile = s.theme.tileColor(x, y).rgb()
            val color = if (isHi) mix(tile, highlightColor, 0.55f) else tile
            drawBox(x - 4f, TILE_HEIGHT / 2f, y - 4f, TILE_SIZE, TILE_HEIGHT, TILE_SIZE, color)
        }
    }

    private fun drawWalls(s: BoardScene) {
        val wallColor = s.theme.wall.rgb()
        for (w in s.state.walls) drawWall(w.x, w.y, w.orientation, wallColor)

        // Kalan duvarlar, oyuncunun kenarında dik olarak dizilir.
        val state = s.state
        for (p in 0 until state.playerCount) {
            val n = state.wallsLeft(p)
            for (i in 0 until n) {
                val offset = (i - (n - 1) / 2f) * 0.42f
                val c = mix(wallColor, Players.rgb(state.playerCount, p), 0.18f)
                when (state.sides[p]) {
                    Side.SOUTH -> drawBox(offset, WALL_HEIGHT / 2f, STORE, WALL_THICK, WALL_HEIGHT, WALL_LEN, c)
                    Side.NORTH -> drawBox(offset, WALL_HEIGHT / 2f, -STORE, WALL_THICK, WALL_HEIGHT, WALL_LEN, c)
                    Side.WEST -> drawBox(-STORE, WALL_HEIGHT / 2f, offset, WALL_LEN, WALL_HEIGHT, WALL_THICK, c)
                    Side.EAST -> drawBox(STORE, WALL_HEIGHT / 2f, offset, WALL_LEN, WALL_HEIGHT, WALL_THICK, c)
                }
            }
        }
    }

    private fun drawWall(x: Int, y: Int, o: Orientation, color: FloatArray, alpha: Float = 1f) {
        val cx = x + 1f - 4.5f
        val cz = y + 1f - 4.5f
        val cy = TILE_HEIGHT + WALL_HEIGHT / 2f
        if (o == Orientation.HORIZONTAL) {
            drawBox(cx, cy, cz, WALL_LEN, WALL_HEIGHT, WALL_THICK, color, alpha)
        } else {
            drawBox(cx, cy, cz, WALL_THICK, WALL_HEIGHT, WALL_LEN, color, alpha)
        }
    }

    private fun drawPawns(s: BoardScene, dt: Float, now: Long) {
        val state = s.state
        if (shown.size != state.playerCount * 2) {
            shown = FloatArray(state.playerCount * 2) { i ->
                val c = state.position(i / 2)
                if (i % 2 == 0) c.x - 4f else c.y - 4f
            }
        }
        val k = min(1f, dt * 9f)
        for (p in 0 until state.playerCount) {
            val c = state.position(p)
            val tx = c.x - 4f
            val tz = c.y - 4f
            shown[p * 2] += (tx - shown[p * 2]) * k
            shown[p * 2 + 1] += (tz - shown[p * 2 + 1]) * k
            val x = shown[p * 2]
            val z = shown[p * 2 + 1]
            // Hareket ederken hafifçe zıplasın.
            val dist = abs(tx - x) + abs(tz - z)
            val hop = min(dist, 1f - min(dist, 1f)) * 0.9f
            val base = TILE_HEIGHT + hop
            val color = Players.rgb(state.playerCount, p)
            val isTurn = p == state.currentPlayer && !state.isOver
            val bob = if (isTurn) (sin(now / 260.0) * 0.04).toFloat() + 0.04f else 0f

            drawPawn(s.pawnStyle, x, base, z, color, bob, now, p)
        }
    }

    private fun drawPawn(style: PawnStyle, x: Float, base: Float, z: Float, color: FloatArray, bob: Float, now: Long, p: Int) {
        val dark = mix(color, BLACK, 0.3f)
        val light = mix(color, WHITE, 0.35f)
        when (style) {
            PawnStyle.CLASSIC -> {
                drawCylinder(x, base + 0.06f, z, 0.34f, 0.12f, dark)
                drawCylinder(x, base + 0.33f + bob, z, 0.22f, 0.42f, color)
                drawSphere(x, base + 0.70f + bob, z, 0.21f, color)
            }
            PawnStyle.CHECKER -> {
                // İki katlı dama taşı; üstte açık renkli yiv.
                drawCylinder(x, base + 0.07f, z, 0.38f, 0.14f, dark)
                drawCylinder(x, base + 0.21f + bob, z, 0.38f, 0.14f, color)
                drawCylinder(x, base + 0.285f + bob, z, 0.26f, 0.012f, light)
                drawCylinder(x, base + 0.292f + bob, z, 0.18f, 0.012f, color)
            }
            PawnStyle.ROOK -> {
                drawCylinder(x, base + 0.06f, z, 0.34f, 0.12f, dark)
                drawCylinder(x, base + 0.36f + bob, z, 0.23f, 0.48f, color)
                drawCylinder(x, base + 0.64f + bob, z, 0.3f, 0.1f, color)
                // Mazgallar
                for ((dx, dz) in listOf(0.17f to 0.17f, -0.17f to 0.17f, 0.17f to -0.17f, -0.17f to -0.17f)) {
                    drawBox(x + dx, base + 0.75f + bob, z + dz, 0.13f, 0.14f, 0.13f, light)
                }
            }
            PawnStyle.CRYSTAL -> {
                drawCylinder(x, base + 0.05f, z, 0.3f, 0.1f, dark)
                drawCylinder(x, base + 0.13f, z, 0.12f, 0.08f, light)
                // Yavaşça dönen sekiz yüzlü kristal; her oyuncunun kristali farklı açıda başlar.
                val spin = (now % 6000L) / 6000f * 360f + p * 30f
                setModel(x, base + 0.55f + bob, z, 0.27f, 0.4f, 0.27f, spin)
                draw(octahedron, mix(color, WHITE, 0.15f), 1f)
            }
        }
    }

    private fun drawTransparent(s: BoardScene, now: Long) {
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glDepthMask(false)
        val state = s.state
        if (!state.isOver) {
            // Sıradaki oyuncunun altında nabız gibi atan halka.
            val c = state.position(state.currentPlayer)
            val pulse = (0.35f + 0.25f * sin(now / 220.0)).toFloat()
            val i = state.currentPlayer * 2
            val x = if (i < shown.size) shown[i] else c.x - 4f
            val z = if (i + 1 < shown.size) shown[i + 1] else c.y - 4f
            drawCylinder(x, TILE_HEIGHT + 0.01f, z, 0.44f, 0.02f, WHITE, pulse)
        }
        s.preview?.let { w ->
            val color = if (s.previewValid) VALID else INVALID
            drawWall(w.x, w.y, w.orientation, color, 0.7f)
        }
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun drawBox(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, c: FloatArray, a: Float = 1f) {
        setModel(x, y, z, sx, sy, sz)
        draw(box, c, a)
    }

    private fun drawCylinder(x: Float, y: Float, z: Float, r: Float, h: Float, c: FloatArray, a: Float = 1f) {
        setModel(x, y, z, r, h, r)
        draw(cylinder, c, a)
    }

    private fun drawSphere(x: Float, y: Float, z: Float, r: Float, c: FloatArray) {
        setModel(x, y, z, r, r, r)
        draw(sphere, c, 1f)
    }

    private fun setModel(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, rotY: Float = 0f) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        if (rotY != 0f) Matrix.rotateM(model, 0, rotY, 0f, 1f, 0f)
        Matrix.scaleM(model, 0, sx, sy, sz)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
    }

    private fun draw(mesh: Mesh, c: FloatArray, a: Float) {
        GLES20.glUniformMatrix4fv(shader.uMvp, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(shader.uModel, 1, false, model, 0)
        GLES20.glUniform4f(shader.uColor, c[0], c[1], c[2], a)
        mesh.draw(shader.aPosition, shader.aNormal)
    }

    private fun mix(a: FloatArray, b: FloatArray, t: Float) =
        floatArrayOf(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t)

    companion object {
        /** Dokunma ışınının kesiştirildiği düzlem: karelerin üst yüzü. */
        const val TILE_HEIGHT = 0.14f
        private const val TILE_SIZE = 0.84f
        private const val WALL_HEIGHT = 0.5f
        private const val WALL_THICK = 0.13f
        private const val WALL_LEN = 1.84f
        private const val STORE = 5.95f

        private val VALID = floatArrayOf(0.30f, 0.95f, 0.45f)
        private val INVALID = floatArrayOf(0.95f, 0.25f, 0.25f)
        private val WHITE = floatArrayOf(1f, 1f, 1f)
        private val BLACK = floatArrayOf(0f, 0f, 0f)
    }
}
