package com.waifuvault.mobile.domain.usecase

import com.waifuvault.mobile.data.repository.FileRepository
import com.waifuvault.mobile.domain.model.FileUploadOptions
import com.waifuvault.mobile.domain.model.WaifuFile
import com.waifuvault.mobile.util.ChunkUploadManager
import kotlinx.coroutines.flow.Flow
import java.io.File

class UploadFileUseCase(
    private val repository: FileRepository,
    private val chunkUploadManager: ChunkUploadManager
) {

    suspend fun uploadWithChunks(
        file: File,
        options: FileUploadOptions
    ): Flow<Result<WaifuFile>> {
        return chunkUploadManager.uploadFileInChunks(file, options)
    }

    suspend fun uploadDirect(
        file: File,
        options: FileUploadOptions
    ): Result<WaifuFile> {
        return repository.uploadFile(file, options)
    }
}
