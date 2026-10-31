package com.example.ads

import android.content.Context
import android.content.SharedPreferences

data class PremiumPlan(
    val id: String,
    val name: String,
    val priceText: String,
    val period: String,
    val popular: Boolean = false,
    val savings: String = ""
)

object PremiumService {
    private const val PREFS_NAME = "aurascan_premium"
    private const val KEY_IS_PREMIUM = "key_is_premium"
    private const val KEY_EXPORT_COUNT = "key_export_count"

    val PLANS = listOf(
        PremiumPlan("monthly", "Monthly Pass", "₹49", "per month"),
        PremiumPlan("yearly", "Annual Pro", "₹399", "per year (₹33/mo)", popular = true, savings = "Save 32%"),
        PremiumPlan("lifetime", "Lifetime Unlimited", "₹799", "one-time payment", savings = "Best Value")
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isPremium(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_PREMIUM, false)
    }

    fun setPremium(context: Context, premium: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_PREMIUM, premium).apply()
    }

    fun recordExportAndCheckUpsell(context: Context): Boolean {
        if (isPremium(context)) return false
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_EXPORT_COUNT, 0) + 1
        prefs.edit().putInt(KEY_EXPORT_COUNT, count).apply()
        // Gentle upsell every 5th export
        return count % 5 == 0
    }
}
