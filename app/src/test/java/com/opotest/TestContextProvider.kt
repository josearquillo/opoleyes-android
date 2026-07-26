package com.opotest

import android.content.Context
import androidx.test.core.app.ApplicationProvider

object TestContextProvider {
    fun getContext(): Context = ApplicationProvider.getApplicationContext()
}

