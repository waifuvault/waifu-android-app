package com.waifuvault.mobile.domain.usecase

import com.waifuvault.mobile.data.repository.FileRepository

class DeleteFileUseCase(private val repository: FileRepository) {

    suspend operator fun invoke(token: String): Result<Unit> {
        return repository.deleteFile(token)
    }
}
