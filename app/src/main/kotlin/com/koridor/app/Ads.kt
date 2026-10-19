package com.koridor.app

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google AdMob reklam birimleri. Şu anki değerler Google'ın resmi TEST kimlikleridir;
 * yayınlamadan önce AdMob panelinden aldığınız kendi kimliklerinizle değiştirin.
 * Uygulama kimliği app/build.gradle.kts içindeki "admobAppId" alanındadır.
 */
object AdIds {
    const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
}

/**
 * Açılış reklamı, oyun sonu geçiş reklamı ve ödüllü reklamı yönetir.
 * Reklam yüklenemezse oyun hiç beklemeden devam eder.
 */
class AdsManager(private val activity: Activity) {

    var rewardedReady by mutableStateOf(false)
        private set

    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private val started = AtomicBoolean(false)
    private var showing = false

    /** Onay formunu (gerekirse) gösterir, ardından reklamları başlatır. */
    fun start(showAppOpen: Boolean) {
        if (!enabled) return
        val launchedAt = SystemClock.elapsedRealtime()
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        val afterConsent = {
            if (consent.canRequestAds()) initialize(if (showAppOpen) launchedAt else null)
        }
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { afterConsent() } },
            { afterConsent() },
        )
        // Daha önce verilmiş onay varsa güncellemeyi beklemeden başla.
        if (consent.canRequestAds()) afterConsent()
    }

    private fun initialize(appOpenSince: Long?) {
        if (!started.compareAndSet(false, true)) return
        MobileAds.initialize(activity) {}
        appOpenSince?.let { loadAppOpen(it) }
        loadInterstitial()
        loadRewarded()
    }

    private fun loadAppOpen(since: Long) {
        AppOpenAd.load(activity, AdIds.APP_OPEN, AdRequest.Builder().build(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                // Çok geç yüklenen açılış reklamı oyunun ortasında çıkmasın.
                if (SystemClock.elapsedRealtime() - since > APP_OPEN_TIMEOUT_MS || showing) return
                showing = true
                ad.fullScreenContentCallback = doneCallback { showing = false }
                ad.show(activity)
            }

            override fun onAdFailedToLoad(error: LoadAdError) = log("açılış", error)
        })
    }

    private fun loadInterstitial() {
        InterstitialAd.load(activity, AdIds.INTERSTITIAL, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) = log("geçiş", error)
        })
    }

    private fun loadRewarded() {
        RewardedAd.load(activity, AdIds.REWARDED, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewarded = ad
                rewardedReady = true
            }

            override fun onAdFailedToLoad(error: LoadAdError) = log("ödüllü", error)
        })
    }

    /** Oyun sonu reklamını gösterir; kapanınca (ya da reklam yoksa hemen) [then] çalışır. */
    fun showInterstitial(then: () -> Unit) {
        val ad = interstitial
        if (!enabled || ad == null || showing) {
            then()
            return
        }
        interstitial = null
        showing = true
        ad.fullScreenContentCallback = doneCallback {
            showing = false
            loadInterstitial()
            then()
        }
        ad.show(activity)
    }

    /** Ödüllü reklamı gösterir. Ödül hak edilirse [onReward] çağrılır. Reklam hazır değilse false döner. */
    fun showRewarded(onReward: () -> Unit): Boolean {
        val ad = rewarded ?: return false
        if (showing) return false
        rewarded = null
        rewardedReady = false
        showing = true
        ad.fullScreenContentCallback = doneCallback {
            showing = false
            loadRewarded()
        }
        ad.show(activity) { onReward() }
        return true
    }

    private fun doneCallback(onDone: () -> Unit) = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() = onDone()
        override fun onAdFailedToShowFullScreenContent(error: AdError) = onDone()
    }

    private fun log(kind: String, error: LoadAdError) {
        Log.w("Koridor", "$kind reklamı yüklenemedi: ${error.message}")
    }

    companion object {
        private const val APP_OPEN_TIMEOUT_MS = 6000L

        /** Otomatik testlerde reklamlar kapalıdır, ekran görüntülerini örtmesinler. */
        val enabled: Boolean by lazy {
            runCatching { Class.forName("androidx.test.platform.app.InstrumentationRegistry") }.isFailure
        }
    }
}
