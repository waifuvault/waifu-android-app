package com.waifuvault.mobile.domain.model

data class FileUploadProgress(
    val fileName: String,
    val status: FileUploadStatus
)

sealed class FileUploadStatus {
    data object Pending : FileUploadStatus()
    data object Uploading : FileUploadStatus()
    data class Success(val file: WaifuFile) : FileUploadStatus()
    data class Error(val message: String) : FileUploadStatus()
}

sealed class UploadState {
    data object Idle : UploadState()
    data class Uploading(val progress: Int) : UploadState()
    data class UploadingMultiple(val files: List<FileUploadProgress>) : UploadState()
    data object Processing : UploadState()
    data class Success(val file: WaifuFile, val allFiles: List<WaifuFile> = listOf()) : UploadState()
    data class Error(val message: String, val exception: Exception? = null) : UploadState()
}
