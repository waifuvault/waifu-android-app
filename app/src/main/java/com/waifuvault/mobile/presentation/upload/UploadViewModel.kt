package com.waifuvault.mobile.presentation.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waifuvault.mobile.data.repository.FileRepository
import com.waifuvault.mobile.domain.model.FileUploadOptions
import com.waifuvault.mobile.domain.model.FileUploadProgress
import com.waifuvault.mobile.domain.model.FileUploadStatus
import com.waifuvault.mobile.domain.model.UploadState
import com.waifuvault.mobile.domain.model.WaifuFile
import com.waifuvault.mobile.domain.usecase.UploadFileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UploadViewModel(
    repository: FileRepository
) : ViewModel() {

    private val uploadFileUseCase = UploadFileUseCase(repository)

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
        uploadFiles(listOf(file))
    }

    fun uploadFiles(files: List<File>) {
        if (files.isEmpty()) return

        viewModelScope.launch {
            if (files.size == 1) {
                _uploadState.value = UploadState.Uploading(0)
            } else {
                val initialProgress = files.map { file ->
                    FileUploadProgress(
                        fileName = file.name,
                        status = FileUploadStatus.Pending
                    )
                }
                _uploadState.value = UploadState.UploadingMultiple(initialProgress)
            }

            val options = FileUploadOptions(
                expires = _uploadOptions.value.expires,
                hideFilename = _uploadOptions.value.hideFilename,
                password = _uploadOptions.value.password,
                oneTimeDownload = _uploadOptions.value.oneTimeDownload,
                bucketToken = null
            )

            val uploadedFiles = mutableListOf<WaifuFile>()
            val errors = mutableListOf<String>()

            files.forEachIndexed { index, file ->
                if (files.size == 1) {
                    val progress = ((index + 1) * 100 / files.size)
                    _uploadState.value = UploadState.Uploading(progress)
                } else {
                    val currentState = _uploadState.value
                    if (currentState is UploadState.UploadingMultiple) {
                        val updatedProgress = currentState.files.toMutableList()
                        updatedProgress[index] = updatedProgress[index].copy(
                            status = FileUploadStatus.Uploading
                        )
                        _uploadState.value = UploadState.UploadingMultiple(updatedProgress)
                    }
                }

                val result = uploadFileUseCase.uploadDirect(file, options)

                if (result.isSuccess) {
                    val waifuFile = result.getOrNull()!!
                    uploadedFiles.add(waifuFile)

                    if (files.size > 1) {
                        val currentState = _uploadState.value
                        if (currentState is UploadState.UploadingMultiple) {
                            val updatedProgress = currentState.files.toMutableList()
                            updatedProgress[index] = updatedProgress[index].copy(
                                status = FileUploadStatus.Success(waifuFile)
                            )
                            _uploadState.value = UploadState.UploadingMultiple(updatedProgress)
                        }
                    }
                } else {
                    val errorMessage = result.exceptionOrNull()?.message
                    errors.add("${file.name}: $errorMessage")

                    if (files.size > 1) {
                        val currentState = _uploadState.value
                        if (currentState is UploadState.UploadingMultiple) {
                            val updatedProgress = currentState.files.toMutableList()
                            updatedProgress[index] = updatedProgress[index].copy(
                                status = FileUploadStatus.Error(errorMessage ?: "Unknown error")
                            )
                            _uploadState.value = UploadState.UploadingMultiple(updatedProgress)
                        }
                    }
                }
            }

            _uploadState.value = when {
                errors.isEmpty() && uploadedFiles.isNotEmpty() -> {
                    UploadState.Success(uploadedFiles.first(), uploadedFiles)
                }
                uploadedFiles.isEmpty() -> {
                    UploadState.Error(
                        errors.joinToString("\n"),
                        Exception(errors.firstOrNull())
                    )
                }
                else -> {
                    UploadState.Success(uploadedFiles.first(), uploadedFiles)
                }
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
