package com.init.file.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.init.file.data.local.InitDatabaseHelper
import com.init.file.data.local.LocalRepository
import com.init.file.data.storage.FileManager

/**
 * Background WorkManager worker for storage profiling and automatic 30-day trash cleanup.
 */
class StorageScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val dbHelper = InitDatabaseHelper(applicationContext)
            val repository = LocalRepository(dbHelper)
            repository.purgeExpiredTrashItems(30)

            val fileManager = FileManager(applicationContext)
            val volumes = fileManager.getStorageVolumes()
            for (volume in volumes) {
                fileManager.analyzeStorage(volume)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
