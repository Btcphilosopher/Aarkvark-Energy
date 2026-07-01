package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.UtilityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AardvarkApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { UtilityRepository(database.utilityDao()) }

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        // Pre-seed database on application launch
        applicationScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }
}
