package com.salaryapp.jigong.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import com.salaryapp.jigong.data.local.dao.PhotoBatchDao
import com.salaryapp.jigong.data.local.dao.SiteDao
import com.salaryapp.jigong.data.local.dao.WorkRecordDao
import com.salaryapp.jigong.data.local.dao.WorkerDao
import com.salaryapp.jigong.data.local.entity.PhotoBatchEntity
import com.salaryapp.jigong.data.local.entity.PhotoItemEntity
import com.salaryapp.jigong.data.local.entity.SiteEntity
import com.salaryapp.jigong.data.local.entity.WorkRecordEntity
import com.salaryapp.jigong.data.local.entity.WorkerEntity

@Database(
    entities = [
        WorkerEntity::class,
        SiteEntity::class,
        WorkRecordEntity::class,
        PhotoBatchEntity::class,
        PhotoItemEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workerDao(): WorkerDao
    abstract fun siteDao(): SiteDao
    abstract fun workRecordDao(): WorkRecordDao
    abstract fun photoBatchDao(): PhotoBatchDao

    companion object {
        val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS work_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        workDate INTEGER NOT NULL,
                        workerId INTEGER NOT NULL,
                        workerNameSnapshot TEXT NOT NULL,
                        siteId INTEGER,
                        siteNameSnapshot TEXT,
                        durationText TEXT,
                        unitPriceText TEXT,
                        amount TEXT NOT NULL,
                        remark TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS work_records_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        workDate INTEGER NOT NULL,
                        workerId INTEGER,
                        workerNameSnapshot TEXT NOT NULL,
                        siteId INTEGER,
                        siteNameSnapshot TEXT,
                        durationText TEXT,
                        unitPriceText TEXT,
                        amount TEXT NOT NULL,
                        remark TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO work_records_new (
                        id, workDate, workerId, workerNameSnapshot, siteId, siteNameSnapshot,
                        durationText, unitPriceText, amount, remark, createdAt, updatedAt
                    )
                    SELECT
                        id, workDate, workerId, workerNameSnapshot, siteId, siteNameSnapshot,
                        durationText, unitPriceText, amount, remark, createdAt, updatedAt
                    FROM work_records
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE work_records")
                db.execSQL("ALTER TABLE work_records_new RENAME TO work_records")
            }
        }

        val Migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS photo_batches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        workDate INTEGER NOT NULL,
                        siteId INTEGER,
                        siteNameSnapshot TEXT NOT NULL,
                        remark TEXT,
                        photoCount INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS photo_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        batchId INTEGER NOT NULL,
                        localUri TEXT NOT NULL,
                        localPath TEXT NOT NULL,
                        originalFileName TEXT,
                        width INTEGER,
                        height INTEGER,
                        sizeBytes INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(batchId) REFERENCES photo_batches(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_photo_items_batchId ON photo_items(batchId)"
                )
            }
        }

        val Migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE work_records ADD COLUMN phoneNumberSnapshot TEXT")
            }
        }
    }
}
