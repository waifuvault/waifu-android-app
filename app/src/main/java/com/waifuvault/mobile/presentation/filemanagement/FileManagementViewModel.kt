package com.waifuvault.mobile.presentation.filemanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waifuvault.mobile.data.repository.FileRepository
import com.waifuvault.mobile.domain.model.WaifuBucket
import com.waifuvault.mobile.domain.usecase.DeleteFileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FileManagementViewModel(
    private val repository: FileRepository
) : ViewModel() {

    private val deleteFileUseCase = DeleteFileUseCase(repository)

    private val _uiState = MutableStateFlow<FileManagementUiState>(
        FileManagementUiState.Info("Files are uploaded anonymously. Use waifuvault.moe to manage your uploads.")
    )
    val uiState: StateFlow<FileManagementUiState> = _uiState.asStateFlow()

    fun deleteFile(token: String) {
        viewModelScope.launch {
            val result = deleteFileUseCase(token)
            if (result.isSuccess) {
                // File deleted successfully
            } else {
                // Handle error - could show a snackbar
            }
        }
    }

    fun refresh() {
        // Nothing to refresh for anonymous uploads
    }
}

sealed class FileManagementUiState {
    data object Loading : FileManagementUiState()
    data class Success(val bucket: WaifuBucket) : FileManagementUiState()
    data class Error(val message: String) : FileManagementUiState()
    data class Info(val message: String) : FileManagementUiState()
}
