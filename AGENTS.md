# Ajan kuralları — Koridor

Bu dosya, bu depoda çalışan her kodlama ajanı için geçerlidir (Claude Code, Gemini,
Codex, Copilot, Cursor, Aider, Cline, OpenCode…). `CLAUDE.md` ve `GEMINI.md`
yalnızca bu dosyayı içe aktarır; kuralları burada güncelleyin.

## Proje

Quoridor türü akıl oyununun Android sürümü. Kotlin, Jetpack Compose, OpenGL ES 2.0.

| Modül | İçerik |
|---|---|
| `game/` | Saf Kotlin oyun motoru: kurallar, yol bulma, yapay zekâ, mağaza. Android bağımlılığı yok. |
| `app/` | Android uygulaması: Compose arayüzü (`ui/`), 3D tahta (`gl/`), reklamlar (`Ads.kt`), sesler (`SoundFx.kt`). |

Sürümler `gradle/libs.versions.toml` içinde. Bağımlılık eklerken oraya yazın, `build.gradle.kts`
dosyalarına doğrudan sürüm numarası yazmayın.

## Komutlar

```bash
./gradlew :game:test            # oyun motoru birim testleri (hızlı, emülatör gerekmez)
./gradlew :app:assembleDebug    # APK derleme
./gradlew :app:connectedDebugAndroidTest  # emülatör/cihaz gerekir; yerelde yoksa CI'a bırakın
```

- Gradle Java 25 ile çalışır (CI: temurin 25).
- Android SDK yoksa `:app` derlenemez; o zaman yalnızca `:game:test` çalıştırın ve bunu raporda söyleyin.
- Her push'ta GitHub Actions (`.github/workflows/android.yml`) derler, testleri ve emülatörde
  ekran görüntüsü testini çalıştırır; görüntüler `ci-screenshots` dalına gider. Arayüz
  değişikliğinden sonra o görüntülere bakın.

## Kurallar

- **Oyun kuralı değişikliği `game/` içinde yapılır ve test ister.** `GameStateTest` /
  `ShopTest`'e, düzeltmeden önce kalan, sonra geçen bir test ekleyin.
- `GameState` değişmezdir: her hamle yeni durum üretir. Bu yapıyı bozmayın (geri alma ve
  yapay zekâ buna dayanır).
- Hiçbir oyuncunun hedefe yolu tamamen kapatılamaz; duvar geçerliliği kontrolünü atlamayın.
- `game/` modülüne Android sınıfı ya da bağımlılığı eklemeyin.
- Arayüz metinleri, kod yorumları ve commit mesajları **Türkçe**.
- **Reklam kimlikleri:** depoda Google'ın test kimlikleri var. Gerçek AdMob kimliklerini,
  imza anahtarlarını (`*.jks`, `*.keystore`), `local.properties` veya herhangi bir sırrı
  commit etmeyin.
- Değişikliği küçük tutun; istenmeyen yeniden düzenleme yapmayın.
- Bitti demeden önce en az `./gradlew :game:test` geçmeli. Çalıştıramadığınız adımı
  "geçti" diye raporlamayın.
- Kullanıcı istemeden `main`'e doğrudan push etmeyin, sürüm numarasını (`versionCode`,
  `versionName`) değiştirmeyin.
