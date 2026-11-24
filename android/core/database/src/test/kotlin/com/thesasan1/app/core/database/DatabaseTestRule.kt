package com.thesasan1.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource

/** A fresh in-memory [AppDatabase] per test. */
class DatabaseTestRule : ExternalResource() {
    lateinit var db: AppDatabase
        private set

    override fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    override fun after() {
        db.close()
    }
}
