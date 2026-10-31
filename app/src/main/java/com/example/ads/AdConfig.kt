package com.example.ads

/**
 * AdMob Configuration for AuraScan
 *
 * NOTE: Set USE_TEST_ADS to false only for the final production build.
 * Never click your own live ads.
 */
import com.example.BuildConfig

object AdConfig {
    var ADS_ENABLED: Boolean = true
    const val APP_ID: String = "ca-app-pub-6086454785172418~1753194588"
    const val BANNER_UNIT_ID: String = "ca-app-pub-6086454785172418/2509415047"
    const val REWARDED_UNIT_ID: String = "ca-app-pub-6086454785172418/3598281802"
    const val INTERSTITIAL_UNIT_ID: String = ""

    const val TEST_APP_ID: String = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_UNIT_ID: String = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_REWARDED_UNIT_ID: String = "ca-app-pub-3940256099942544/5224354917"

    // In DEBUG mode or testing, automatically use official AdMob test ads for 100% fill rate.
    // In RELEASE mode, automatically uses live production ads.
    var USE_TEST_ADS: Boolean = BuildConfig.DEBUG

    fun getAdUnitId(type: String): String {
        return when (type) {
            "banner" -> if (USE_TEST_ADS) TEST_BANNER_UNIT_ID else BANNER_UNIT_ID
            "rewarded" -> if (USE_TEST_ADS) TEST_REWARDED_UNIT_ID else REWARDED_UNIT_ID
            else -> ""
        }
    }
}
