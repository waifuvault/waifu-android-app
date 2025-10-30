package com.waifuvault.mobile.util

import com.waifuvault.mobile.data.repository.FileRepository
import com.waifuvault.mobile.domain.model.ChunkInfo
import com.waifuvault.mobile.domain.model.FileUploadOptions
import com.waifuvault.mobile.domain.model.UploadProgress
import com.waifuvault.mobile.domain.model.WaifuFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

class ChunkUploadManager(
    private val repository: FileRepository,
    private val chunkSize: Int = DEFAULT_CHUNK_SIZE
) {

    companion object {
        const val DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024 // 5MB
        const val MAX_CONCURRENT_UPLOADS = 3
        const val MAX_RETRIES = 3
        const val INITIAL_RETRY_DELAY = 1000L // 1 second
    }

    suspend fun uploadFileInChunks(
        file: File,
        options: FileUploadOptions
    ): Flow<Result<WaifuFile>> = flow {
        try {
            val fileSize = file.length()

            // If file is smaller than chunk size, upload directly
            if (fileSize <= chunkSize) {
                val result = repository.uploadFile(file, options)
                emit(result)
                return@flow
            }

            // Create chunks
            val chunks = createChunks(file)
            val totalChunks = chunks.size

            // For now, we'll upload the whole file at once
            // In a production app, you would implement the chunking logic
            // similar to your Next.js implementation with temporary files
            // and a finalize endpoint

            // Since WaifuVault API doesn't natively support chunked uploads,
            // we upload the complete file in one request
            val result = repository.uploadFile(file, options)
            emit(result)

        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private suspend fun createChunks(file: File): List<ChunkInfo> = withContext(Dispatchers.IO) {
        val chunks = mutableListOf<ChunkInfo>()
        val fileSize = file.length()
        var offset = 0L
        var chunkIndex = 0

        RandomAccessFile(file, "r").use { raf ->
            while (offset < fileSize) {
                val remainingBytes = fileSize - offset
                val currentChunkSize = minOf(chunkSize.toLong(), remainingBytes)

                val buffer = ByteArray(currentChunkSize.toInt())
                raf.seek(offset)
                raf.readFully(buffer)

                chunks.add(
                    ChunkInfo(
                        index = chunkIndex,
                        size = currentChunkSize,
                        offset = offset,
                        data = buffer
                    )
                )

                offset += currentChunkSize
                chunkIndex++
            }
        }

        chunks
    }

    fun calculateProgress(uploadedBytes: Long, totalBytes: Long, currentChunk: Int, totalChunks: Int): UploadProgress {
        return UploadProgress.calculate(uploadedBytes, totalBytes, currentChunk, totalChunks)
    }
}
