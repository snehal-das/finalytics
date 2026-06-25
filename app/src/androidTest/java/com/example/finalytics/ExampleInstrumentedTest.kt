package com.example.finalytics

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.finalytics.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.finalytics", appContext.packageName)
    }

    @After
    fun tearDown() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.getDatabase(appContext)
        runBlocking {
            db.clearAllTables()
        }
    }
}