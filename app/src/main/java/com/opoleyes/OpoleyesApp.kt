package com.opoleyes

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.opoleyes.BuildConfig

class OpoleyesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.ADS_ENABLED) {
            MobileAds.initialize(this)
        }
    }
}
