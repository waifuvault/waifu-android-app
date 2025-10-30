package com.waifuvault.mobile.domain.model

import org.junit.Assert.*
import org.junit.Test

class WaifuFileTest {

    @Test
    fun `WaifuFile creation with all fields`() {
        val options = FileOptions(
            hideFilename = true,
            oneTimeDownload = true,
            protected = true
        )

        val waifuFile = WaifuFile(
            token = "test-token-123",
            url = "https://waifuvault.moe/f/test-file.jpg",
            options = options,
            retentionPeriod = "1h",
            bucket = "bucket-123",
            id = 456,
            views = 10
        )

        assertEquals("test-token-123", waifuFile.token)
        assertEquals("https://waifuvault.moe/f/test-file.jpg", waifuFile.url)
        assertEquals("1h", waifuFile.retentionPeriod)
        assertEquals("bucket-123", waifuFile.bucket)
        assertEquals(456, waifuFile.id)
        assertEquals(10, waifuFile.views)
        assertTrue(waifuFile.options.hideFilename)
        assertTrue(waifuFile.options.oneTimeDownload)
        assertTrue(waifuFile.options.protected)
    }

    @Test
    fun `WaifuFile creation with minimal fields`() {
        val options = FileOptions()

        val waifuFile = WaifuFile(
            token = "test-token",
            url = "https://waifuvault.moe/f/file.png",
            options = options,
            retentionPeriod = "30m"
        )

        assertEquals("test-token", waifuFile.token)
        assertEquals("https://waifuvault.moe/f/file.png", waifuFile.url)
        assertEquals("30m", waifuFile.retentionPeriod)
        assertNull(waifuFile.bucket)
        assertNull(waifuFile.id)
        assertNull(waifuFile.views)
        assertFalse(waifuFile.options.hideFilename)
        assertFalse(waifuFile.options.oneTimeDownload)
        assertFalse(waifuFile.options.protected)
    }

    @Test
    fun `FileOptions defaults to false`() {
        val options = FileOptions()

        assertFalse(options.hideFilename)
        assertFalse(options.oneTimeDownload)
        assertFalse(options.protected)
    }

    @Test
    fun `FileUploadOptions creation with all options`() {
        val uploadOptions = FileUploadOptions(
            expires = "2d",
            hideFilename = true,
            password = "secure123",
            oneTimeDownload = true,
            bucketToken = "bucket-token"
        )

        assertEquals("2d", uploadOptions.expires)
        assertTrue(uploadOptions.hideFilename)
        assertEquals("secure123", uploadOptions.password)
        assertTrue(uploadOptions.oneTimeDownload)
        assertEquals("bucket-token", uploadOptions.bucketToken)
    }

    @Test
    fun `FileUploadOptions creation with defaults`() {
        val uploadOptions = FileUploadOptions()

        assertNull(uploadOptions.expires)
        assertFalse(uploadOptions.hideFilename)
        assertNull(uploadOptions.password)
        assertFalse(uploadOptions.oneTimeDownload)
        assertNull(uploadOptions.bucketToken)
    }

    @Test
    fun `FileUploadOptions with only password`() {
        val uploadOptions = FileUploadOptions(
            password = "mypassword"
        )

        assertNull(uploadOptions.expires)
        assertFalse(uploadOptions.hideFilename)
        assertEquals("mypassword", uploadOptions.password)
        assertFalse(uploadOptions.oneTimeDownload)
        assertNull(uploadOptions.bucketToken)
    }
}
