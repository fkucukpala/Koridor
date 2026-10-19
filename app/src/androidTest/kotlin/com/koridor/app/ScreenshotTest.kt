package com.koridor.app

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.koridor.game.Cell
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

    @Test
    fun captureScreens() {
        shot("01_menu")

        onUi {
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
    }
}
