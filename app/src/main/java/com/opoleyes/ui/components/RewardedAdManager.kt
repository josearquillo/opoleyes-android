package com.opoleyes.ui.components

import android.app.Activity
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Manages loading and showing a rewarded ad.
 * Uses the test ad unit ID for development; replace with a real ID for production.
 */
object RewardedAdManager {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    // Test rewarded ad unit ID
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    fun showAd(activity: Activity, onReward: () -> Unit, onDismissed: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            // Ad not loaded — load and show immediately
            isLoading = true
            val callback = object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loadedAd: RewardedAd) {
                    rewardedAd = loadedAd
                    isLoading = false
                    showLoadedAd(loadedAd, activity, onReward, onDismissed)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                    onDismissed()
                }
            }
            RewardedAd.load(activity, AD_UNIT_ID, com.google.android.gms.ads.AdRequest.Builder().build(), callback)
        } else {
            showLoadedAd(ad, activity, onReward, onDismissed)
        }
    }

    private fun showLoadedAd(
        ad: RewardedAd,
        activity: Activity,
        onReward: () -> Unit,
        onDismissed: () -> Unit
    ) {
        ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                rewardedAd = null
                onDismissed()
            }
        }
        ad.show(activity) { onReward() }
    }
}
