package com.koridor.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.unit.sp
import com.koridor.game.Catalog
import com.koridor.game.ItemKind
import com.koridor.game.ShopItem
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
fun AppearanceScreen(vm: KoridorViewModel, adReady: Boolean, onWatchAd: (ShopItem) -> Unit) {
    val sample = remember { sampleState() }
    // Satın alma / reklam sonrası kartlar yeniden okunsun.
    @Suppress("UNUSED_VARIABLE") val version = vm.shopVersion

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
                PointsBadge(vm.points)
            }
            Text(
                "Oyun kazandıkça puan toplarsın. Kilitli görünümleri puanla ya da reklam izleyerek açabilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            SectionTitle("Tahta")
            ItemGrid(Catalog.ofKind(ItemKind.BOARD), vm, adReady, onWatchAd) { item ->
                val theme = BoardTheme.forId(item.id)
                Board2D(
                    BoardScene(sample, emptyList(), null, false, theme, vm.pawnStyle),
                    onTap = { _, _ -> },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionTitle("Piyon")
            ItemGrid(Catalog.ofKind(ItemKind.PAWN), vm, adReady, onWatchAd) { item ->
                PawnPreview(PawnStyle.forId(item.id), vm.boardTheme)
            }
        }
    }
}

@Composable
fun PointsBadge(points: Int) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(Color(0xFFF2C14E), RoundedCornerShape(50)))
        Spacer(Modifier.width(6.dp))
        Text("$points puan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ItemGrid(
    items: List<ShopItem>,
    vm: KoridorViewModel,
    adReady: Boolean,
    onWatchAd: (ShopItem) -> Unit,
    preview: @Composable (ShopItem) -> Unit,
) {
    for (row in items.chunked(2)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
            for (item in row) {
                ItemCard(item, vm, adReady, onWatchAd, Modifier.weight(1f)) { preview(item) }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun ItemCard(
    item: ShopItem,
    vm: KoridorViewModel,
    adReady: Boolean,
    onWatchAd: (ShopItem) -> Unit,
    modifier: Modifier,
    preview: @Composable () -> Unit,
) {
    val unlocked = vm.isUnlocked(item)
    val selected = vm.isSelected(item)
    val shape = RoundedCornerShape(14.dp)
    Card(
        modifier = modifier
            .border(3.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
            .clickable(enabled = unlocked) { vm.select(item) },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                preview()
                if (!unlocked) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🔒", fontSize = 28.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            when {
                selected -> Text("Seçili", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                unlocked -> Text("Seçmek için dokun", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { vm.buy(item) },
                        enabled = vm.canAfford(item),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.fillMaxWidth().height(34.dp),
                    ) { Text("${item.price} puanla aç", style = MaterialTheme.typography.labelMedium) }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { onWatchAd(item) },
                        enabled = adReady,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.fillMaxWidth().height(34.dp),
                    ) {
                        Text(
                            "Reklam izle ${vm.adsWatched(item)}/${item.adsRequired}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
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
