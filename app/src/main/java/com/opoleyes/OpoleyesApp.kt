package com.opoleyes

import android.app.Application
import com.google.android.gms.ads.MobileAds

class OpoleyesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}
