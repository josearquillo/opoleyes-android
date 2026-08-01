package com.opoleyes

import android.app.Application

class OpoleyesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // MobileAds.initialize(this) — temporarily disabled for dev
    }
}
