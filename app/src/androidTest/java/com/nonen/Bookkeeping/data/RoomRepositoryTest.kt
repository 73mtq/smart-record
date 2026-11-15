package com.nonen.Bookkeeping.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.nonen.Bookkeeping.data.db.AppDatabase
import org.junit.After
import org.junit.Before

abstract class RoomRepositoryTest {
    protected lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "com.nonen.Bookkeeping.uivalidation")
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }
}
