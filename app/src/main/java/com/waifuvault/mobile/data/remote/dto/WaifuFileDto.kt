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
data class ErrorResponse(
    val name: String,
    val message: String,
    val status: Int
)
