package com.waifuvault.mobile.domain.model

sealed class UploadState {
    data object Idle : UploadState()
    data class Uploading(val progress: Int, val currentChunk: Int, val totalChunks: Int) : UploadState()
    data object Processing : UploadState()
    data class Success(val file: WaifuFile) : UploadState()
    data class Error(val message: String, val exception: Exception? = null) : UploadState()
}

data class ChunkInfo(
    val index: Int,
    val size: Long,
    val offset: Long,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChunkInfo

        if (index != other.index) return false
        if (size != other.size) return false
        if (offset != other.offset) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + size.hashCode()
        result = 31 * result + offset.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

data class UploadProgress(
    val uploadedBytes: Long,
    val totalBytes: Long,
    val percentage: Int,
    val currentChunk: Int,
    val totalChunks: Int
) {
    companion object {
        fun calculate(uploadedBytes: Long, totalBytes: Long, currentChunk: Int, totalChunks: Int): UploadProgress {
            val percentage = if (totalBytes > 0) {
                ((uploadedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
            } else 0

            return UploadProgress(
                uploadedBytes = uploadedBytes,
                totalBytes = totalBytes,
                percentage = percentage,
                currentChunk = currentChunk,
                totalChunks = totalChunks
            )
        }
    }
}
