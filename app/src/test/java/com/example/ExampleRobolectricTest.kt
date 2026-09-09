package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.CpassClinicalEngine
import com.example.model.DailyLogEntity
import com.example.model.PeriodFlow
import org.junit.Assert.assertEquals
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
        assertEquals("Homavales", appName)
    }

    @Test
    fun `test cpass clinical calculation empty logs`() {
        val report = CpassClinicalEngine.evaluateProspectiveTracking(emptyList())
        assertEquals(0, report.totalCyclesEvaluated)
    }
}
