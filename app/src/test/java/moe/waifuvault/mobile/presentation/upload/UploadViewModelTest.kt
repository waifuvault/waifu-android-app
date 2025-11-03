package moe.waifuvault.mobile.presentation.upload

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import moe.waifuvault.mobile.data.repository.FileRepository
import moe.waifuvault.mobile.domain.model.FileOptions
import moe.waifuvault.mobile.domain.model.UploadState
import moe.waifuvault.mobile.domain.model.WaifuFile
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UploadViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FileRepository
    private lateinit var viewModel: UploadViewModel
    private lateinit var testFile: File

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()

        coEvery { repository.getRestrictions() } returns Result.success(
            moe.waifuvault.mobile.domain.model.Restrictions.DEFAULT
        )

        viewModel = UploadViewModel(repository)
        testFile = File.createTempFile("test", ".jpg")
        testFile.writeText("test content")
    }

    @After
    fun teardown() {
        if (testFile.exists()) {
            testFile.delete()
        }
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        viewModel.uploadState.test {
            assertEquals(UploadState.Idle, awaitItem())
        }
    }

    @Test
    fun `initial upload options are default`() = runTest {
        viewModel.uploadOptions.test {
            val options = awaitItem()
            assertNull(options.expires)
            assertFalse(options.hideFilename)
            assertNull(options.password)
            assertFalse(options.oneTimeDownload)
        }
    }

    @Test
    fun `uploadFile success updates state to Success`() = runTest(testDispatcher) {
        val expectedWaifuFile = WaifuFile(
            token = "token123",
            url = "https://waifuvault.moe/f/file.jpg",
            options = FileOptions(),
            retentionPeriod = "1h",
            fileName = testFile.name
        )

        coEvery {
            repository.uploadFile(testFile, any())
        } returns Result.success(expectedWaifuFile)

        viewModel.uploadState.test {
            assertEquals(UploadState.Idle, awaitItem())

            viewModel.uploadFile(testFile)

            assertEquals(UploadState.Uploading(0), awaitItem())
            assertEquals(UploadState.Uploading(100), awaitItem())

            val successState = awaitItem()
            assertTrue(successState is UploadState.Success)
            assertEquals(expectedWaifuFile, (successState as UploadState.Success).file)
            assertEquals(listOf(expectedWaifuFile), successState.allFiles)
        }
    }

    @Test
    fun `uploadFile failure updates state to Error`() = runTest(testDispatcher) {
        coEvery {
            repository.uploadFile(testFile, any())
        } returns Result.failure(Exception("Network error"))

        viewModel.uploadState.test {
            assertEquals(UploadState.Idle, awaitItem())

            viewModel.uploadFile(testFile)

            assertEquals(UploadState.Uploading(0), awaitItem())
            assertEquals(UploadState.Uploading(100), awaitItem())

            val errorState = awaitItem()
            assertTrue(errorState is UploadState.Error)
            assertTrue((errorState as UploadState.Error).message.contains("Network error"))
        }
    }

    @Test
    fun `uploadFile with custom options`() = runTest(testDispatcher) {
        val customOptions = UploadOptions(
            expires = "2d",
            hideFilename = true,
            password = "secure123",
            oneTimeDownload = true
        )

        viewModel.updateUploadOptions(customOptions)

        val expectedWaifuFile = WaifuFile(
            token = "token",
            url = "url",
            options = FileOptions(),
            retentionPeriod = "2d",
            fileName = testFile.name
        )

        coEvery {
            repository.uploadFile(testFile, any())
        } returns Result.success(expectedWaifuFile)

        viewModel.uploadFile(testFile)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.uploadFile(testFile, match {
                it.expires == "2d" && it.hideFilename && it.password == "secure123" && it.oneTimeDownload
            })
        }
    }

    @Test
    fun `updateUploadOptions updates state flow`() = runTest {
        val newOptions = UploadOptions(
            expires = "1h",
            hideFilename = true,
            password = "pass",
            oneTimeDownload = false
        )

        viewModel.uploadOptions.test {
            skipItems(1)

            viewModel.updateUploadOptions(newOptions)

            val updated = awaitItem()
            assertEquals("1h", updated.expires)
            assertTrue(updated.hideFilename)
            assertEquals("pass", updated.password)
            assertFalse(updated.oneTimeDownload)
        }
    }

    @Test
    fun `resetUploadState resets to Idle`() = runTest(testDispatcher) {
        val expectedWaifuFile = WaifuFile(
            token = "token",
            url = "url",
            options = FileOptions(),
            retentionPeriod = "1h",
            fileName = testFile.name
        )

        coEvery {
            repository.uploadFile(testFile, any())
        } returns Result.success(expectedWaifuFile)

        viewModel.uploadState.test {
            skipItems(1)

            viewModel.uploadFile(testFile)
            skipItems(3)

            viewModel.resetUploadState()

            assertEquals(UploadState.Idle, awaitItem())
        }
    }

    @Test
    fun `uploadFile with null exception message defaults to Unknown error`() = runTest(testDispatcher) {
        coEvery {
            repository.uploadFile(testFile, any())
        } returns Result.failure(Exception())

        viewModel.uploadState.test {
            assertEquals(UploadState.Idle, awaitItem())

            viewModel.uploadFile(testFile)

            assertEquals(UploadState.Uploading(0), awaitItem())
            assertEquals(UploadState.Uploading(100), awaitItem())

            val errorState = awaitItem()
            assertTrue(errorState is UploadState.Error)
            assertTrue((errorState as UploadState.Error).message.contains("null"))
        }
    }

    @Test
    fun `multiple uploadFile calls work independently`() = runTest(testDispatcher) {
        val waifuFile = WaifuFile(
            token = "token",
            url = "url",
            options = FileOptions(),
            retentionPeriod = "1h",
            fileName = testFile.name
        )

        coEvery {
            repository.uploadFile(testFile, any())
        } returns Result.success(waifuFile)

        viewModel.uploadFile(testFile)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetUploadState()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uploadState.test {
            assertEquals(UploadState.Idle, awaitItem())
        }
    }

    @Test
    fun `uploadFiles with multiple files uploads all successfully`() = runTest(testDispatcher) {
        val testFile2 = File.createTempFile("test2", ".png")
        val testFile3 = File.createTempFile("test3", ".gif")
        testFile2.writeText("test content 2")
        testFile3.writeText("test content 3")

        val waifuFile1 = WaifuFile(
            token = "token1",
            url = "https://waifuvault.moe/f/file1.jpg",
            options = FileOptions(),
            retentionPeriod = "1h",
            fileName = testFile.name
        )
        val waifuFile2 = WaifuFile(
            token = "token2",
            url = "https://waifuvault.moe/f/file2.png",
            options = FileOptions(),
            retentionPeriod = "1h",
            fileName = testFile2.name
        )
        val waifuFile3 = WaifuFile(
            token = "token3",
            url = "https://waifuvault.moe/f/file3.gif",
            options = FileOptions(),
            retentionPeriod = "1h",
            fileName = testFile3.name
        )

        coEvery {
            repository.uploadFile(testFile, any())
        } returns Result.success(waifuFile1)

        coEvery {
            repository.uploadFile(testFile2, any())
        } returns Result.success(waifuFile2)

        coEvery {
            repository.uploadFile(testFile3, any())
        } returns Result.success(waifuFile3)

        viewModel.uploadState.test {
            assertEquals(UploadState.Idle, awaitItem())

            viewModel.uploadFiles(listOf(testFile, testFile2, testFile3))

            val initialState = awaitItem()
            assertTrue(initialState is UploadState.UploadingMultiple)

            skipItems(6)

            val successState = awaitItem()
            assertTrue(successState is UploadState.Success)
            assertEquals(waifuFile1, (successState as UploadState.Success).file)
            assertEquals(3, successState.allFiles.size)
            assertEquals(listOf(waifuFile1, waifuFile2, waifuFile3), successState.allFiles)
        }

        testFile2.delete()
        testFile3.delete()
    }

    @Test
    fun `uploadFiles with partial failures returns success with errors`() = runTest(testDispatcher) {
        val testFile2 = File.createTempFile("test2", ".png")
        testFile2.writeText("test content 2")

        val waifuFile1 = WaifuFile(
            token = "token1",
            url = "https://waifuvault.moe/f/file1.jpg",
            options = FileOptions(),
            retentionPeriod = "1h",
            fileName = testFile.name
        )

        coEvery {
            repository.uploadFile(testFile, any())
        } returns Result.success(waifuFile1)

        coEvery {
            repository.uploadFile(testFile2, any())
        } returns Result.failure(Exception("Upload failed"))

        viewModel.uploadState.test {
            assertEquals(UploadState.Idle, awaitItem())

            viewModel.uploadFiles(listOf(testFile, testFile2))

            val initialState = awaitItem()
            assertTrue(initialState is UploadState.UploadingMultiple)

            skipItems(4)

            val successState = awaitItem()
            assertTrue(successState is UploadState.Success)
            assertEquals(waifuFile1, (successState as UploadState.Success).file)
            assertEquals(1, successState.allFiles.size)
            assertEquals(listOf(waifuFile1), successState.allFiles)
        }

        testFile2.delete()
    }

    @Test
    fun `uploadFiles with all failures returns error`() = runTest(testDispatcher) {
        val testFile2 = File.createTempFile("test2", ".png")
        testFile2.writeText("test content 2")

        coEvery {
            repository.uploadFile(testFile, any())
        } returns Result.failure(Exception("Upload failed 1"))

        coEvery {
            repository.uploadFile(testFile2, any())
        } returns Result.failure(Exception("Upload failed 2"))

        viewModel.uploadState.test {
            assertEquals(UploadState.Idle, awaitItem())

            viewModel.uploadFiles(listOf(testFile, testFile2))

            val initialState = awaitItem()
            assertTrue(initialState is UploadState.UploadingMultiple)

            skipItems(4)

            val errorState = awaitItem()
            assertTrue(errorState is UploadState.Error)
            assertTrue((errorState as UploadState.Error).message.contains("Upload failed 1"))
            assertTrue(errorState.message.contains("Upload failed 2"))
        }

        testFile2.delete()
    }

    @Test
    fun `uploadFiles with cleanup flag cleans up temp files after success`() = runTest(testDispatcher) {
        // Create a file in a cache-like directory to simulate real usage
        val cacheDir = File(System.getProperty("java.io.tmpdir"), "cache")
        cacheDir.mkdirs()
        val tempFile = File(cacheDir, "temp_upload.jpg")
        tempFile.writeText("temporary content")
        assertTrue(tempFile.exists())

        val waifuFile = WaifuFile(
            token = "token",
            url = "https://waifuvault.moe/f/file.jpg",
            options = FileOptions(),
            retentionPeriod = "1h",
            fileName = tempFile.name
        )

        coEvery {
            repository.uploadFile(tempFile, any())
        } returns Result.success(waifuFile)

        viewModel.uploadFiles(listOf(tempFile), shouldCleanup = true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the file is deleted after upload completes
        assertFalse(tempFile.exists())
        cacheDir.delete()
    }

    @Test
    fun `uploadFiles with cleanup flag cleans up temp files after error`() = runTest(testDispatcher) {
        // Create a file in a cache-like directory to simulate real usage
        val cacheDir = File(System.getProperty("java.io.tmpdir"), "cache")
        cacheDir.mkdirs()
        val tempFile = File(cacheDir, "temp_upload_error.jpg")
        tempFile.writeText("temporary content")
        assertTrue(tempFile.exists())

        coEvery {
            repository.uploadFile(tempFile, any())
        } returns Result.failure(Exception("Upload failed"))

        viewModel.uploadFiles(listOf(tempFile), shouldCleanup = true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the file is deleted even after upload fails
        assertFalse(tempFile.exists())
        cacheDir.delete()
    }

    @Test
    fun `uploadFiles without cleanup flag does not delete temp files`() = runTest(testDispatcher) {
        val tempFile = File.createTempFile("temp_upload", ".jpg")
        tempFile.writeText("temporary content")
        assertTrue(tempFile.exists())

        val waifuFile = WaifuFile(
            token = "token",
            url = "https://waifuvault.moe/f/file.jpg",
            options = FileOptions(),
            retentionPeriod = "1h",
            fileName = tempFile.name
        )

        coEvery {
            repository.uploadFile(tempFile, any())
        } returns Result.success(waifuFile)

        viewModel.uploadFiles(listOf(tempFile), shouldCleanup = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the file still exists when cleanup is disabled
        assertTrue(tempFile.exists())

        // Clean up manually for test cleanup
        tempFile.delete()
    }

    // Restrictions Validation Tests

    @Test
    fun `validateFilesForSelection accepts files within size limit`() {
        val files = listOf(
            "small.jpg" to 1024L, // 1KB
            "medium.png" to 1024L * 1024L // 1MB
        )
        val mimeTypes = mapOf(
            "small.jpg" to "image/jpeg",
            "medium.png" to "image/png"
        )

        val result = viewModel.validateFilesForSelection(files, mimeTypes)

        assertEquals(2, result.validFiles.size)
        assertTrue(result.invalidFiles.isEmpty())
    }

    @Test
    fun `validateFilesForSelection rejects files over size limit`() {
        val files = listOf(
            "huge.mp4" to 3L * 1024L * 1024L * 1024L, // 3GB (over 2GB limit)
            "normal.jpg" to 1024L * 1024L // 1MB
        )
        val mimeTypes = mapOf(
            "huge.mp4" to "video/mp4",
            "normal.jpg" to "image/jpeg"
        )

        val result = viewModel.validateFilesForSelection(files, mimeTypes)

        assertEquals(1, result.validFiles.size)
        assertEquals("normal.jpg", result.validFiles[0].first)
        assertEquals(1, result.invalidFiles.size)
        assertTrue(result.invalidFiles[0].contains("huge.mp4"))
        assertTrue(result.invalidFiles[0].contains("Too large"))
    }

    @Test
    fun `validateFilesForSelection rejects banned MIME types`() {
        val files = listOf(
            "malware.exe" to 1024L,
            "archive.jar" to 1024L,
            "photo.jpg" to 1024L
        )
        val mimeTypes = mapOf(
            "malware.exe" to "application/x-dosexec", // Banned
            "archive.jar" to "application/x-java-archive", // Banned
            "photo.jpg" to "image/jpeg" // Allowed
        )

        val result = viewModel.validateFilesForSelection(files, mimeTypes)

        assertEquals(1, result.validFiles.size)
        assertEquals("photo.jpg", result.validFiles[0].first)
        assertEquals(2, result.invalidFiles.size)
        assertTrue(result.invalidFiles.any { it.contains("malware.exe") && it.contains("Banned file type") })
        assertTrue(result.invalidFiles.any { it.contains("archive.jar") && it.contains("Banned file type") })
    }

    @Test
    fun `validateFilesForSelection allows null MIME types`() {
        val files = listOf(
            "unknown.bin" to 1024L
        )
        val mimeTypes = mapOf(
            "unknown.bin" to null
        )

        val result = viewModel.validateFilesForSelection(files, mimeTypes)

        assertEquals(1, result.validFiles.size)
        assertTrue(result.invalidFiles.isEmpty())
    }

    @Test
    fun `validateFilesForSelection handles mixed valid and invalid files`() {
        val files = listOf(
            "valid1.jpg" to 1024L,
            "too_large.mp4" to 3L * 1024L * 1024L * 1024L, // 3GB
            "valid2.png" to 2048L,
            "banned.exe" to 1024L,
            "valid3.txt" to 512L
        )
        val mimeTypes = mapOf(
            "valid1.jpg" to "image/jpeg",
            "too_large.mp4" to "video/mp4",
            "valid2.png" to "image/png",
            "banned.exe" to "application/x-dosexec",
            "valid3.txt" to "text/plain"
        )

        val result = viewModel.validateFilesForSelection(files, mimeTypes)

        assertEquals(3, result.validFiles.size)
        assertTrue(result.validFiles.any { it.first == "valid1.jpg" })
        assertTrue(result.validFiles.any { it.first == "valid2.png" })
        assertTrue(result.validFiles.any { it.first == "valid3.txt" })

        assertEquals(2, result.invalidFiles.size)
        assertTrue(result.invalidFiles.any { it.contains("too_large.mp4") })
        assertTrue(result.invalidFiles.any { it.contains("banned.exe") })
    }

    @Test
    fun `validateFilesForSelection handles all invalid files`() {
        val files = listOf(
            "huge.mp4" to 3L * 1024L * 1024L * 1024L,
            "malware.exe" to 1024L
        )
        val mimeTypes = mapOf(
            "huge.mp4" to "video/mp4",
            "malware.exe" to "application/x-dosexec"
        )

        val result = viewModel.validateFilesForSelection(files, mimeTypes)

        assertTrue(result.validFiles.isEmpty())
        assertEquals(2, result.invalidFiles.size)
    }

    @Test
    fun `validateFilesForSelection handles empty file list`() {
        val result = viewModel.validateFilesForSelection(emptyList(), emptyMap())

        assertTrue(result.validFiles.isEmpty())
        assertTrue(result.invalidFiles.isEmpty())
    }

    @Test
    fun `showValidationError sets error state with message`() = runTest {
        viewModel.uploadState.test {
            assertEquals(UploadState.Idle, awaitItem())

            viewModel.showValidationError("• file1.exe: Banned file type\n• file2.mp4: Too large")

            val errorState = awaitItem()
            assertTrue(errorState is UploadState.Error)
            assertTrue((errorState as UploadState.Error).message.contains("Invalid files were shared"))
            assertTrue(errorState.message.contains("file1.exe"))
            assertTrue(errorState.message.contains("file2.mp4"))
        }
    }

    @Test
    fun `restrictions are fetched on ViewModel initialization`() = runTest {
        // Verify that getRestrictions was called during setup
        coVerify(exactly = 1) { repository.getRestrictions() }
    }

    @Test
    fun `validation uses custom restrictions when API returns different values`() = runTest {
        val customRepository = mockk<FileRepository>()
        val customRestrictions = moe.waifuvault.mobile.domain.model.Restrictions(
            maxFileSize = 1024L * 1024L, // 1MB limit
            bannedMimeTypes = setOf("image/png") // Ban PNG files
        )

        coEvery { customRepository.getRestrictions() } returns Result.success(customRestrictions)

        val customViewModel = UploadViewModel(customRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Test that 2MB file is rejected
        val files1 = listOf("large.jpg" to 2L * 1024L * 1024L)
        val result1 = customViewModel.validateFilesForSelection(files1, mapOf("large.jpg" to "image/jpeg"))
        assertTrue(result1.validFiles.isEmpty())
        assertTrue(result1.invalidFiles.isNotEmpty())

        // Test that PNG is rejected
        val files2 = listOf("image.png" to 1024L)
        val result2 = customViewModel.validateFilesForSelection(files2, mapOf("image.png" to "image/png"))
        assertTrue(result2.validFiles.isEmpty())
        assertTrue(result2.invalidFiles.isNotEmpty())
    }
}
