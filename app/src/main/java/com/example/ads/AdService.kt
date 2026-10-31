package com.example.ads

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AdMetrics(
    val bannerShownCount: Int = 0,
    val bannerFailCount: Int = 0,
    val rewardedStarted: Int = 0,
    val rewardedCompleted: Int = 0,
    val coinsGranted: Int = 0,
    val isRewardedReady: Boolean = false
)

object AdService {
    private const val PREFS_NAME = "aurascan_ads"
    private const val KEY_METRICS_BANNER_SHOWN = "banner_shown"
    private const val KEY_METRICS_BANNER_FAIL = "banner_fail"
    private const val KEY_METRICS_REWARDED_START = "rewarded_start"
    private const val KEY_METRICS_REWARDED_COMPLETE = "rewarded_complete"
    private const val KEY_METRICS_COINS_GRANTED = "coins_granted"

    private const val KEY_LAST_REWARDED_DATE = "last_rewarded_date"
    private const val KEY_DAILY_REWARDED_COUNT = "daily_rewarded_count"
    private const val KEY_LAST_REWARDED_TIMESTAMP = "last_rewarded_timestamp"

    val MAX_DAILY_REWARDED: Int get() = if (AdConfig.USE_TEST_ADS) 50 else 5
    val COOLDOWN_SECONDS: Int get() = if (AdConfig.USE_TEST_ADS) 0 else 30
    const val REWARD_AMOUNT = 50

    private val _metrics = MutableStateFlow(AdMetrics())
    val metrics: StateFlow<AdMetrics> = _metrics.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private var rewardedAd: com.google.android.gms.ads.rewarded.RewardedAd? = null
    private var isAdLoading = false

    fun init(context: Context) {
        val prefs = getPrefs(context)
        _metrics.value = AdMetrics(
            bannerShownCount = prefs.getInt(KEY_METRICS_BANNER_SHOWN, 0),
            bannerFailCount = prefs.getInt(KEY_METRICS_BANNER_FAIL, 0),
            rewardedStarted = prefs.getInt(KEY_METRICS_REWARDED_START, 0),
            rewardedCompleted = prefs.getInt(KEY_METRICS_REWARDED_COMPLETE, 0),
            coinsGranted = prefs.getInt(KEY_METRICS_COINS_GRANTED, 0)
        )
        // Pre-load a rewarded ad
        loadRewardedAd(context)
    }

    fun loadRewardedAd(context: Context, isRetryWithFallback: Boolean = false) {
        if (rewardedAd != null) {
            _metrics.value = _metrics.value.copy(isRewardedReady = true)
            return
        }
        if (isAdLoading) return
        isAdLoading = true

        val unitId = if (isRetryWithFallback) {
            AdConfig.TEST_REWARDED_UNIT_ID
        } else {
            AdConfig.getAdUnitId("rewarded")
        }

        val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
        android.util.Log.d("AdService", "Requesting RewardedAd with ID: $unitId (isFallback=$isRetryWithFallback)")

        com.google.android.gms.ads.rewarded.RewardedAd.load(
            context,
            unitId,
            adRequest,
            object : com.google.android.gms.ads.rewarded.RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: com.google.android.gms.ads.LoadAdError) {
                    android.util.Log.d("AdService", "Rewarded ad load notice ($unitId): code=${adError.code}, message=${adError.message}")
                    rewardedAd = null
                    isAdLoading = false
                    _metrics.value = _metrics.value.copy(isRewardedReady = false)

                    // If live ad had no fill (Code 3) or failed, try test ad unit once
                    if (!isRetryWithFallback && unitId != AdConfig.TEST_REWARDED_UNIT_ID) {
                        android.util.Log.d("AdService", "Live ad had no fill. Checking test ad unit.")
                        loadRewardedAd(context, isRetryWithFallback = true)
                    } else {
                        loadRewardedAdWithDelay(context)
                    }
                }

                override fun onAdLoaded(ad: com.google.android.gms.ads.rewarded.RewardedAd) {
                    android.util.Log.d("AdService", "Rewarded ad loaded successfully and ready! (unitId=$unitId)")
                    rewardedAd = ad
                    isAdLoading = false
                    _metrics.value = _metrics.value.copy(isRewardedReady = true)
                }
            }
        )
    }

    private fun loadRewardedAdWithDelay(context: Context) {
        kotlinx.coroutines.MainScope().launch {
            kotlinx.coroutines.delay(60000)
            loadRewardedAd(context)
        }
    }

    fun showRewardedAd(
        activity: android.app.Activity,
        onAdDismissed: () -> Unit,
        onRewardEarned: (Int) -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            var rewardEarned = false
            var earnedCoins = REWARD_AMOUNT

            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    android.util.Log.d("AdService", "Rewarded ad showed successfully in full screen")
                    recordRewardedStarted(activity.applicationContext)
                }

                override fun onAdDismissedFullScreenContent() {
                    android.util.Log.d("AdService", "Rewarded ad dismissed. rewardEarned=$rewardEarned")
                    rewardedAd = null
                    _metrics.value = _metrics.value.copy(isRewardedReady = false)
                    loadRewardedAd(activity.applicationContext) // Preload next one

                    if (rewardEarned) {
                        val coins = recordRewardedCompleted(activity.applicationContext)
                        onRewardEarned(coins)
                    } else {
                        onAdDismissed()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    android.util.Log.w("AdService", "Rewarded ad notice while showing: ${error.message}")
                    rewardedAd = null
                    _metrics.value = _metrics.value.copy(isRewardedReady = false)
                    loadRewardedAd(activity.applicationContext)
                    // Grant reward so user is never blocked from their PDF download
                    val coins = recordRewardedCompleted(activity.applicationContext)
                    onRewardEarned(coins)
                }
            }

            ad.show(activity) { rewardItem ->
                android.util.Log.d("AdService", "Reward earned callback: amount=${rewardItem.amount}")
                rewardEarned = true
                earnedCoins = rewardItem.amount.coerceAtLeast(REWARD_AMOUNT)
            }
        } else {
            android.util.Log.d("AdService", "Rewarded ad not cached; smoothly granting reward.")
            _metrics.value = _metrics.value.copy(isRewardedReady = false)
            loadRewardedAd(activity.applicationContext)
            val coins = recordRewardedCompleted(activity.applicationContext)
            onRewardEarned(coins)
        }
    }

    fun isRewardedAdLoaded(): Boolean = rewardedAd != null

    fun shouldShowAds(context: Context): Boolean {
        if (!AdConfig.ADS_ENABLED) return false
        if (PremiumService.isPremium(context)) return false
        return true
    }

    fun recordBannerShown(context: Context) {
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_METRICS_BANNER_SHOWN, 0) + 1
        prefs.edit().putInt(KEY_METRICS_BANNER_SHOWN, current).apply()
        _metrics.value = _metrics.value.copy(bannerShownCount = current)
    }

    fun recordBannerFailure(context: Context) {
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_METRICS_BANNER_FAIL, 0) + 1
        prefs.edit().putInt(KEY_METRICS_BANNER_FAIL, current).apply()
        _metrics.value = _metrics.value.copy(bannerFailCount = current)
    }

    fun getRemainingRewardedToday(context: Context): Int {
        val prefs = getPrefs(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = prefs.getString(KEY_LAST_REWARDED_DATE, "")
        val count = if (lastDate == today) prefs.getInt(KEY_DAILY_REWARDED_COUNT, 0) else 0
        return (MAX_DAILY_REWARDED - count).coerceAtLeast(0)
    }

    fun getCooldownRemainingSeconds(context: Context): Int {
        val prefs = getPrefs(context)
        val lastTs = prefs.getLong(KEY_LAST_REWARDED_TIMESTAMP, 0L)
        val elapsed = (System.currentTimeMillis() - lastTs) / 1000
        return if (elapsed < COOLDOWN_SECONDS) (COOLDOWN_SECONDS - elapsed).toInt() else 0
    }

    fun canWatchRewarded(context: Context): Pair<Boolean, String> {
        if (getRemainingRewardedToday(context) <= 0) {
            return Pair(false, "Daily limit of 5 rewarded videos reached. Come back tomorrow!")
        }
        val cooldown = getCooldownRemainingSeconds(context)
        if (cooldown > 0) {
            return Pair(false, "Please wait ${cooldown}s before watching another ad.")
        }
        return Pair(true, "")
    }

    fun recordRewardedStarted(context: Context) {
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_METRICS_REWARDED_START, 0) + 1
        prefs.edit().putInt(KEY_METRICS_REWARDED_START, current).apply()
        _metrics.value = _metrics.value.copy(rewardedStarted = current)
    }

    fun recordRewardedCompleted(context: Context): Int {
        val prefs = getPrefs(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = prefs.getString(KEY_LAST_REWARDED_DATE, "")
        val dailyCount = if (lastDate == today) prefs.getInt(KEY_DAILY_REWARDED_COUNT, 0) + 1 else 1
        val completed = prefs.getInt(KEY_METRICS_REWARDED_COMPLETE, 0) + 1
        val coins = prefs.getInt(KEY_METRICS_COINS_GRANTED, 0) + REWARD_AMOUNT

        prefs.edit()
            .putString(KEY_LAST_REWARDED_DATE, today)
            .putInt(KEY_DAILY_REWARDED_COUNT, dailyCount)
            .putLong(KEY_LAST_REWARDED_TIMESTAMP, System.currentTimeMillis())
            .putInt(KEY_METRICS_REWARDED_COMPLETE, completed)
            .putInt(KEY_METRICS_COINS_GRANTED, coins)
            .apply()

        _metrics.value = _metrics.value.copy(
            rewardedCompleted = completed,
            coinsGranted = coins
        )

        return REWARD_AMOUNT
    }
}
