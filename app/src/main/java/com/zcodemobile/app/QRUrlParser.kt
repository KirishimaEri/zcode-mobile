package com.zcodemobile.app

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

// 桌面端远程控制二维码内容：
// https://zcode.z.ai/remote/v4?sid=<deviceSid>&hash=<口令哈希>&t=<时间戳>&mid=<机器ID>&name=<主机名>&app_version=<版本>
data class ZcodeConnection(
    val url: String,
    val host: String,
    val deviceName: String?,
)

sealed interface ParseResult {
    data class Ok(val connection: ZcodeConnection) : ParseResult
    data class Invalid(val reason: String) : ParseResult
}

object QRUrlParser {

    val KNOWN_HOSTS = setOf("zcode.z.ai", "zcode.chatglm.site")

    // strict=true 用于扫码识别，false 用于手动粘贴
    fun parse(raw: String, strict: Boolean): ParseResult {
        // 剪贴板内容常混入换行等空白字符
        val text = raw.replace(Regex("\\s+"), "")
        if (text.isEmpty()) return ParseResult.Invalid("内容为空")
        if (strict && text.length > 4096) return ParseResult.Invalid("内容过长，不是二维码链接")

        val uri = try {
            URI(asciiEncoded(text))
        } catch (_: Exception) {
            return ParseResult.Invalid("无法识别的链接")
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "https" && scheme != "http") return ParseResult.Invalid("不是网页链接")

        val host = uri.host?.lowercase() ?: return ParseResult.Invalid("链接缺少主机名")
        if (strict && host !in KNOWN_HOSTS) {
            return ParseResult.Invalid("主机 $host 不在 ZCode 官方域名内")
        }

        val path = uri.rawPath ?: ""
        if (strict && !path.startsWith("/remote/v")) {
            return ParseResult.Invalid("不是 ZCode 远程控制地址")
        }

        val params = parseQuery(uri.rawQuery)
        if (strict && (params["sid"].isNullOrBlank() || params["hash"].isNullOrBlank())) {
            return ParseResult.Invalid("链接缺少配对参数（sid/hash），请在桌面端重新生成二维码")
        }

        val name = params["name"]?.takeIf { it.isNotBlank() }
        return ParseResult.Ok(ZcodeConnection(text, host, name))
    }

    fun displayName(connection: ZcodeConnection): String =
        connection.deviceName ?: connection.host

    // 二维码由 JS URL 生成已百分号编码，这里对非 ASCII 兜底再编一次
    private fun asciiEncoded(text: String): String = buildString {
        for (ch in text) {
            if (ch.code in 32..126) append(ch) else append(URLEncoder.encode(ch.toString(), Charsets.UTF_8))
        }
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split('&').mapNotNull { pair ->
            val i = pair.indexOf('=')
            if (i <= 0) return@mapNotNull null
            val key = runCatching { URLDecoder.decode(pair.substring(0, i), Charsets.UTF_8) }.getOrNull() ?: return@mapNotNull null
            val value = runCatching { URLDecoder.decode(pair.substring(i + 1), Charsets.UTF_8) }.getOrNull() ?: return@mapNotNull null
            key to value
        }.toMap()
    }
}
