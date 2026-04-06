package com.salaryapp.jigong.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.salaryapp.jigong.data.local.dao.PhotoBatchDao
import com.salaryapp.jigong.data.local.dao.SiteDao
import com.salaryapp.jigong.data.local.entity.PhotoBatchEntity
import com.salaryapp.jigong.data.local.entity.PhotoBatchWithItems
import com.salaryapp.jigong.data.local.entity.PhotoItemEntity
import com.salaryapp.jigong.domain.model.PhotoBatch
import com.salaryapp.jigong.domain.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PhotoRepository(
    private val context: Context,
    private val photoBatchDao: PhotoBatchDao,
    private val siteDao: SiteDao
) {
    fun observePhotoBatches(): Flow<List<PhotoBatch>> = photoBatchDao.observePhotoBatches().map { batches ->
        batches.map { it.toModel() }
    }

    suspend fun savePhotoBatch(
        input: PhotoBatchInput,
        pickedUris: List<Uri>,
        cameraBitmap: Bitmap?
    ): SavePhotoBatchResult = withContext(Dispatchers.IO) {
        if (input.siteName.isBlank()) {
            return@withContext SavePhotoBatchResult.ValidationError("请选择工地")
        }
        if (pickedUris.isEmpty() && cameraBitmap == null) {
            return@withContext SavePhotoBatchResult.ValidationError("请先选择照片或拍照")
        }

        val site = input.siteId?.let { siteDao.getSiteById(it) }
        if (input.siteId != null && site == null) {
            return@withContext SavePhotoBatchResult.ValidationError("所选工地不存在，请重新选择")
        }

        val persisted = mutableListOf<PersistedPhoto>()
        try {
            if (cameraBitmap != null) {
                persisted += saveBitmapToAppStorage(cameraBitmap)
            }
            pickedUris.forEach { uri ->
                saveUriToAppStorage(uri)?.let { persisted += it }
            }
            if (persisted.isEmpty()) {
                return@withContext SavePhotoBatchResult.ValidationError("没有成功保存任何照片")
            }

            val now = System.currentTimeMillis()
            val batchId = photoBatchDao.insertBatch(
                PhotoBatchEntity(
                    workDate = input.workDate,
                    siteId = site?.id,
                    siteNameSnapshot = site?.siteName ?: input.siteName.trim(),
                    remark = input.remark.trim().ifBlank { null },
                    photoCount = persisted.size,
                    createdAt = now,
                    updatedAt = now
                )
            )
            photoBatchDao.insertItems(
                persisted.map {
                    PhotoItemEntity(
                        batchId = batchId,
                        localUri = it.localUri,
                        localPath = it.localPath,
                        originalFileName = it.originalFileName,
                        width = it.width,
                        height = it.height,
                        sizeBytes = it.sizeBytes,
                        createdAt = now
                    )
                }
            )
            SavePhotoBatchResult.Success(persisted.size)
        } catch (_: Exception) {
            persisted.forEach { deleteIfExists(it.localPath) }
            SavePhotoBatchResult.ValidationError("照片保存失败，请重试")
        }
    }

    suspend fun deletePhotoBatches(ids: List<Long>): Int = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext 0
        val relatedFiles = ids.mapNotNull { photoBatchDao.getBatchWithItems(it) }
            .flatMap { batch -> batch.items.map { it.localPath } }
        val deletedCount = photoBatchDao.deleteBatches(ids)
        relatedFiles.forEach(::deleteIfExists)
        deletedCount
    }

    suspend fun exportPhotoToAlbum(photoItem: PhotoItem): SaveToAlbumResult = withContext(Dispatchers.IO) {
        val source = File(photoItem.localPath)
        if (!source.exists()) {
            return@withContext SaveToAlbumResult.Error("原图不存在，无法下载")
        }

        val fileName = photoItem.originalFileName?.takeIf { it.isNotBlank() }
            ?: "jigong_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + File.separator + "记工相册"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val targetUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext SaveToAlbumResult.Error("系统相册写入失败")

        try {
            resolver.openOutputStream(targetUri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: return@withContext SaveToAlbumResult.Error("无法写入系统相册")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(targetUri, values, null, null)
            SaveToAlbumResult.Success(targetUri)
        } catch (_: Exception) {
            resolver.delete(targetUri, null, null)
            SaveToAlbumResult.Error("保存到系统相册失败")
        }
    }

    private fun saveUriToAppStorage(uri: Uri): PersistedPhoto? {
        val resolver = context.contentResolver
        val fileName = queryDisplayName(uri)
        resolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            if (bytes.isEmpty()) return null
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            return saveBitmapToAppStorage(bitmap, fileName)
        }
        return null
    }

    private fun saveBitmapToAppStorage(bitmap: Bitmap, originalFileName: String? = null): PersistedPhoto {
        val folder = File(context.filesDir, "photo_archive/${dayStamp()}").apply { mkdirs() }
        val safeName = originalFileName?.substringAfterLast('/')?.substringAfterLast('\\')
        val finalName = safeName?.takeIf { it.isNotBlank() } ?: "photo_${UUID.randomUUID()}.jpg"
        val outputFile = File(folder, finalName.ensureJpegExtension())
        FileOutputStream(outputFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, output)
        }
        return PersistedPhoto(
            localUri = Uri.fromFile(outputFile).toString(),
            localPath = outputFile.absolutePath,
            originalFileName = safeName,
            width = bitmap.width,
            height = bitmap.height,
            sizeBytes = outputFile.length()
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        val resolver = context.contentResolver
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    private fun dayStamp(): String = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

    private fun deleteIfExists(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}

data class PhotoBatchInput(
    val workDate: Long,
    val siteId: Long?,
    val siteName: String,
    val remark: String
)

sealed interface SavePhotoBatchResult {
    data class Success(val savedCount: Int) : SavePhotoBatchResult
    data class ValidationError(val message: String) : SavePhotoBatchResult
}

sealed interface SaveToAlbumResult {
    data class Success(val uri: Uri) : SaveToAlbumResult
    data class Error(val message: String) : SaveToAlbumResult
}

private data class PersistedPhoto(
    val localUri: String,
    val localPath: String,
    val originalFileName: String?,
    val width: Int?,
    val height: Int?,
    val sizeBytes: Long?
)

private fun PhotoBatchWithItems.toModel(): PhotoBatch = PhotoBatch(
    id = batch.id,
    workDate = batch.workDate,
    siteId = batch.siteId,
    siteNameSnapshot = batch.siteNameSnapshot,
    remark = batch.remark,
    photoCount = batch.photoCount,
    createdAt = batch.createdAt,
    updatedAt = batch.updatedAt,
    items = items.map {
        PhotoItem(
            id = it.id,
            batchId = it.batchId,
            localUri = it.localUri,
            localPath = it.localPath,
            originalFileName = it.originalFileName,
            width = it.width,
            height = it.height,
            sizeBytes = it.sizeBytes,
            createdAt = it.createdAt
        )
    }
)

private fun String.ensureJpegExtension(): String {
    val lower = lowercase(Locale.getDefault())
    return if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) this else "$this.jpg"
}
