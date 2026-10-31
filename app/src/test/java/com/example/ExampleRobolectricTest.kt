package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ads.AdConfig
import com.example.ads.AdService
import com.example.data.AppStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AuraScan", appName)
  }

  @Test
  fun `verify ad config and units`() {
    assertTrue(AdConfig.ADS_ENABLED)
    if (AdConfig.USE_TEST_ADS) {
        assertEquals(AdConfig.TEST_BANNER_UNIT_ID, AdConfig.getAdUnitId("banner"))
        assertEquals(AdConfig.TEST_REWARDED_UNIT_ID, AdConfig.getAdUnitId("rewarded"))
    } else {
        assertEquals(AdConfig.BANNER_UNIT_ID, AdConfig.getAdUnitId("banner"))
        assertEquals(AdConfig.REWARDED_UNIT_ID, AdConfig.getAdUnitId("rewarded"))
    }
  }

  @Test
  fun `verify storage initialization and coins balance`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = AppStorage(context)
    assertNotNull(storage.documents.value)
    assertNotNull(storage.expenses.value)
    assertTrue(storage.coins.value >= 350)
  }
}
