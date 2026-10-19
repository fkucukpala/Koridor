package com.koridor.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val rules = listOf(
    "Amaç" to "Piyonunu karşı kenara ilk ulaştıran kazanır. Hedef kenar tahtada senin renginle işaretlidir.",
    "Sıra" to "Her turda ya piyonunu bir kare (yukarı, aşağı, sağa, sola) oynatırsın ya da bir duvar koyarsın.",
    "Duvarlar" to "Duvar iki kare uzunluğundadır ve karelerin arasındaki oluklara konur. Duvarlar üst üste binemez, birbirini çaprazlayamaz. İki kişilik oyunda herkesin 10, dört kişilikte 5 duvarı vardır.",
    "Yol kuralı" to "Hiçbir oyuncunun hedefe giden yolu tamamen kapatılamaz. Böyle bir duvar kırmızı görünür ve konamaz.",
    "Atlama" to "Bir piyon seninkinin hemen önündeyse üstünden atlayabilirsin. Arkasında duvar ya da başka piyon varsa çapraz geçebilirsin.",
    "Oynanış" to "\"Piyon\" modunda parlayan karelerden birine dokun. \"Duvar\" modunda oluklara dokun, önizleme yeşilse aynı yere tekrar dokun ya da \"Yerleştir\"e bas. \"Yatay/Dikey\" ile duvarı çevir.",
    "3D görünüm" to "Tahtayı tek parmakla sürükleyerek 360° döndürebilir, iki parmakla yakınlaştırabilirsin. \"2D\" düğmesiyle düz görünüme geçebilirsin.",
)

@Composable
fun RulesScreen(onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text("Nasıl Oynanır?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            for ((title, body) in rules) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(body, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(14.dp))
            }
            Button(onClick = onBack) { Text("Menüye Dön") }
        }
    }
}
