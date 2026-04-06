package com.salaryapp.jigong.data.repository

import android.content.Context
import androidx.room.Room
import com.salaryapp.jigong.core.preference.PreferenceRepository
import com.salaryapp.jigong.data.local.db.AppDatabase

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "jigong.db"
        )
            .addMigrations(AppDatabase.Migration1To2)
            .addMigrations(AppDatabase.Migration2To3)
            .addMigrations(AppDatabase.Migration3To4)
            .addMigrations(AppDatabase.Migration4To5)
            .build()
    }

    val preferenceRepository: PreferenceRepository by lazy {
        PreferenceRepository(appContext)
    }

    val workerRepository: WorkerRepository by lazy {
        WorkerRepository(database.workerDao())
    }

    val siteRepository: SiteRepository by lazy {
        SiteRepository(database.siteDao())
    }

    val workRecordRepository: WorkRecordRepository by lazy {
        WorkRecordRepository(
            workRecordDao = database.workRecordDao(),
            workerDao = database.workerDao(),
            siteDao = database.siteDao()
        )
    }

    val photoRepository: PhotoRepository by lazy {
        PhotoRepository(
            context = appContext,
            photoBatchDao = database.photoBatchDao(),
            siteDao = database.siteDao()
        )
    }

    val salaryStatsExportRepository: SalaryStatsExportRepository by lazy {
        SalaryStatsExportRepository(appContext)
    }
}
