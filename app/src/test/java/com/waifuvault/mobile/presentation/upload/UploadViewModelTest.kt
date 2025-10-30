package com.waifuvault.mobile.presentation.upload

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.waifuvault.mobile.data.repository.FileRepository
import com.waifuvault.mobile.domain.model.FileOptions
import com.waifuvault.mobile.domain.model.UploadState
import com.waifuvault.mobile.domain.model.WaifuFile
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
            retentionPeriod = "1h"
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
            retentionPeriod = "2d"
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
            retentionPeriod = "1h"
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
            retentionPeriod = "1h"
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
            retentionPeriod = "1h"
        )
        val waifuFile2 = WaifuFile(
            token = "token2",
            url = "https://waifuvault.moe/f/file2.png",
            options = FileOptions(),
            retentionPeriod = "1h"
        )
        val waifuFile3 = WaifuFile(
            token = "token3",
            url = "https://waifuvault.moe/f/file3.gif",
            options = FileOptions(),
            retentionPeriod = "1h"
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
            retentionPeriod = "1h"
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
}
