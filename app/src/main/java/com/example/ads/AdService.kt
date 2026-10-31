package com.example.ads

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AdMetrics(
    val bannerShownCount: Int = 0,
    val bannerFailCount: Int = 0,
    val rewardedStarted: Int = 0,
    val rewardedCompleted: Int = 0,
    val coinsGranted: Int = 0
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

    const val MAX_DAILY_REWARDED = 5
    const val COOLDOWN_SECONDS = 30
    const val REWARD_AMOUNT = 100

    private val _metrics = MutableStateFlow(AdMetrics())
    val metrics: StateFlow<AdMetrics> = _metrics.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun init(context: Context) {
        val prefs = getPrefs(context)
        _metrics.value = AdMetrics(
            bannerShownCount = prefs.getInt(KEY_METRICS_BANNER_SHOWN, 0),
            bannerFailCount = prefs.getInt(KEY_METRICS_BANNER_FAIL, 0),
            rewardedStarted = prefs.getInt(KEY_METRICS_REWARDED_START, 0),
            rewardedCompleted = prefs.getInt(KEY_METRICS_REWARDED_COMPLETE, 0),
            coinsGranted = prefs.getInt(KEY_METRICS_COINS_GRANTED, 0)
        )
    }

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
