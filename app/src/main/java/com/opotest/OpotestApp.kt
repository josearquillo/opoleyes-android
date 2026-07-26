package com.opotest

import android.app.Application
import com.google.android.gms.ads.MobileAds

class OpotestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}
