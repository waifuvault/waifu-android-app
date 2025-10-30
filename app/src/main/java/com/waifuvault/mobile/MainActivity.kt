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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.waifuvault.mobile.data.repository.FileRepository
import com.waifuvault.mobile.presentation.filemanagement.FileManagementScreen
import com.waifuvault.mobile.presentation.filemanagement.FileManagementViewModel
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
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "upload") {
        composable("upload") {
            val viewModel: UploadViewModel = viewModel(
                factory = UploadViewModelFactory(FileRepository())
            )
            UploadScreen(
                viewModel = viewModel,
                onNavigateToFileList = { navController.navigate("files") }
            )
        }

        composable("files") {
            val viewModel: FileManagementViewModel = viewModel(
                factory = FileManagementViewModelFactory(FileRepository())
            )
            FileManagementScreen(
                viewModel = viewModel,
                onNavigateToUpload = { navController.navigate("upload") }
            )
        }
    }
}

// ViewModelFactory classes
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

class FileManagementViewModelFactory(
    private val repository: FileRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FileManagementViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
