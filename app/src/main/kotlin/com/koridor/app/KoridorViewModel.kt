package com.koridor.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koridor.app.gl.OrbitCamera
import com.koridor.game.Ai
import com.koridor.game.Board
import com.koridor.game.Cell
import com.koridor.game.Difficulty
import com.koridor.game.GameState
import com.koridor.game.Move
import com.koridor.game.Orientation
import com.koridor.game.Wall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.roundToInt

enum class Screen { MENU, GAME, RULES }

enum class InputMode { MOVE, WALL }

/** Hangi oyuncunun bilgisayar olduğunu belirler; null insan demektir. */
data class GameConfig(val playerCount: Int, val computers: Map<Int, Difficulty> = emptyMap())

/** Hem 2D hem 3D tahtanın çizdiği her şey. */
data class BoardScene(
    val state: GameState,
    val highlights: List<Cell>,
    val preview: Wall?,
    val previewValid: Boolean,
)

class KoridorViewModel : ViewModel() {

    var screen by mutableStateOf(Screen.MENU)
        private set

    var config by mutableStateOf(GameConfig(2))
        private set

    var state by mutableStateOf(GameState.newGame(2))
        private set

    var mode by mutableStateOf(InputMode.MOVE)
        private set

    var orientation by mutableStateOf(Orientation.HORIZONTAL)
        private set

    var preview by mutableStateOf<Wall?>(null)
        private set

    var use3d by mutableStateOf(true)
        private set

    var aiThinking by mutableStateOf(false)
        private set

    /** Kamera açısı ekran döndürmelerinde kaybolmasın diye burada tutulur. */
    val camera = OrbitCamera()

    private val history = ArrayList<GameState>()
    private var aiJob: Job? = null

    val isHumanTurn: Boolean
        get() = !state.isOver && state.currentPlayer !in config.computers

    val scene: BoardScene
        get() {
            val p = preview
            return BoardScene(
                state = state,
                highlights = if (isHumanTurn && mode == InputMode.MOVE) state.legalSteps() else emptyList(),
                preview = if (isHumanTurn && mode == InputMode.WALL) p else null,
                previewValid = p != null && state.canPlaceWall(p),
            )
        }

    fun openMenu() {
        aiJob?.cancel()
        aiThinking = false
        screen = Screen.MENU
    }

    fun openRules() {
        screen = Screen.RULES
    }

    fun startGame(newConfig: GameConfig) {
        config = newConfig
        restart()
        screen = Screen.GAME
    }

    fun restart() {
        aiJob?.cancel()
        aiThinking = false
        history.clear()
        state = GameState.newGame(config.playerCount)
        mode = InputMode.MOVE
        preview = null
        camera.reset()
        maybeRunAi()
    }

    fun toggle3d() {
        use3d = !use3d
    }

    fun selectMode(m: InputMode) {
        mode = m
        if (m == InputMode.MOVE) preview = null
    }

    fun toggleOrientation() {
        orientation = orientation.toggled()
        preview = preview?.copy(orientation = orientation)
    }

    /** Tahta koordinatında (0..9 arası, kare birimi) dokunma. */
    fun onBoardTap(u: Float, v: Float) {
        if (!isHumanTurn) return
        if (u < 0f || v < 0f || u > Board.SIZE || v > Board.SIZE) return
        when (mode) {
            InputMode.MOVE -> {
                val cell = Cell(floor(u).toInt(), floor(v).toInt())
                if (cell in state.legalSteps()) play(Move.Step(cell))
            }
            InputMode.WALL -> {
                val wall = Wall(
                    (u.roundToInt() - 1).coerceIn(0, Board.SLOTS - 1),
                    (v.roundToInt() - 1).coerceIn(0, Board.SLOTS - 1),
                    orientation,
                )
                if (wall == preview && state.canPlaceWall(wall)) {
                    confirmWall()
                } else {
                    preview = wall
                }
            }
        }
    }

    fun confirmWall() {
        val w = preview ?: return
        if (isHumanTurn && state.canPlaceWall(w)) {
            preview = null
            play(Move.PlaceWall(w))
        }
    }

    val canUndo: Boolean get() = history.isNotEmpty() && !aiThinking

    fun undo() {
        if (!canUndo) return
        aiJob?.cancel()
        var s = history.removeAt(history.lastIndex)
        // Bilgisayara karşı oynarken insanın son hamlesine kadar geri git.
        while (s.currentPlayer in config.computers && history.isNotEmpty()) {
            s = history.removeAt(history.lastIndex)
        }
        state = s
        preview = null
        maybeRunAi()
    }

    private fun play(move: Move) {
        history += state
        state = state.apply(move)
        if (mode == InputMode.WALL && state.wallsLeft(state.currentPlayer) == 0) mode = InputMode.MOVE
        advanceIfStuck()
        maybeRunAi()
    }

    private fun advanceIfStuck() {
        var guard = 0
        while (!state.isOver && state.legalMoves().isEmpty() && guard++ < state.playerCount) {
            state = state.skipTurn()
        }
    }

    private fun maybeRunAi() {
        val difficulty = config.computers[state.currentPlayer] ?: return
        if (state.isOver) return
        val snapshot = state
        aiThinking = true
        aiJob = viewModelScope.launch {
            delay(450)
            val move = withContext(Dispatchers.Default) { Ai(difficulty).chooseMove(snapshot) }
            aiThinking = false
            if (state !== snapshot) return@launch
            if (move == null) {
                state = state.skipTurn()
                maybeRunAi()
            } else {
                play(move)
            }
        }
    }

    /** Testlerde ve hızlı kurulumlarda hamleleri doğrudan uygulamak için. */
    fun playForTest(move: Move) = play(move)

    fun setUse3dForTest(value: Boolean) {
        use3d = value
    }

    fun setModeForTest(m: InputMode, wall: Wall? = null) {
        mode = m
        preview = wall
        wall?.let { orientation = it.orientation }
    }
}
