package com.waifuvault.mobile.presentation.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.waifuvault.mobile.domain.model.UploadState
import com.waifuvault.mobile.util.FileUtils
import java.io.File

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val size: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: UploadViewModel
) {
    val context = LocalContext.current
    val uploadState by viewModel.uploadState.collectAsState()
    val uploadOptions by viewModel.uploadOptions.collectAsState()

    var selectedFiles by remember { mutableStateOf<List<SelectedFile>>(emptyList()) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedFiles = uris.map { uri ->
            SelectedFile(
                uri = uri,
                name = FileUtils.getFileName(context, uri) ?: "Unknown",
                size = FileUtils.getFileSize(context, uri)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WaifuVault") }
            )
        },
        floatingActionButton = {
            if (selectedFiles.isNotEmpty() && uploadState !is UploadState.Uploading && uploadState !is UploadState.UploadingMultiple) {
                FloatingActionButton(
                    onClick = {
                        val filesToUpload = selectedFiles.mapIndexedNotNull { index, selectedFile ->
                            val uniqueName = "${System.currentTimeMillis()}_${index}_${selectedFile.name}"
                            val tempFile = File(context.cacheDir, uniqueName)
                            if (FileUtils.copyUriToFile(context, selectedFile.uri, tempFile)) {
                                tempFile
                            } else null
                        }
                        viewModel.uploadFiles(filesToUpload)
                    }
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Upload")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // File Selection
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Select Files", style = MaterialTheme.typography.titleMedium)

                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Files")
                    }

                    if (selectedFiles.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Selected ${selectedFiles.size} file(s):",
                            style = MaterialTheme.typography.labelMedium)
                        selectedFiles.forEach { file ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        FileUtils.formatFileSize(file.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    selectedFiles = selectedFiles.filter { it != file }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                }
            }

            // Upload Options
            UploadOptionsCard(
                options = uploadOptions,
                onOptionsChange = { viewModel.updateUploadOptions(it) }
            )

            // Upload Status
            when (val state = uploadState) {
                is UploadState.Idle -> {}
                is UploadState.Uploading -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Uploading... ${state.progress}%")
                        }
                    }
                }

                is UploadState.UploadingMultiple -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Uploading Files", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))

                            state.files.forEach { fileProgress ->
                                FileUploadProgressItem(fileProgress)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                is UploadState.Processing -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Processing upload...")
                        }
                    }
                }

                is UploadState.Success -> {
                    UploadSuccessCard(
                        files = state.allFiles,
                        onDismiss = { viewModel.resetUploadState() }
                    )
                }

                is UploadState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Upload Failed", style = MaterialTheme.typography.titleMedium)
                            Text(state.message)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.resetUploadState() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UploadOptionsCard(
    options: UploadOptions,
    onOptionsChange: (UploadOptions) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Upload Options", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hide Filename")
                Switch(
                    checked = options.hideFilename,
                    onCheckedChange = { onOptionsChange(options.copy(hideFilename = it)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("One-Time Download")
                Switch(
                    checked = options.oneTimeDownload,
                    onCheckedChange = { onOptionsChange(options.copy(oneTimeDownload = it)) }
                )
            }

            var showPasswordField by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Password Protection")
                Switch(
                    checked = showPasswordField,
                    onCheckedChange = {
                        showPasswordField = it
                        if (!it) onOptionsChange(options.copy(password = null))
                    }
                )
            }

            if (showPasswordField) {
                OutlinedTextField(
                    value = options.password ?: "",
                    onValueChange = { onOptionsChange(options.copy(password = it)) },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            var expiryInput by remember { mutableStateOf("") }
            OutlinedTextField(
                value = expiryInput,
                onValueChange = {
                    expiryInput = it
                    onOptionsChange(options.copy(expires = it.ifBlank { null }))
                },
                label = { Text("Expiry (e.g., 1h, 30m, 2d)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Leave empty for default") }
            )
        }
    }
}

@Composable
fun FileUploadProgressItem(progress: com.waifuvault.mobile.domain.model.FileUploadProgress) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(progress.fileName, style = MaterialTheme.typography.bodyMedium)
            when (val status = progress.status) {
                is com.waifuvault.mobile.domain.model.FileUploadStatus.Pending -> {
                    Text("Waiting...", style = MaterialTheme.typography.bodySmall)
                }
                is com.waifuvault.mobile.domain.model.FileUploadStatus.Uploading -> {
                    Text("Uploading...", style = MaterialTheme.typography.bodySmall)
                }
                is com.waifuvault.mobile.domain.model.FileUploadStatus.Success -> {
                    Text("Complete", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                is com.waifuvault.mobile.domain.model.FileUploadStatus.Error -> {
                    Text("Error: ${status.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        when (progress.status) {
            is com.waifuvault.mobile.domain.model.FileUploadStatus.Pending -> {
                Icon(Icons.Default.Schedule, contentDescription = "Pending", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is com.waifuvault.mobile.domain.model.FileUploadStatus.Uploading -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            is com.waifuvault.mobile.domain.model.FileUploadStatus.Success -> {
                Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = MaterialTheme.colorScheme.primary)
            }
            is com.waifuvault.mobile.domain.model.FileUploadStatus.Error -> {
                Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun UploadSuccessCard(files: List<com.waifuvault.mobile.domain.model.WaifuFile>, onDismiss: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (files.size == 1) "Upload Successful!" else "Uploaded ${files.size} files!",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            files.forEach { file ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("URL: ${file.url}", style = MaterialTheme.typography.bodySmall)
                    Text("Token: ${file.token}", style = MaterialTheme.typography.bodySmall)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                        as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("WaifuVault URL", file.url)
                                clipboard.setPrimaryClip(clip)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy")
                        }

                        Button(
                            onClick = {
                                val shareIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, file.url)
                                    type = "text/plain"
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share via"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                }

                if (file != files.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Dismiss")
            }
        }
    }
}
