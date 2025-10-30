package com.waifuvault.mobile.data.repository

import com.waifuvault.mobile.data.remote.WaifuVaultApi
import com.waifuvault.mobile.data.remote.dto.FileOptionsDto
import com.waifuvault.mobile.data.remote.dto.WaifuFileDto
import com.waifuvault.mobile.domain.model.FileUploadOptions
import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.File

class FileRepositoryTest {

    private lateinit var api: WaifuVaultApi
    private lateinit var repository: FileRepository
    private lateinit var testFile: File

    @Before
    fun setup() {
        api = mockk()
        repository = FileRepository(api)

        testFile = File.createTempFile("test", ".jpg")
        testFile.writeText("test content")
    }

    @After
    fun teardown() {
        if (testFile.exists()) {
            testFile.delete()
        }
        unmockkAll()
    }

    @Test
    fun `uploadFile success returns WaifuFile`() = runTest {
        val options = FileUploadOptions(
            expires = "1h",
            hideFilename = true,
            password = "password123",
            oneTimeDownload = true,
            bucketToken = null
        )

        val mockDto = WaifuFileDto(
            token = "token123",
            url = "https://waifuvault.moe/f/test.jpg",
            options = FileOptionsDto(
                hideFilename = true,
                oneTimeDownload = true,
                protected = true
            ),
            retentionPeriod = "1h",
            bucket = null,
            id = 1,
            views = 0
        )

        coEvery {
            api.uploadFile(
                file = any(),
                expires = any(),
                hideFilename = any(),
                password = any(),
                oneTimeDownload = any(),
                bucketToken = any()
            )
        } returns Response.success(mockDto)

        val result = repository.uploadFile(testFile, options)

        assertTrue(result.isSuccess)
        val waifuFile = result.getOrNull()
        assertNotNull(waifuFile)
        assertEquals("token123", waifuFile?.token)
        assertEquals("https://waifuvault.moe/f/test.jpg", waifuFile?.url)
        assertEquals("1h", waifuFile?.retentionPeriod)
        assertTrue(waifuFile?.options?.hideFilename ?: false)
    }

    @Test
    fun `uploadFile failure returns error Result`() = runTest {
        val options = FileUploadOptions()

        coEvery {
            api.uploadFile(
                file = any(),
                expires = any(),
                hideFilename = any(),
                password = any(),
                oneTimeDownload = any(),
                bucketToken = any()
            )
        } returns Response.error(500, "Server error".toResponseBody())

        val result = repository.uploadFile(testFile, options)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Upload failed") ?: false)
    }

    @Test
    fun `uploadFile with network exception returns failure`() = runTest {
        val options = FileUploadOptions()

        coEvery {
            api.uploadFile(
                file = any(),
                expires = any(),
                hideFilename = any(),
                password = any(),
                oneTimeDownload = any(),
                bucketToken = any()
            )
        } throws Exception("Network timeout")

        val result = repository.uploadFile(testFile, options)

        assertTrue(result.isFailure)
        assertEquals("Network timeout", result.exceptionOrNull()?.message)
    }

    @Test
    fun `uploadFile with minimal options`() = runTest {
        val options = FileUploadOptions()

        val mockDto = WaifuFileDto(
            token = "simple-token",
            url = "https://waifuvault.moe/f/simple.png",
            options = FileOptionsDto(),
            retentionPeriod = "24h"
        )

        coEvery {
            api.uploadFile(
                file = any(),
                expires = any(),
                hideFilename = any(),
                password = any(),
                oneTimeDownload = any(),
                bucketToken = any()
            )
        } returns Response.success(mockDto)

        val result = repository.uploadFile(testFile, options)

        assertTrue(result.isSuccess)
        val waifuFile = result.getOrNull()
        assertNotNull(waifuFile)
        assertEquals("simple-token", waifuFile?.token)
        assertFalse(waifuFile?.options?.hideFilename ?: true)
        assertFalse(waifuFile?.options?.oneTimeDownload ?: true)
    }
}
