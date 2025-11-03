package moe.waifuvault.mobile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.waifuvault.mobile.data.repository.FileRepository
import moe.waifuvault.mobile.presentation.upload.UploadScreen
import moe.waifuvault.mobile.presentation.upload.UploadViewModel
import moe.waifuvault.mobile.ui.theme.WaifuVaultTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private var sharedFiles: List<File>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle shared files from intent
        sharedFiles = handleSharedIntent(intent)

        setContent {
            WaifuVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WaifuVaultApp(sharedFiles)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        sharedFiles = handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent): List<File>? {
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                // Handle single file
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                uri?.let { listOfNotNull(copyUriToFile(it)) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                // Handle multiple files
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                uris?.mapNotNull { uri -> copyUriToFile(uri) }
            }
            else -> null
        }
    }

    private fun copyUriToFile(uri: Uri): File? {
        return try {
            val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else {
                    "shared_file_${System.currentTimeMillis()}"
                }
            } ?: "shared_file_${System.currentTimeMillis()}"

            val tempFile = File(cacheDir, fileName)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Composable
fun WaifuVaultApp(sharedFiles: List<File>? = null) {
    val viewModel: UploadViewModel = viewModel(
        factory = UploadViewModelFactory(FileRepository())
    )

    // Handle shared files upload and cleanup
    LaunchedEffect(sharedFiles) {
        sharedFiles?.let { files ->
            if (files.isNotEmpty()) {
                viewModel.uploadFiles(files, shouldCleanup = true)
            }
        }
    }

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
