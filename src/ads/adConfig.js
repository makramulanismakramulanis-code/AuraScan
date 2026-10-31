/**
 * AuraScan AdMob Configuration
 *
 * NOTE: Set USE_TEST_ADS to false only for the final production build.
 * Never click your own live ads.
 */
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

export function getAdUnitId(type) {
  if (type === "banner") {
    return AD_CONFIG.USE_TEST_ADS ? AD_CONFIG.TEST_BANNER_UNIT_ID : AD_CONFIG.BANNER_UNIT_ID;
  }
  if (type === "rewarded") {
    return AD_CONFIG.USE_TEST_ADS ? AD_CONFIG.TEST_REWARDED_UNIT_ID : AD_CONFIG.REWARDED_UNIT_ID;
  }
  return "";
}
