package moe.waifuvault.mobile.domain.usecase

import moe.waifuvault.mobile.data.repository.FileRepository
import moe.waifuvault.mobile.domain.model.FileUploadOptions
import moe.waifuvault.mobile.domain.model.WaifuFile
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
