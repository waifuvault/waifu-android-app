package com.waifuvault.mobile.domain.model

import org.junit.Assert.*
import org.junit.Test

class UploadStateTest {

    @Test
    fun `Idle state is singleton`() {
        val state1 = UploadState.Idle
        val state2 = UploadState.Idle

        assertSame(state1, state2)
    }

    @Test
    fun `Uploading state holds progress`() {
        val uploadingState = UploadState.Uploading(progress = 45)

        assertEquals(45, uploadingState.progress)
    }

    @Test
    fun `Uploading state with zero progress`() {
        val uploadingState = UploadState.Uploading(progress = 0)

        assertEquals(0, uploadingState.progress)
    }

    @Test
    fun `Uploading state with full progress`() {
        val uploadingState = UploadState.Uploading(progress = 100)

        assertEquals(100, uploadingState.progress)
    }

    @Test
    fun `Processing state is singleton`() {
        val state1 = UploadState.Processing
        val state2 = UploadState.Processing

        assertSame(state1, state2)
    }

    @Test
    fun `Success state holds WaifuFile`() {
        val waifuFile = WaifuFile(
            token = "token123",
            url = "https://waifuvault.moe/f/file.jpg",
            options = FileOptions(),
            retentionPeriod = "1h"
        )

        val successState = UploadState.Success(waifuFile)

        assertEquals(waifuFile, successState.file)
        assertEquals("token123", successState.file.token)
    }

    @Test
    fun `Error state holds message and exception`() {
        val exception = Exception("Network error")
        val errorState = UploadState.Error("Upload failed", exception)

        assertEquals("Upload failed", errorState.message)
        assertEquals(exception, errorState.exception)
        assertEquals("Network error", errorState.exception?.message)
    }

    @Test
    fun `Error state without exception`() {
        val errorState = UploadState.Error("Unknown error")

        assertEquals("Unknown error", errorState.message)
        assertNull(errorState.exception)
    }

    @Test
    fun `All states are distinct sealed class types`() {
        val idle = UploadState.Idle
        val uploading = UploadState.Uploading(50)
        val processing = UploadState.Processing
        val success = UploadState.Success(
            WaifuFile("token", "url", FileOptions(), "1h")
        )
        val error = UploadState.Error("error")

        assertNotEquals(idle::class, uploading::class)
        assertNotEquals(uploading::class, processing::class)
        assertNotEquals(processing::class, success::class)
        assertNotEquals(success::class, error::class)
    }
}
