package com.koridor.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.koridor.app.BoardScene
import com.koridor.app.BoardTheme
import com.koridor.app.KoridorViewModel
import com.koridor.app.PawnStyle
import com.koridor.app.Players
import com.koridor.game.Cell
import com.koridor.game.GameState
import com.koridor.game.Move
import com.koridor.game.Orientation
import com.koridor.game.Wall

/** Örnek önizleme durumu: birkaç hamle oynanmış bir oyun. */
private fun sampleState(): GameState = listOf(
    Move.Step(Cell(4, 7)),
    Move.PlaceWall(Wall(3, 5, Orientation.HORIZONTAL)),
    Move.PlaceWall(Wall(4, 1, Orientation.VERTICAL)),
    Move.Step(Cell(4, 1)),
).fold(GameState.newGame(2)) { s, m -> s.apply(m) }

@Composable
fun AppearanceScreen(vm: KoridorViewModel) {
    val sample = remember { sampleState() }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = vm::openMenu) { Text("← Menü") }
                Spacer(Modifier.weight(1f))
                Text("Görünüm", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(72.dp))
            }

            SectionTitle("Tahta")
            for (row in BoardTheme.all.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    for (theme in row) {
                        OptionCard(
                            title = theme.name,
                            selected = theme == vm.boardTheme,
                            onClick = { vm.selectBoard(theme) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Board2D(
                                BoardScene(sample, emptyList(), null, false, theme, vm.pawnStyle),
                                onTap = { _, _ -> vm.selectBoard(theme) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            SectionTitle("Piyon")
            for (row in PawnStyle.entries.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    for (style in row) {
                        OptionCard(
                            title = style.title,
                            selected = style == vm.pawnStyle,
                            onClick = { vm.selectPawn(style) },
                            modifier = Modifier.weight(1f),
                        ) {
                            PawnPreview(style, vm.boardTheme)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Text(
                "Seçimin hem 2D hem 3D tahtada kullanılır ve kaydedilir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun OptionCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    preview: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Card(
        modifier = modifier
            .border(3.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            preview()
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                if (selected) "Seçili" else "Seçmek için dokun",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Dört oyuncu renginde piyon önizlemesi, seçili tahtanın karesi üzerinde. */
@Composable
private fun PawnPreview(style: PawnStyle, theme: BoardTheme) {
    Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = size.width / 2
            for (i in 0 until 4) {
                val x = i % 2
                val y = i / 2
                val tile = theme.tileColor(x, y)
                drawRoundRect(
                    tile,
                    topLeft = Offset(x * cell + cell * 0.05f, y * cell + cell * 0.05f),
                    size = Size(cell * 0.9f, cell * 0.9f),
                    cornerRadius = CornerRadius(cell * 0.12f),
                )
                drawPawn(style, Players.colors[i], Offset(x * cell + cell / 2, y * cell + cell / 2), cell * 0.32f)
            }
        }
    }
}
