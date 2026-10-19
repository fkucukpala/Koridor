package com.koridor.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koridor.app.gl.OrbitCamera
import com.koridor.game.Ai
import com.koridor.game.Board
import com.koridor.game.Catalog
import com.koridor.game.Cell
import com.koridor.game.Difficulty
import com.koridor.game.GameState
import com.koridor.game.ItemKind
import com.koridor.game.Move
import com.koridor.game.Orientation
import com.koridor.game.Rewards
import com.koridor.game.ShopItem
import com.koridor.game.Wall
import com.koridor.game.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

enum class Screen { MENU, GAME, RULES, APPEARANCE }

enum class InputMode { MOVE, WALL }

/** Hangi oyuncunun bilgisayar olduğunu belirler; null insan demektir. */
data class GameConfig(val playerCount: Int, val computers: Map<Int, Difficulty> = emptyMap())

/** Hem 2D hem 3D tahtanın çizdiği her şey. */
data class BoardScene(
    val state: GameState,
    val highlights: List<Cell>,
    val preview: Wall?,
    val previewValid: Boolean,
    val theme: BoardTheme = BoardTheme.WALNUT,
    val pawnStyle: PawnStyle = PawnStyle.CLASSIC,
)

class KoridorViewModel(app: Application) : AndroidViewModel(app) {

    private val store = PrefsStore(app)
    private val wallet = Wallet(store)

    /** Çalınacak sesler; arayüz bunları dinleyip [SoundFx] ile çalar. */
    private val _sounds = MutableSharedFlow<Sfx>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val sounds: SharedFlow<Sfx> = _sounds

    var soundOn by mutableStateOf(store.getInt(KEY_SOUND, 1) == 1)
        private set

    var points by mutableIntStateOf(wallet.points)
        private set

    /** Mağaza durumu değişince (satın alma, reklam) önizlemelerin yenilenmesi için sayaç. */
    var shopVersion by mutableIntStateOf(0)
        private set

    var boardTheme by mutableStateOf(BoardTheme.forId(wallet.selected(ItemKind.BOARD).id))
        private set

    var pawnStyle by mutableStateOf(PawnStyle.forId(wallet.selected(ItemKind.PAWN).id))
        private set

    /** Son biten oyunda kazanılan puan; oyun sonu penceresinde gösterilir. */
    var lastReward by mutableIntStateOf(0)
        private set

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
                theme = boardTheme,
                pawnStyle = pawnStyle,
            )
        }

    private fun sound(s: Sfx) {
        if (soundOn) _sounds.tryEmit(s)
    }

    fun toggleSound() {
        soundOn = !soundOn
        store.putInt(KEY_SOUND, if (soundOn) 1 else 0)
        sound(Sfx.CLICK)
    }

    // ---- Mağaza ----

    fun isUnlocked(item: ShopItem): Boolean = wallet.isUnlocked(item)
    fun adsWatched(item: ShopItem): Int = wallet.adsWatched(item)
    fun canAfford(item: ShopItem): Boolean = wallet.canAfford(item)

    fun isSelected(item: ShopItem): Boolean =
        item.id == boardTheme.id || item.id == pawnStyle.id

    fun select(item: ShopItem) {
        if (!wallet.select(item)) {
            sound(Sfx.INVALID)
            return
        }
        when (item.kind) {
            ItemKind.BOARD -> boardTheme = BoardTheme.forId(item.id)
            ItemKind.PAWN -> pawnStyle = PawnStyle.forId(item.id)
        }
        sound(Sfx.CLICK)
    }

    fun buy(item: ShopItem) {
        if (wallet.buy(item)) {
            refreshShop()
            sound(Sfx.COIN)
            select(item)
        } else {
            sound(Sfx.INVALID)
        }
    }

    /** Ödüllü reklam sonuna kadar izlendi. */
    fun onRewardedAdWatched(item: ShopItem) {
        val unlocked = wallet.recordAd(item)
        refreshShop()
        sound(Sfx.COIN)
        if (unlocked) select(item)
    }

    private fun refreshShop() {
        points = wallet.points
        shopVersion++
    }

    // ---- Ekranlar ----

    fun openMenu() {
        aiJob?.cancel()
        aiThinking = false
        screen = Screen.MENU
    }

    fun openRules() {
        screen = Screen.RULES
    }

    fun openAppearance() {
        screen = Screen.APPEARANCE
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
        lastReward = 0
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
                if (cell in state.legalSteps()) {
                    play(Move.Step(cell))
                } else if (cell != state.position(state.currentPlayer)) {
                    sound(Sfx.INVALID)
                }
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
                    sound(if (state.canPlaceWall(wall)) Sfx.CLICK else Sfx.INVALID)
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

    val canUndo: Boolean get() = history.isNotEmpty() && !aiThinking && !state.isOver

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
        sound(Sfx.CLICK)
        maybeRunAi()
    }

    private fun play(move: Move) {
        val before = state
        history += before
        state = before.apply(move)
        when (move) {
            is Move.Step -> {
                val from = before.position(before.currentPlayer)
                val far = abs(from.x - move.to.x) + abs(from.y - move.to.y) > 1
                sound(if (far) Sfx.JUMP else Sfx.STEP)
            }
            is Move.PlaceWall -> sound(Sfx.WALL)
        }
        if (mode == InputMode.WALL && state.wallsLeft(state.currentPlayer) == 0) mode = InputMode.MOVE
        state.winner?.let(::onGameOver)
        advanceIfStuck()
        maybeRunAi()
    }

    private fun onGameOver(winner: Int) {
        lastReward = Rewards.forGame(config.computers, config.playerCount, winner)
        wallet.addPoints(lastReward)
        points = wallet.points
        val humanWon = winner !in config.computers
        viewModelScope.launch {
            delay(350) // son hamle sesi bitsin
            sound(if (humanWon) Sfx.WIN else Sfx.LOSE)
        }
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

    // ---- Testler için ----

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

    fun unlockAllForTest() {
        wallet.addPoints(Catalog.items.sumOf { it.price })
        Catalog.items.forEach { wallet.buy(it) }
        refreshShop()
    }

    fun selectForTest(theme: BoardTheme, pawn: PawnStyle) {
        Catalog.byId(theme.id)?.let(::select)
        Catalog.byId(pawn.id)?.let(::select)
    }

    private companion object {
        const val KEY_SOUND = "sound"
    }
}
