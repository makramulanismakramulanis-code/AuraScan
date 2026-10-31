/**
 * AuraScan Consent Management Flow
 * Designed for Google User Messaging Platform (UMP) and GDPR/Privacy choices
 */
export async function requestConsentInfo() {
  if (typeof window !== 'undefined' && window.Capacitor?.isNativePlatform()) {
    try {
      // UMP consent flow via native plugin if installed
      console.log('Requesting native UMP consent info...');
    } catch (e) {
      console.warn('Native consent error:', e);
    }
  } else {
    // Web / Local Storage check
    const consent = localStorage.getItem('aurascan_ad_consent');
    return consent ? JSON.parse(consent) : null;
  }
}

export function saveConsentChoice(choice) {
  if (typeof window !== 'undefined') {
    localStorage.setItem('aurascan_ad_consent', JSON.stringify({
      accepted: choice.accepted,
      personalized: choice.personalized,
      timestamp: Date.now()
    }));
  }
}
