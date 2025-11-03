package moe.waifuvault.mobile.data.repository

import moe.waifuvault.mobile.data.remote.ApiClient
import moe.waifuvault.mobile.data.remote.WaifuVaultApi
import moe.waifuvault.mobile.data.remote.dto.*
import moe.waifuvault.mobile.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class FileRepository(
    private val api: WaifuVaultApi = ApiClient.waifuVaultApi
) {

    suspend fun getRestrictions(): Result<Restrictions> = withContext(Dispatchers.IO) {
        try {
            val response = api.getRestrictions()

            if (response.isSuccessful && response.body() != null) {
                val restrictions = response.body()!!
                var maxFileSize = Restrictions.DEFAULT.maxFileSize
                val bannedMimeTypes = mutableSetOf<String>()

                restrictions.forEach { restriction ->
                    when (restriction.type) {
                        "MAX_FILE_SIZE" -> {
                            maxFileSize = restriction.value.toLongOrNull() ?: maxFileSize
                        }
                        "BANNED_MIME_TYPE" -> {
                            bannedMimeTypes.addAll(
                                restriction.value.split(",").map { it.trim() }
                            )
                        }
                    }
                }

                Result.success(
                    Restrictions(
                        maxFileSize = maxFileSize,
                        bannedMimeTypes = bannedMimeTypes
                    )
                )
            } else {
                // Return default restrictions if API call fails
                Result.success(Restrictions.DEFAULT)
            }
        } catch (e: Exception) {
            // Return default restrictions on error
            Result.success(Restrictions.DEFAULT)
        }
    }

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
                Result.success(response.body()!!.toDomain(file.name))
            } else {
                val errorMessage = try {
                    response.errorBody()?.string()?.let { errorBody ->
                        val errorResponse = Json.decodeFromString<ErrorResponse>(errorBody)
                        errorResponse.message
                    }
                } catch (_: Exception) {
                    null
                }
                Result.failure(Exception(errorMessage ?: "Upload failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun WaifuFileDto.toDomain(fileName: String) = WaifuFile(
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
    views = views,
    fileName = fileName
)
