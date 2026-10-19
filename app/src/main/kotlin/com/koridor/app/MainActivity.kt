package com.koridor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.koridor.app.ui.GameScreen
import com.koridor.app.ui.KoridorTheme
import com.koridor.app.ui.MenuScreen
import com.koridor.app.ui.RulesScreen

class MainActivity : ComponentActivity() {

    val viewModel: KoridorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KoridorTheme {
                when (viewModel.screen) {
                    Screen.MENU -> MenuScreen(
                        onStart = viewModel::startGame,
                        onRules = viewModel::openRules,
                    )
                    Screen.RULES -> {
                        BackHandler { viewModel.openMenu() }
                        RulesScreen(onBack = viewModel::openMenu)
                    }
                    Screen.GAME -> {
                        BackHandler { viewModel.openMenu() }
                        GameScreen(viewModel)
                    }
                }
            }
        }
    }
}
