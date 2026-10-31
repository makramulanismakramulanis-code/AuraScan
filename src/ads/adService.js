import { AD_CONFIG, getAdUnitId } from './adConfig.js';

class AdService {
  constructor() {
    this.isNative = typeof window !== 'undefined' && window.Capacitor?.isNativePlatform();
    this.bannerVisible = false;
    this.rewardedLoaded = false;
    this.rewardedCountToday = 0;
    this.lastRewardedTimestamp = 0;
  }

  isNativeApp() {
    return typeof window !== 'undefined' && !!window.Capacitor?.isNativePlatform();
  }

  async initAds() {
    if (!AD_CONFIG.ADS_ENABLED) return;
    if (this.isNativeApp()) {
      try {
        const { AdMob } = await import('@capacitor-community/admob');
        await AdMob.initialize({
          testingDevices: [AD_CONFIG.TEST_APP_ID],
          initializeForTesting: AD_CONFIG.USE_TEST_ADS
        });
      } catch (e) {
        console.warn('AdMob initialization error:', e);
      }
    }
  }

  async showBanner(position = 'bottom') {
    if (!AD_CONFIG.ADS_ENABLED) return;
    if (this.isNativeApp()) {
      try {
        const { AdMob, BannerAdPosition, BannerAdSize } = await import('@capacitor-community/admob');
        await AdMob.showBanner({
          adId: getAdUnitId('banner'),
          adSize: BannerAdSize.ADAPTIVE_BANNER,
          position: position === 'bottom' ? BannerAdPosition.BOTTOM_CENTER : BannerAdPosition.TOP_CENTER,
          margin: 64
        });
        this.bannerVisible = true;
      } catch (e) {
        console.warn('Failed to display native banner:', e);
      }
    } else {
      this.bannerVisible = true;
    }
  }

  async hideBanner() {
    if (this.isNativeApp()) {
      try {
        const { AdMob } = await import('@capacitor-community/admob');
        await AdMob.hideBanner();
        this.bannerVisible = false;
      } catch (e) {
        console.warn('Failed to hide banner:', e);
      }
    } else {
      this.bannerVisible = false;
    }
  }

  async loadRewarded() {
    if (!AD_CONFIG.ADS_ENABLED) return;
    if (this.isNativeApp()) {
      try {
        const { AdMob } = await import('@capacitor-community/admob');
        await AdMob.prepareRewardVideoAd({
          adId: getAdUnitId('rewarded')
        });
        this.rewardedLoaded = true;
      } catch (e) {
        console.warn('Failed to prepare rewarded ad:', e);
      }
    }
  }

  async showRewarded() {
    if (!AD_CONFIG.ADS_ENABLED) return { rewarded: false, reason: 'ads_disabled' };

    // Cooldown check
    const now = Date.now();
    if (now - this.lastRewardedTimestamp < 30000) {
      return { rewarded: false, reason: 'cooldown' };
    }

    if (this.isNativeApp()) {
      try {
        const { AdMob } = await import('@capacitor-community/admob');
        const rewardItem = await AdMob.showRewardVideoAd();
        this.lastRewardedTimestamp = Date.now();
        this.rewardedCountToday++;
        return { rewarded: !!rewardItem };
      } catch (e) {
        return { rewarded: false, error: e };
      }
    } else {
      // Browser simulation
      return new Promise((resolve) => {
        let count = 5;
        const interval = setInterval(() => {
          count--;
          if (count <= 0) {
            clearInterval(interval);
            this.lastRewardedTimestamp = Date.now();
            this.rewardedCountToday++;
            resolve({ rewarded: true });
          }
        }, 1000);
      });
    }
  }
}

export const adService = new AdService();
