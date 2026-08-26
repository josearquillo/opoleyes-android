package com.opoleyes.ui.components

import android.app.Activity

/**
 * Abstraction over the rewarded ad SDK so it can be faked in tests.
 */
interface RewardedAdProvider {
    /**
     * Show a rewarded ad. Call [onReward] when the ad reward is earned,
     * and [onDismissed] when the ad is closed (or fails to show).
     */
    fun showAd(activity: Activity, onReward: () -> Unit, onDismissed: () -> Unit)
}
