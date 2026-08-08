package com.opoleyes.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.PreferencesManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MissionRepositoryDataTest {

    private lateinit var prefs: PreferencesManager
    private lateinit var missionRepo: MissionRepository

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        prefs = PreferencesManager(ctx)
        prefs.resetAll()
        missionRepo = MissionRepository(ctx)
    }

    @Test
    fun generateDailyMissions_returnsMissionsForToday() {
        val data = missionRepo.generateDailyMissions()
        assertEquals(LocalDate.now().toString(), data.date)
        assertTrue("At least 1 mission", data.missions.isNotEmpty())
    }

    @Test
    fun generateDailyMissions_sameDayReturnsSame() {
        val first = missionRepo.generateDailyMissions()
        val second = missionRepo.generateDailyMissions()
        assertEquals(first.date, second.date)
        assertEquals(first.missions.size, second.missions.size)
    }
}
