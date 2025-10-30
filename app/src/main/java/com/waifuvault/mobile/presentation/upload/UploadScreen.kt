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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: UploadViewModel,
    onNavigateToFileList: () -> Unit
) {
    val context = LocalContext.current
    val uploadState by viewModel.uploadState.collectAsState()
    val uploadOptions by viewModel.uploadOptions.collectAsState()

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableLongStateOf(0L) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = FileUtils.getFileName(context, it)
            selectedFileSize = FileUtils.getFileSize(context, it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WaifuVault") },
                actions = {
                    IconButton(onClick = onNavigateToFileList) {
                        Icon(Icons.Default.List, contentDescription = "View Files")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedFileUri != null && uploadState !is UploadState.Uploading) {
                FloatingActionButton(
                    onClick = {
                        selectedFileUri?.let { uri ->
                            val tempFile = File(context.cacheDir, selectedFileName ?: "temp_file")
                            if (FileUtils.copyUriToFile(context, uri, tempFile)) {
                                viewModel.uploadFile(tempFile)
                            }
                        }
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
                    Text("Select File", style = MaterialTheme.typography.titleMedium)

                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose File")
                    }

                    if (selectedFileName != null) {
                        Divider()
                        Text("Selected: $selectedFileName")
                        Text("Size: ${FileUtils.formatFileSize(selectedFileSize)}")
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
                    UploadSuccessCard(file = state.file, onDismiss = { viewModel.resetUploadState() })
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
fun UploadSuccessCard(file: com.waifuvault.mobile.domain.model.WaifuFile, onDismiss: () -> Unit) {
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
                Text("Upload Successful!", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("URL: ${file.url}", style = MaterialTheme.typography.bodySmall)
            Text("Token: ${file.token}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Text("Copy URL")
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

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Dismiss")
            }
        }
    }
}
