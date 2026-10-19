package com.koridor.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koridor.app.GameConfig
import com.koridor.game.Difficulty

private val difficultyNames = mapOf(
    Difficulty.EASY to "Kolay",
    Difficulty.MEDIUM to "Orta",
    Difficulty.HARD to "Zor",
)

@Composable
fun MenuScreen(onStart: (GameConfig) -> Unit, onRules: () -> Unit, onAppearance: () -> Unit) {
    var difficulty by rememberSaveable { mutableStateOf(Difficulty.MEDIUM) }
    var fourWithComputers by rememberSaveable { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text("KORİDOR", fontSize = 44.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text(
                "Yolunu bul, rakibinin yolunu kapat",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            val cardModifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
            MenuCard(cardModifier, "Bilgisayara Karşı", "Sen kırmızısın, bilgisayar mavi.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (d in Difficulty.entries) {
                        FilterChip(
                            selected = d == difficulty,
                            onClick = { difficulty = d },
                            label = { Text(difficultyNames.getValue(d)) },
                        )
                    }
                }
                Button(
                    onClick = { onStart(GameConfig(2, mapOf(1 to difficulty))) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Başla") }
            }
            Spacer(Modifier.height(16.dp))
            MenuCard(cardModifier, "İki Kişi", "Aynı telefonda sırayla oynayın. Her oyuncunun 10 duvarı var.") {
                Button(onClick = { onStart(GameConfig(2)) }, modifier = Modifier.fillMaxWidth()) { Text("Başla") }
            }
            Spacer(Modifier.height(16.dp))
            MenuCard(cardModifier, "Dört Kişi", "Dört kenardan dört piyon, her oyuncuya 5 duvar.") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Diğer üç oyuncu bilgisayar", modifier = Modifier.weight(1f))
                    Switch(checked = fourWithComputers, onCheckedChange = { fourWithComputers = it })
                }
                Button(
                    onClick = {
                        val computers = if (fourWithComputers) (1..3).associateWith { Difficulty.MEDIUM } else emptyMap()
                        onStart(GameConfig(4, computers))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Başla") }
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onAppearance) { Text("Görünüm") }
                OutlinedButton(onClick = onRules) { Text("Nasıl Oynanır?") }
            }
        }
    }
}

@Composable
private fun MenuCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}
