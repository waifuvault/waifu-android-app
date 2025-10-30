package com.waifuvault.mobile.domain.usecase

import com.waifuvault.mobile.data.repository.FileRepository
import com.waifuvault.mobile.domain.model.FileUploadOptions
import com.waifuvault.mobile.domain.model.WaifuFile
import java.io.File

class UploadFileUseCase(
    private val repository: FileRepository
) {

    suspend fun uploadDirect(
        file: File,
        options: FileUploadOptions
    ): Result<WaifuFile> {
        return repository.uploadFile(file, options)
    }
}
