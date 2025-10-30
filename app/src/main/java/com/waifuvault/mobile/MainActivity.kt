package com.waifuvault.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waifuvault.mobile.data.repository.FileRepository
import com.waifuvault.mobile.presentation.upload.UploadScreen
import com.waifuvault.mobile.presentation.upload.UploadViewModel
import com.waifuvault.mobile.ui.theme.WaifuVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WaifuVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WaifuVaultApp()
                }
            }
        }
    }
}

@Composable
fun WaifuVaultApp() {
    val viewModel: UploadViewModel = viewModel(
        factory = UploadViewModelFactory(FileRepository())
    )
    UploadScreen(viewModel = viewModel)
}

class UploadViewModelFactory(
    private val repository: FileRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UploadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UploadViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
