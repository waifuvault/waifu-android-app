package com.waifuvault.mobile.data.repository

import com.waifuvault.mobile.data.remote.ApiClient
import com.waifuvault.mobile.data.remote.dto.*
import com.waifuvault.mobile.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class FileRepository {

    private val api = ApiClient.waifuVaultApi

    suspend fun uploadFile(
        file: File,
        options: FileUploadOptions
    ): Result<WaifuFile> = withContext(Dispatchers.IO) {
        try {
            val filePart = MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )

            val expiresBody = options.expires?.toRequestBody()
            val hideFilenameBody = options.hideFilename.toString().toRequestBody()
            val passwordBody = options.password?.toRequestBody()
            val oneTimeDownloadBody = options.oneTimeDownload.toString().toRequestBody()
            val bucketTokenBody = options.bucketToken?.toRequestBody()

            val response = api.uploadFile(
                file = filePart,
                expires = expiresBody,
                hideFilename = hideFilenameBody,
                password = passwordBody,
                oneTimeDownload = oneTimeDownloadBody,
                bucketToken = bucketTokenBody
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Upload failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFileInfo(token: String): Result<WaifuFile> = withContext(Dispatchers.IO) {
        try {
            val response = api.getFileInfo(token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to get file info: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteFile(token)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete file: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(
        identifier: String,
        password: String? = null,
        destinationFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val response = api.downloadFile(identifier, password)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.byteStream().use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Result.success(destinationFile)
            } else {
                Result.failure(Exception("Failed to download file: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun modifyEntry(
        token: String,
        request: ModifyEntryRequest
    ): Result<WaifuFile> = withContext(Dispatchers.IO) {
        try {
            val response = api.modifyEntry(token, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to modify entry: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Bucket operations
    suspend fun createBucket(): Result<WaifuBucket> = withContext(Dispatchers.IO) {
        try {
            val response = api.createBucket()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to create bucket: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBucket(token: String): Result<WaifuBucket> = withContext(Dispatchers.IO) {
        try {
            val response = api.getBucket(token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to get bucket: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBucket(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteBucket(token)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete bucket: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Album operations
    suspend fun createAlbum(name: String, bucketToken: String): Result<WaifuAlbum> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.createAlbum(CreateAlbumRequest(name, bucketToken))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.toDomain())
                } else {
                    Result.failure(Exception("Failed to create album: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getAlbum(token: String): Result<WaifuAlbum> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAlbum(token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to get album: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAlbum(token: String, deleteFiles: Boolean = false): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.deleteAlbum(token, deleteFiles)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete album: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getRestrictions(): Result<Restrictions> = withContext(Dispatchers.IO) {
        try {
            val response = api.getRestrictions()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to get restrictions: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Extension functions to convert DTOs to domain models
private fun WaifuFileDto.toDomain() = WaifuFile(
    token = token,
    url = url,
    options = FileOptions(
        hideFilename = options.hideFilename,
        oneTimeDownload = options.oneTimeDownload,
        protected = options.protected
    ),
    retentionPeriod = retentionPeriod,
    bucket = bucket,
    id = id,
    views = views
)

private fun WaifuBucketDto.toDomain() = WaifuBucket(
    token = token,
    files = files.map { it.toDomain() }
)

private fun WaifuAlbumDto.toDomain() = WaifuAlbum(
    token = token,
    bucketToken = bucketToken,
    publicToken = publicToken,
    name = name,
    files = files.map { it.toDomain() },
    dateCreated = dateCreated
)

private fun RestrictionsDto.toDomain() = Restrictions(
    maxFileSize = maxFileSize,
    bannedMimeTypes = bannedMimeTypes,
    retentionPeriods = retentionPeriods
)
