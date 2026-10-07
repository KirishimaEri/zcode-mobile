package com.zcodemobile.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStoreTest {

    private val sample = SavedConnection(
        id = "a1",
        url = "https://zcode.z.ai/remote/v4?sid=d_Test&hash=abc",
        host = "zcode.z.ai",
        name = "My-PC",
        lastUsed = 1759700000000L,
        favorite = true,
    )

    @Test
    fun `encode then decode round-trips all fields`() {
        val decoded = decodeConnections(encodeConnections(listOf(sample)))
        assertEquals(listOf(sample), decoded)
    }

    @Test
    fun `decode handles null blank and malformed json`() {
        assertTrue(decodeConnections(null).isEmpty())
        assertTrue(decodeConnections("").isEmpty())
        assertTrue(decodeConnections("   ").isEmpty())
        assertTrue(decodeConnections("{not json").isEmpty())
    }

    @Test
    fun `decode skips entries without url`() {
        val json = """[
            {"id":"1","url":"","host":"zcode.z.ai","name":"x","lastUsed":1},
            {"id":"2","url":"https://zcode.z.ai/remote/v4","host":"zcode.z.ai","name":"y","lastUsed":2}
        ]"""
        val decoded = decodeConnections(json)
        assertEquals(1, decoded.size)
        assertEquals("2", decoded[0].id)
    }

    @Test
    fun `decode falls back to host when name missing`() {
        val json = """[{"id":"1","url":"https://a.b/c","host":"a.b","name":"","lastUsed":0}]"""
        assertEquals("a.b", decodeConnections(json)[0].name)
    }

    @Test
    fun `favorite defaults to false when absent`() {
        val json = """[{"id":"1","url":"https://a.b/c","host":"a.b","name":"a","lastUsed":0}]"""
        assertEquals(false, decodeConnections(json)[0].favorite)
    }

    @Test
    fun `empty list encodes to empty json array`() {
        assertEquals("[]", encodeConnections(emptyList()))
    }
}
