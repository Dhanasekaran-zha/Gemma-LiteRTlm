package com.utils.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for StoredMedia model.
 */
class StoredMediaTest {

    @Test
    fun `StoredMedia creation with all fields`() {
        val media = StoredMedia(
            localUri = "/data/app/chat_media/img_123.jpg",
            mimeType = "image/jpeg",
            fileSize = 1024L,
            width = 720,
            height = 480
        )

        assertEquals("/data/app/chat_media/img_123.jpg", media.localUri)
        assertEquals("image/jpeg", media.mimeType)
        assertEquals(1024L, media.fileSize)
        assertEquals(720, media.width)
        assertEquals(480, media.height)
    }

    @Test
    fun `StoredMedia copy creates independent instance`() {
        val original = StoredMedia(
            localUri = "/path/original.jpg",
            mimeType = "image/jpeg",
            fileSize = 2048L,
            width = 1440,
            height = 1080
        )

        val copy = original.copy(localUri = "/path/copy.jpg")

        assertEquals("/path/copy.jpg", copy.localUri)
        assertEquals(original.mimeType, copy.mimeType)
        assertEquals(original.fileSize, copy.fileSize)
    }

    @Test
    fun `StoredMedia equality by value`() {
        val media1 = StoredMedia("uri", "mime", 100, 50, 50)
        val media2 = StoredMedia("uri", "mime", 100, 50, 50)

        assertEquals(media1, media2)
        assertEquals(media1.hashCode(), media2.hashCode())
    }
}
