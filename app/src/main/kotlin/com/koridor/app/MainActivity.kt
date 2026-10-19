package com.koridor.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.koridor.app.ui.AppearanceScreen
import com.koridor.app.ui.GameScreen
import com.koridor.app.ui.KoridorTheme
import com.koridor.app.ui.MenuScreen
import com.koridor.app.ui.RulesScreen

class MainActivity : ComponentActivity() {

    val viewModel: KoridorViewModel by viewModels()

    private lateinit var sounds: SoundFx
    private lateinit var ads: AdsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sounds = SoundFx(this)
        ads = AdsManager(this)
        // Açılış reklamı yalnızca uygulama ilk açıldığında (ekran döndürmede değil) gösterilir.
        ads.start(showAppOpen = savedInstanceState == null)

        setContent {
            LaunchedEffect(Unit) {
                viewModel.sounds.collect { sounds.play(it) }
            }
            KoridorTheme {
                when (viewModel.screen) {
                    Screen.MENU -> MenuScreen(
                        points = viewModel.points,
                        soundOn = viewModel.soundOn,
                        onToggleSound = viewModel::toggleSound,
                        onStart = viewModel::startGame,
                        onRules = viewModel::openRules,
                        onAppearance = viewModel::openAppearance,
                    )
                    Screen.RULES -> {
                        BackHandler { viewModel.openMenu() }
                        RulesScreen(onBack = viewModel::openMenu)
                    }
                    Screen.APPEARANCE -> {
                        BackHandler { viewModel.openMenu() }
                        AppearanceScreen(viewModel, adReady = ads.rewardedReady) { item ->
                            val shown = ads.showRewarded { viewModel.onRewardedAdWatched(item) }
                            if (!shown) {
                                Toast.makeText(this, "Reklam şu an hazır değil, biraz sonra tekrar dene.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    Screen.GAME -> {
                        BackHandler { viewModel.openMenu() }
                        GameScreen(viewModel, afterGame = ads::showInterstitial)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        sounds.release()
        super.onDestroy()
    }
}
