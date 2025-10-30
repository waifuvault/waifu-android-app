package com.waifuvault.mobile.domain.usecase

import com.waifuvault.mobile.data.repository.FileRepository
import com.waifuvault.mobile.domain.model.WaifuBucket

class GetBucketUseCase(private val repository: FileRepository) {

    suspend operator fun invoke(token: String): Result<WaifuBucket> {
        return repository.getBucket(token)
    }
}
