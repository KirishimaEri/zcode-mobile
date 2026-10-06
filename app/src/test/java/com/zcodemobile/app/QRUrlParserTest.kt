package com.zcodemobile.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QRUrlParserTest {

    @Test
    fun `parses valid v4 url`() {
        val raw = "https://zcode.z.ai/remote/v4?sid=d_MJyaSAAAAAAAAoXpD&hash=abc123%2F%3D%3D&t=1759600000000" +
            "&mid=uuid-1234&name=My-PC&app_version=3.14.0"
        val result = QRUrlParser.parse(raw, strict = true)
        assertTrue(result is ParseResult.Ok)
        val conn = (result as ParseResult.Ok).connection
        assertEquals("zcode.z.ai", conn.host)
        assertEquals("My-PC", conn.deviceName)
        assertEquals(raw, conn.url)
    }

    @Test
    fun `parses v3 url`() {
        val raw = "https://zcode.z.ai/remote/v3?sid=abc&hash=xyz&t=1"
        assertTrue(QRUrlParser.parse(raw, strict = true) is ParseResult.Ok)
    }

    @Test
    fun `decodes url-encoded chinese device name`() {
        val raw = "https://zcode.z.ai/remote/v4?sid=a&hash=b&t=1&name=" +
            java.net.URLEncoder.encode("实验室工作站", "UTF-8")
        val result = QRUrlParser.parse(raw, strict = true)
        assertTrue(result is ParseResult.Ok)
        assertEquals("实验室工作站", (result as ParseResult.Ok).connection.deviceName)
    }

    @Test
    fun `rejects foreign host when strict`() {
        val raw = "https://evil.example.com/remote/v4?sid=a&hash=b&t=1"
        val result = QRUrlParser.parse(raw, strict = true)
        assertTrue(result is ParseResult.Invalid)
        assertTrue((result as ParseResult.Invalid).reason.contains("域名"))
    }

    @Test
    fun `rejects wrong path when strict`() {
        val raw = "https://zcode.z.ai/other?sid=a&hash=b"
        assertTrue(QRUrlParser.parse(raw, strict = true) is ParseResult.Invalid)
    }

    @Test
    fun `rejects missing pairing params when strict`() {
        val raw = "https://zcode.z.ai/remote/v4?mid=x&name=pc"
        assertTrue(QRUrlParser.parse(raw, strict = true) is ParseResult.Invalid)
    }

    @Test
    fun `rejects non-http scheme`() {
        assertTrue(QRUrlParser.parse("ftp://example.com/file", strict = false) is ParseResult.Invalid)
        assertTrue(QRUrlParser.parse("hello world", strict = false) is ParseResult.Invalid)
    }

    @Test
    fun `lenient mode allows any web host`() {
        val raw = "https://my-relay.example.com/remote/v4?sid=a&hash=b"
        assertTrue(QRUrlParser.parse(raw, strict = false) is ParseResult.Ok)
    }

    @Test
    fun `empty input is invalid`() {
        assertTrue(QRUrlParser.parse("   ", strict = true) is ParseResult.Invalid)
    }
}
