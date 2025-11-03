package moe.waifuvault.mobile.domain.model

import java.util.Locale

data class Restrictions(
    val maxFileSize: Long,
    val bannedMimeTypes: Set<String>
) {
    companion object {
        val DEFAULT = Restrictions(
            maxFileSize = 2145386496L, // ~2GB
            bannedMimeTypes = setOf(
                "application/x-dosexec",
                "application/x-executable",
                "application/x-hdf5",
                "application/x-java-archive",
                "application/vnd.rar"
            )
        )
    }

    fun isFileSizeValid(fileSize: Long): Boolean {
        return fileSize <= maxFileSize
    }

    fun isMimeTypeAllowed(mimeType: String?): Boolean {
        if (mimeType == null) {
            return true
        }
        return !bannedMimeTypes.contains(mimeType)
    }

    fun formatMaxFileSize(): String {
        val sizeInMB = maxFileSize / (1024 * 1024)
        return if (sizeInMB >= 1024) {
            val sizeInGB = sizeInMB / 1024.0
            String.format(Locale.UK, "%.1f GB", sizeInGB)
        } else {
            "$sizeInMB MB"
        }
    }
}

sealed class ValidationError(val message: String) {
    data class FileTooLarge(val fileName: String, val fileSize: Long, val maxSize: Long) :
        ValidationError("File '$fileName' is too large")

    data class BannedMimeType(val fileName: String, val mimeType: String) :
        ValidationError("File '$fileName' has a banned file type: $mimeType")
}
