package com.koridor.app

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.koridor.game.Cell
import com.koridor.game.Difficulty
import com.koridor.game.Move
import com.koridor.game.Orientation
import com.koridor.game.Wall
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Uygulamayı emülatörde açıp birkaç durumda ekran görüntüsü alır.
 * Görüntüler CI tarafından "ci-screenshots" dalına yüklenir.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val vm get() = rule.activity.viewModel

    private fun shot(name: String) {
        rule.waitForIdle()
        Thread.sleep(1800)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val dir = File(rule.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun onUi(block: () -> Unit) {
        rule.runOnUiThread(block)
        rule.waitForIdle()
    }

    private val sampleMoves = listOf(
        Move.Step(Cell(4, 7)),
        Move.PlaceWall(Wall(3, 5, Orientation.HORIZONTAL)),
        Move.PlaceWall(Wall(4, 1, Orientation.VERTICAL)),
        Move.Step(Cell(4, 1)),
        Move.Step(Cell(5, 7)),
        Move.PlaceWall(Wall(5, 6, Orientation.HORIZONTAL)),
        Move.PlaceWall(Wall(1, 2, Orientation.VERTICAL)),
        Move.Step(Cell(4, 2)),
    )

    @Test
    fun captureScreens() {
        shot("01_menu")

        onUi {
            vm.selectForTest(BoardTheme.WALNUT, PawnStyle.CLASSIC)
            vm.startGame(GameConfig(2))
            val h = Orientation.HORIZONTAL
            val v = Orientation.VERTICAL
            listOf(
                Move.Step(Cell(4, 7)),
                Move.PlaceWall(Wall(3, 5, h)),
                Move.PlaceWall(Wall(4, 1, v)),
                Move.Step(Cell(4, 1)),
                Move.Step(Cell(5, 7)),
                Move.PlaceWall(Wall(5, 6, h)),
                Move.Step(Cell(6, 7)),
            ).forEach(vm::playForTest)
        }
        assertEquals(Cell(6, 7), vm.state.position(0))
        shot("02_oyun_3d")

        onUi { vm.setModeForTest(InputMode.WALL, Wall(2, 2, Orientation.VERTICAL)) }
        shot("03_duvar_onizleme_3d")

        onUi {
            vm.camera.yaw = 135f
            vm.camera.pitch = 32f
        }
        shot("04_dondurulmus_3d")

        onUi {
            vm.camera.reset()
            vm.setUse3dForTest(false)
        }
        shot("05_oyun_2d")

        onUi {
            vm.setUse3dForTest(true)
            vm.startGame(GameConfig(4))
            listOf(
                Move.Step(Cell(4, 7)),
                Move.Step(Cell(1, 4)),
                Move.PlaceWall(Wall(3, 1, Orientation.HORIZONTAL)),
                Move.PlaceWall(Wall(6, 3, Orientation.VERTICAL)),
            ).forEach(vm::playForTest)
        }
        shot("06_dort_kisi_3d")

        onUi { vm.openRules() }
        shot("07_kurallar")

        // Mağaza: önce kilitli hâli, sonra her şey açılmış hâli.
        onUi { vm.openAppearance() }
        shot("08_gorunum_kilitli")
        onUi { vm.unlockAllForTest() }

        // Her tahta tasarımı farklı bir piyonla, aynı oyun durumunda.
        val combos = listOf(
            BoardTheme.GRASS to PawnStyle.CHECKER,
            BoardTheme.MARBLE to PawnStyle.ROOK,
            BoardTheme.ICE to PawnStyle.CRYSTAL,
            BoardTheme.NEON to PawnStyle.CLASSIC,
        )
        for ((i, combo) in combos.withIndex()) {
            val (theme, pawn) = combo
            onUi {
                vm.selectForTest(theme, pawn)
                vm.setUse3dForTest(true)
                vm.startGame(GameConfig(2))
                sampleMoves.forEach(vm::playForTest)
                vm.camera.yaw = 25f
                vm.camera.pitch = 42f
                vm.camera.distance = 13f
            }
            shot("%02d_%s_3d".format(9 + i * 2, theme.id.removePrefix("board_")))
            onUi { vm.setUse3dForTest(false) }
            shot("%02d_%s_2d".format(10 + i * 2, theme.id.removePrefix("board_")))
        }

        // Varsayılana dön ki uygulama bir sonraki açılışta normal görünsün.
        onUi {
            vm.selectForTest(BoardTheme.WALNUT, PawnStyle.CLASSIC)
            vm.setUse3dForTest(true)
        }

        // Bilgisayara karşı kazanılan oyunun sonu: puan ödülü penceresi.
        onUi {
            vm.startGame(GameConfig(2, mapOf(1 to Difficulty.EASY)))
            val blue = listOf(Cell(3, 0), Cell(2, 0))
            for (i in 0 until 8) {
                vm.playForTest(Move.Step(Cell(4, 7 - i)))
                if (i < 7) vm.playForTest(Move.Step(blue[i % 2]))
            }
        }
        assertEquals(0, vm.state.winner)
        shot("17_oyun_sonu_odul")
    }
}
