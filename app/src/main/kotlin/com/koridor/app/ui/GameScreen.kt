package com.koridor.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koridor.app.InputMode
import com.koridor.app.KoridorViewModel
import com.koridor.app.Players
import com.koridor.game.Orientation

@Composable
fun GameScreen(vm: KoridorViewModel) {
    val state = vm.state
    val scene = vm.scene
    val count = state.playerCount

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = vm::openMenu) { Text("← Menü") }
                Spacer(Modifier.weight(1f))
                Text("KORİDOR", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = vm::toggle3d, modifier = Modifier.testTag("toggle3d")) {
                    Text(if (vm.use3d) "2D" else "3D")
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (p in 0 until count) {
                    PlayerBadge(
                        name = if (p in vm.config.computers) "Bilgisayar" else Players.name(count, p),
                        color = Players.color(count, p),
                        walls = state.wallsLeft(p),
                        active = p == state.currentPlayer && !state.isOver,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (vm.use3d) {
                    Board3D(scene, vm.camera, vm::onBoardTap, Modifier.fillMaxSize())
                    Row(
                        Modifier.align(Alignment.BottomEnd).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SmallButton("Üstten") { vm.camera.topView() }
                        SmallButton("Sıfırla") { vm.camera.reset() }
                    }
                } else {
                    Board2D(scene, vm::onBoardTap, Modifier.fillMaxWidth())
                }
            }

            Text(
                statusText(vm),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = vm.mode == InputMode.MOVE,
                    onClick = { vm.selectMode(InputMode.MOVE) },
                    label = { Text("Piyon") },
                )
                FilterChip(
                    selected = vm.mode == InputMode.WALL,
                    enabled = state.wallsLeft(state.currentPlayer) > 0,
                    onClick = { vm.selectMode(InputMode.WALL) },
                    label = { Text("Duvar") },
                )
                if (vm.mode == InputMode.WALL) {
                    OutlinedButton(onClick = vm::toggleOrientation) {
                        Text(if (vm.orientation == Orientation.HORIZONTAL) "Yatay ⇄" else "Dikey ⇅")
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = vm::undo, enabled = vm.canUndo) { Text("Geri Al") }
            }
            if (vm.mode == InputMode.WALL && scene.preview != null) {
                Button(
                    onClick = vm::confirmWall,
                    enabled = scene.previewValid,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (scene.previewValid) "Yerleştir" else "Buraya duvar konamaz") }
            }
        }
    }

    state.winner?.let { w ->
        val name = if (w in vm.config.computers) "Bilgisayar" else Players.name(count, w)
        AlertDialog(
            onDismissRequest = {},
            title = { Text(if (w in vm.config.computers) "Kaybettin" else "Tebrikler!") },
            text = { Text("$name kazandı.") },
            confirmButton = { Button(onClick = vm::restart) { Text("Yeniden Oyna") } },
            dismissButton = { TextButton(onClick = vm::openMenu) { Text("Menü") } },
        )
    }
}

private fun statusText(vm: KoridorViewModel): String {
    val s = vm.state
    val count = s.playerCount
    if (s.isOver) return "Oyun bitti"
    if (vm.aiThinking || s.currentPlayer in vm.config.computers) return "Bilgisayar düşünüyor…"
    val name = Players.name(count, s.currentPlayer)
    return when (vm.mode) {
        InputMode.MOVE -> "Sıra $name oyuncuda: parlayan karelerden birine dokun"
        InputMode.WALL -> "Sıra $name oyuncuda: duvar için oluklara dokun"
    }
}

@Composable
private fun PlayerBadge(name: String, color: Color, walls: Int, active: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .background(if (active) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface, shape)
            .border(2.dp, if (active) color else Color.Transparent, shape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Text("Duvar: $walls", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.height(36.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}
