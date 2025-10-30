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
    val expires: String? = null,
    val hideFilename: Boolean = false,
    val password: String? = null,
    val oneTimeDownload: Boolean = false,
    val bucketToken: String? = null
)
