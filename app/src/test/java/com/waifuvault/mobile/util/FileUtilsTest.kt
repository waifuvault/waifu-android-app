package com.waifuvault.mobile.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class FileUtilsTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var uri: Uri

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        uri = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `getFileName returns file name from cursor`() {
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
        every { cursor.moveToFirst() } returns true
        every { cursor.getString(0) } returns "test-file.jpg"
        every { contentResolver.query(uri, null, null, null, null) } returns cursor

        val fileName = FileUtils.getFileName(context, uri)

        assertEquals("test-file.jpg", fileName)
        verify { cursor.close() }
    }

    @Test
    fun `getFileName returns null when cursor is empty`() {
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
        every { cursor.moveToFirst() } returns false
        every { contentResolver.query(uri, null, null, null, null) } returns cursor

        val fileName = FileUtils.getFileName(context, uri)

        assertNull(fileName)
        verify { cursor.close() }
    }

    @Test
    fun `getFileName returns null when column index is invalid`() {
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns -1
        every { cursor.moveToFirst() } returns true
        every { contentResolver.query(uri, null, null, null, null) } returns cursor

        val fileName = FileUtils.getFileName(context, uri)

        assertNull(fileName)
        verify { cursor.close() }
    }

    @Test
    fun `getFileName returns null when query returns null`() {
        every { contentResolver.query(uri, null, null, null, null) } returns null

        val fileName = FileUtils.getFileName(context, uri)

        assertNull(fileName)
    }

    @Test
    fun `getFileSize returns file size from cursor`() {
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.getColumnIndex(OpenableColumns.SIZE) } returns 1
        every { cursor.moveToFirst() } returns true
        every { cursor.getLong(1) } returns 1024000L
        every { contentResolver.query(uri, null, null, null, null) } returns cursor

        val fileSize = FileUtils.getFileSize(context, uri)

        assertEquals(1024000L, fileSize)
        verify { cursor.close() }
    }

    @Test
    fun `getFileSize returns 0 when cursor is empty`() {
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.getColumnIndex(OpenableColumns.SIZE) } returns 1
        every { cursor.moveToFirst() } returns false
        every { contentResolver.query(uri, null, null, null, null) } returns cursor

        val fileSize = FileUtils.getFileSize(context, uri)

        assertEquals(0L, fileSize)
        verify { cursor.close() }
    }

    @Test
    fun `getFileSize returns 0 when column index is invalid`() {
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.getColumnIndex(OpenableColumns.SIZE) } returns -1
        every { cursor.moveToFirst() } returns true
        every { contentResolver.query(uri, null, null, null, null) } returns cursor

        val fileSize = FileUtils.getFileSize(context, uri)

        assertEquals(0L, fileSize)
        verify { cursor.close() }
    }

    @Test
    fun `copyUriToFile successfully copies content`() {
        val inputStream = ByteArrayInputStream("test content".toByteArray())
        every { contentResolver.openInputStream(uri) } returns inputStream

        val tempFile = File.createTempFile("test", ".txt")
        tempFile.deleteOnExit()

        val result = FileUtils.copyUriToFile(context, uri, tempFile)

        assertTrue(result)
        assertTrue(tempFile.exists())
        assertEquals("test content", tempFile.readText())
        tempFile.delete()
    }

    @Test
    fun `copyUriToFile returns false on exception`() {
        every { contentResolver.openInputStream(uri) } throws Exception("IO Error")

        val tempFile = File.createTempFile("test", ".txt")
        tempFile.deleteOnExit()

        val result = FileUtils.copyUriToFile(context, uri, tempFile)

        assertFalse(result)
        tempFile.delete()
    }

    @Test
    fun `copyUriToFile returns true when input stream is null`() {
        every { contentResolver.openInputStream(uri) } returns null

        val tempFile = File.createTempFile("test", ".txt")
        tempFile.deleteOnExit()

        val result = FileUtils.copyUriToFile(context, uri, tempFile)

        assertTrue(result)
        tempFile.delete()
    }

    @Test
    fun `formatFileSize formats bytes correctly`() {
        assertEquals("512 B", FileUtils.formatFileSize(512))
    }

    @Test
    fun `formatFileSize formats kilobytes correctly`() {
        assertEquals("1.50 KB", FileUtils.formatFileSize(1536))
    }

    @Test
    fun `formatFileSize formats megabytes correctly`() {
        assertEquals("2.50 MB", FileUtils.formatFileSize(2621440))
    }

    @Test
    fun `formatFileSize formats gigabytes correctly`() {
        assertEquals("1.50 GB", FileUtils.formatFileSize(1610612736))
    }

    @Test
    fun `formatFileSize handles zero bytes`() {
        assertEquals("0 B", FileUtils.formatFileSize(0))
    }

    @Test
    fun `formatFileSize handles 1 KB boundary`() {
        assertEquals("1.00 KB", FileUtils.formatFileSize(1024))
    }

    @Test
    fun `formatFileSize handles 1 MB boundary`() {
        assertEquals("1.00 MB", FileUtils.formatFileSize(1048576))
    }

    @Test
    fun `formatFileSize handles 1 GB boundary`() {
        assertEquals("1.00 GB", FileUtils.formatFileSize(1073741824))
    }

    @Test
    fun `formatFileSize handles large file sizes`() {
        val largeSize = 5368709120L
        assertEquals("5.00 GB", FileUtils.formatFileSize(largeSize))
    }
}
