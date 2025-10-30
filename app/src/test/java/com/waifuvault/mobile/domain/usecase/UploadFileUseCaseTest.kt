package com.waifuvault.mobile.domain.usecase

import com.waifuvault.mobile.data.repository.FileRepository
import com.waifuvault.mobile.domain.model.FileOptions
import com.waifuvault.mobile.domain.model.FileUploadOptions
import com.waifuvault.mobile.domain.model.WaifuFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class UploadFileUseCaseTest {

    private lateinit var repository: FileRepository
    private lateinit var useCase: UploadFileUseCase
    private lateinit var testFile: File

    @Before
    fun setup() {
        repository = mockk()
        useCase = UploadFileUseCase(repository)
        testFile = File.createTempFile("test", ".jpg")
        testFile.writeText("test content")
    }

    @After
    fun teardown() {
        if (testFile.exists()) {
            testFile.delete()
        }
    }

    @Test
    fun `uploadDirect success returns WaifuFile`() = runTest {
        val options = FileUploadOptions(
            expires = "2d",
            hideFilename = true,
            password = "secure",
            oneTimeDownload = false
        )

        val expectedFile = WaifuFile(
            token = "test-token",
            url = "https://waifuvault.moe/f/file.jpg",
            options = FileOptions(
                hideFilename = true,
                oneTimeDownload = false,
                protected = true
            ),
            retentionPeriod = "2d"
        )

        coEvery { repository.uploadFile(testFile, options) } returns Result.success(expectedFile)

        val result = useCase.uploadDirect(testFile, options)

        assertTrue(result.isSuccess)
        assertEquals(expectedFile, result.getOrNull())
        coVerify(exactly = 1) { repository.uploadFile(testFile, options) }
    }

    @Test
    fun `uploadDirect failure returns error Result`() = runTest {
        val options = FileUploadOptions()

        val expectedException = Exception("Upload failed")
        coEvery { repository.uploadFile(testFile, options) } returns Result.failure(expectedException)

        val result = useCase.uploadDirect(testFile, options)

        assertTrue(result.isFailure)
        assertEquals("Upload failed", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { repository.uploadFile(testFile, options) }
    }

    @Test
    fun `uploadDirect with minimal options`() = runTest {
        val options = FileUploadOptions()

        val expectedFile = WaifuFile(
            token = "minimal-token",
            url = "https://waifuvault.moe/f/minimal.png",
            options = FileOptions(),
            retentionPeriod = "24h"
        )

        coEvery { repository.uploadFile(testFile, options) } returns Result.success(expectedFile)

        val result = useCase.uploadDirect(testFile, options)

        assertTrue(result.isSuccess)
        val waifuFile = result.getOrNull()
        assertNotNull(waifuFile)
        assertEquals("minimal-token", waifuFile?.token)
        assertFalse(waifuFile?.options?.hideFilename ?: true)
        assertFalse(waifuFile?.options?.oneTimeDownload ?: true)
    }

    @Test
    fun `uploadDirect delegates to repository correctly`() = runTest {
        val options = FileUploadOptions(
            expires = "1h",
            password = "test123"
        )

        val expectedFile = WaifuFile(
            token = "token",
            url = "url",
            options = FileOptions(protected = true),
            retentionPeriod = "1h"
        )

        coEvery { repository.uploadFile(testFile, options) } returns Result.success(expectedFile)

        useCase.uploadDirect(testFile, options)

        coVerify { repository.uploadFile(testFile, options) }
    }

    @Test
    fun `uploadDirect with network error`() = runTest {
        val options = FileUploadOptions()

        coEvery { repository.uploadFile(testFile, options) } returns
            Result.failure(Exception("Network timeout"))

        val result = useCase.uploadDirect(testFile, options)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Network timeout") ?: false)
    }
}
