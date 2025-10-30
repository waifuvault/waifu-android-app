package com.waifuvault.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WaifuFileDto(
    val token: String,
    val url: String,
    val options: FileOptionsDto,
    val retentionPeriod: String,
    val bucket: String? = null,
    val id: Int? = null,
    val views: Int? = null
)

@Serializable
data class FileOptionsDto(
    val hideFilename: Boolean = false,
    val oneTimeDownload: Boolean = false,
    val protected: Boolean = false
)

@Serializable
data class FileUploadRequest(
    val expires: String? = null,          // "1h", "30m", "2d"
    val hideFilename: Boolean = false,
    val password: String? = null,
    val oneTimeDownload: Boolean = false,
    val bucketToken: String? = null
)

@Serializable
data class ModifyEntryRequest(
    val password: String? = null,
    val previousPassword: String? = null,
    val customExpiry: String? = null,
    val hideFilename: Boolean? = null
)

@Serializable
data class WaifuBucketDto(
    val token: String,
    val files: List<WaifuFileDto> = emptyList()
)

@Serializable
data class WaifuAlbumDto(
    val token: String,
    val bucketToken: String,
    val publicToken: String? = null,
    val name: String,
    val files: List<WaifuFileDto> = emptyList(),
    val dateCreated: Long
)

@Serializable
data class CreateAlbumRequest(
    val name: String,
    val bucketToken: String
)

@Serializable
data class AssociateFilesRequest(
    val fileTokens: List<String>
)

@Serializable
data class RestrictionsDto(
    val maxFileSize: Long,
    val bannedMimeTypes: List<String> = emptyList(),
    val retentionPeriods: List<String> = emptyList()
)

@Serializable
data class ErrorResponse(
    val name: String,
    val message: String,
    val status: Int
)
