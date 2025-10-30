package com.waifuvault.mobile.domain.model

data class WaifuFile(
    val token: String,
    val url: String,
    val options: FileOptions,
    val retentionPeriod: String,
    val bucket: String? = null,
    val id: Int? = null,
    val views: Int? = null
)

data class FileOptions(
    val hideFilename: Boolean = false,
    val oneTimeDownload: Boolean = false,
    val protected: Boolean = false
)

data class FileUploadOptions(
    val expires: String? = null,          // "1h", "30m", "2d"
    val hideFilename: Boolean = false,
    val password: String? = null,
    val oneTimeDownload: Boolean = false,
    val bucketToken: String? = null
)

data class WaifuBucket(
    val token: String,
    val files: List<WaifuFile> = emptyList()
)

data class WaifuAlbum(
    val token: String,
    val bucketToken: String,
    val publicToken: String? = null,
    val name: String,
    val files: List<WaifuFile> = emptyList(),
    val dateCreated: Long
)

data class Restrictions(
    val maxFileSize: Long,
    val bannedMimeTypes: List<String> = emptyList(),
    val retentionPeriods: List<String> = emptyList()
)
