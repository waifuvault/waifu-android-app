package moe.waifuvault.mobile.presentation.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import moe.waifuvault.mobile.data.repository.FileRepository
import moe.waifuvault.mobile.domain.model.FileUploadOptions
import moe.waifuvault.mobile.domain.model.FileUploadProgress
import moe.waifuvault.mobile.domain.model.FileUploadStatus
import moe.waifuvault.mobile.domain.model.UploadState
import moe.waifuvault.mobile.domain.model.WaifuFile
import moe.waifuvault.mobile.domain.usecase.UploadFileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UploadViewModel(
    private val repository: FileRepository
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

    private var tempFilesToCleanup = mutableListOf<File>()
    private var restrictions: moe.waifuvault.mobile.domain.model.Restrictions? = null

    init {
        // Fetch restrictions when ViewModel is created
        viewModelScope.launch {
            val result = repository.getRestrictions()
            restrictions = result.getOrNull()
        }
    }

    fun uploadFile(file: File) {
        uploadFiles(listOf(file))
    }

    data class FileValidationResult(
        val validFiles: List<Pair<String, Long>>, // name, size pairs
        val invalidFiles: List<String> // error messages
    )

    fun validateFilesForSelection(files: List<Pair<String, Long>>, mimeTypes: Map<String, String?>): FileValidationResult {
        val validFiles = mutableListOf<Pair<String, Long>>()
        val invalidFiles = mutableListOf<String>()
        val currentRestrictions = restrictions ?: moe.waifuvault.mobile.domain.model.Restrictions.DEFAULT

        files.forEach { (name, size) ->
            val mimeType = mimeTypes[name]

            // Check file size
            if (!currentRestrictions.isFileSizeValid(size)) {
                invalidFiles.add("$name: Too large (${moe.waifuvault.mobile.util.FileUtils.formatFileSize(size)}). Max: ${currentRestrictions.formatMaxFileSize()}")
            }
            // Check MIME type
            else if (!currentRestrictions.isMimeTypeAllowed(mimeType)) {
                invalidFiles.add("$name: Banned file type (${mimeType ?: "unknown"})")
            } else {
                validFiles.add(Pair(name, size))
            }
        }

        return FileValidationResult(validFiles, invalidFiles)
    }

    fun uploadFiles(files: List<File>, shouldCleanup: Boolean = false) {
        if (shouldCleanup) {
            tempFilesToCleanup.addAll(files)
        }
        if (files.isEmpty()) return

        viewModelScope.launch {
            // Files are already validated at selection time, so proceed directly to upload
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
            cleanupTempFiles()
        }
    }

    private fun cleanupTempFiles() {
        tempFilesToCleanup.forEach { file ->
            try {
                if (file.exists() && file.absolutePath.contains("cache")) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        tempFilesToCleanup.clear()
    }

    fun updateUploadOptions(options: UploadOptions) {
        _uploadOptions.value = options
    }

    fun showValidationError(message: String) {
        _uploadState.value = UploadState.Error(
            "Invalid files were shared:\n$message",
            Exception("File validation failed")
        )
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
