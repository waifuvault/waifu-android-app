package com.waifuvault.mobile.presentation.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waifuvault.mobile.data.repository.FileRepository
import com.waifuvault.mobile.domain.model.FileUploadOptions
import com.waifuvault.mobile.domain.model.UploadState
import com.waifuvault.mobile.domain.usecase.UploadFileUseCase
import com.waifuvault.mobile.util.ChunkUploadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UploadViewModel(
    private val repository: FileRepository
) : ViewModel() {

    private val chunkUploadManager = ChunkUploadManager(repository)
    private val uploadFileUseCase = UploadFileUseCase(repository, chunkUploadManager)

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _uploadOptions = MutableStateFlow(
        UploadOptions(
            expires = null,
            hideFilename = false,
            password = null,
            oneTimeDownload = false
        )
    )
    val uploadOptions: StateFlow<UploadOptions> = _uploadOptions.asStateFlow()

    fun uploadFile(file: File) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading(0, 0, 1)

            val options = FileUploadOptions(
                expires = _uploadOptions.value.expires,
                hideFilename = _uploadOptions.value.hideFilename,
                password = _uploadOptions.value.password,
                oneTimeDownload = _uploadOptions.value.oneTimeDownload,
                bucketToken = null  // No bucket token needed
            )

            val result = uploadFileUseCase.uploadDirect(file, options)

            _uploadState.value = if (result.isSuccess) {
                UploadState.Success(result.getOrNull()!!)
            } else {
                UploadState.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error",
                    result.exceptionOrNull() as? Exception
                )
            }
        }
    }

    fun updateUploadOptions(options: UploadOptions) {
        _uploadOptions.value = options
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }
}

data class UploadOptions(
    val expires: String?,
    val hideFilename: Boolean,
    val password: String?,
    val oneTimeDownload: Boolean
)
