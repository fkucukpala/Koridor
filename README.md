# Koridor

Klasik **Koridor** (Quoridor) akıl oyununun Android sürümü.

- 9×9 tahta, piyon atlama ve çapraz geçiş kuralları
- **Bilgisayara karşı** (Kolay / Orta / Zor), **iki kişi** veya **dört kişi** oyun
- **3D tahta:** tek parmakla 360° döndürme, iki parmakla yakınlaştırma; istenirse **2D** görünüm
- Hiçbir oyuncunun yolu tamamen kapatılamaz; geçersiz duvar önizlemesi kırmızı görünür
- Geri alma, yeniden başlatma
- 5 tahta ve 4 piyon tasarımı; puanla ya da ödüllü reklam izleyerek açılır
- Ses efektleri (kod ile üretilir), piyon ve duvar animasyonları
- Reklamlar (Google AdMob): açılışta App Open, oyun sonunda geçiş reklamı, tasarım açmak için ödüllü reklam

## Reklam kimlikleri

Projede şu an Google'ın **test** reklam kimlikleri var. Yayınlamadan önce AdMob panelinden aldığınız kimliklerle değiştirin:

- Uygulama kimliği: `app/build.gradle.kts` → `manifestPlaceholders["admobAppId"]`
- Reklam birimleri: `app/src/main/kotlin/com/koridor/app/Ads.kt` → `AdIds`

## Android Studio ile çalıştırma

1. Android Studio'da **File → New → Project from Version Control** seçin.
2. URL olarak `https://github.com/fkucukpala/Koridor` girin ve **Clone**'a basın.
3. Gradle senkronizasyonu bitince üstteki cihaz listesinden bir emülatör ya da USB ile bağlı telefonunuzu seçip **▶ Run**'a basın.

Güncellemeleri almak için: **Git → Pull**.

Hazır APK: GitHub'da **Actions** sekmesi → son başarılı çalışma → **koridor-debug-apk**.

## Proje yapısı

| Modül | İçerik |
|---|---|
| `game/` | Saf Kotlin oyun motoru (kurallar, yol bulma, yapay zekâ) ve birim testleri |
| `app/` | Android uygulaması: Jetpack Compose arayüzü, OpenGL ES 2.0 ile 3D tahta |

Testler:

```bash
./gradlew :game:test                    # oyun motoru
./gradlew :app:connectedDebugAndroidTest # emülatörde ekran görüntüsü testi
```

Her gönderimde GitHub Actions projeyi derler, testleri çalıştırır, emülatörde ekran görüntüleri alır ve bunları `ci-screenshots` dalına koyar.
