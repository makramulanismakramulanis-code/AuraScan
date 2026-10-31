# AuraScan - Android & Capacitor Build Instructions

## 1. Overview
AuraScan is built natively for Android using Jetpack Compose, Material Design 3, and Google Gemini AI, with full support for AdMob monetization and on-device offline storage.

## 2. Ad Configuration
The ad unit IDs are configured in both:
- **Android Kotlin**: `app/src/main/java/com/example/ads/AdConfig.kt`
- **Web / Capacitor**: `src/ads/adConfig.js`

```javascript
export const AD_CONFIG = {
  ADS_ENABLED: true,
  APP_ID: "ca-app-pub-6086454785172418~1753194588",
  BANNER_UNIT_ID: "ca-app-pub-6086454785172418/2509415047",
  REWARDED_UNIT_ID: "ca-app-pub-3940256099942544/5224354917",
  INTERSTITIAL_UNIT_ID: "",
  TEST_APP_ID: "ca-app-pub-3940256099942544~3347511713",
  TEST_BANNER_UNIT_ID: "ca-app-pub-3940256099942544/6300978111",
  TEST_REWARDED_UNIT_ID: "ca-app-pub-3940256099942544/5224354917",
  USE_TEST_ADS: true
};
```
> **IMPORTANT**: Set `USE_TEST_ADS` to `false` only for the final production release build. Never click your own live ads.

## 3. AndroidManifest.xml Integration
The Google Mobile Ads Application ID is added to `app/src/main/AndroidManifest.xml`:
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-6086454785172418~1753194588"/>
```

## 4. Capacitor Android Export & Build Steps
If you export the web app and build via Capacitor CLI, execute the following commands in sequence:

```bash
# 1. Install Capacitor dependencies and AdMob community plugin
npm install @capacitor/core @capacitor/cli @capacitor/android @capacitor-community/admob

# 2. Initialize Capacitor project
npx cap init AuraScan com.aurascan.app

# 3. Build web distribution
npm run build

# 4. Add the Android platform
npx cap add android

# 5. Sync web code and native plugins to the android folder
npx cap sync

# 6. Open the project in Android Studio
npx cap open android

# 7. Run on a real device or emulator using test ads
# 8. Switch USE_TEST_ADS to false in src/ads/adConfig.js / AdConfig.kt for the final release APK/AAB build.
```

## 5. Next Manual Steps
1. **Create the Rewarded Ad Unit in Google AdMob Console**:
   - Go to [AdMob Console](https://apps.admob.com/) -> Apps -> AuraScan.
   - Click **Add Ad Unit** -> **Rewarded Video**.
   - Copy the generated unit ID and replace `"REWARDED_UNIT_ID_HERE"` in `AdConfig.kt` and `adConfig.js`.
2. **Google Play Console Listing**:
   - Create the app listing for `com.aurascan.app` / `com.aistudio.aurascan.kxpztr`.
   - Complete Data Safety (state that images are sent to Gemini only upon user initiation; ads delivered via AdMob).
3. **Google Play Billing (In-App Purchases)**:
   - For AuraScan Pro (Monthly ₹49, Annual ₹399, Lifetime ₹799), create in-app products in Play Console and link them using `PremiumService`.
