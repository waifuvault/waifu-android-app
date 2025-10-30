package com.waifuvault.mobile.domain.usecase

import com.waifuvault.mobile.data.repository.FileRepository
import com.waifuvault.mobile.domain.model.WaifuFile

class GetFileInfoUseCase(private val repository: FileRepository) {

    suspend operator fun invoke(token: String): Result<WaifuFile> {
        return repository.getFileInfo(token)
    }
}
