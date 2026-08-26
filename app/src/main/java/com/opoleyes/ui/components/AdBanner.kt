package com.opoleyes.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.opoleyes.BuildConfig
import com.opoleyes.ui.theme.BgDark

/**
 * Persistent banner ad using Google AdMob.
 * Uses the test ad unit ID for development; replace with a real ID for production.
 * In release builds with ADS_ENABLED=false the banner renders as an empty box.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    if (!BuildConfig.ADS_ENABLED) return

    val context = LocalContext.current
    val adView = remember {
        AdView(context).apply {
            // Test banner ad unit ID
            setAdUnitId("ca-app-pub-3940256099942544/6300978111")
            setAdSize(AdSize.BANNER)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(BgDark)
    ) {
        AndroidView(
            factory = {
                adView.apply {
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
