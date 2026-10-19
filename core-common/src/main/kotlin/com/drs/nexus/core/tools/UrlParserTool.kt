package com.drs.nexus.core.tools

import java.net.URI
import java.net.URLDecoder

/**
 * Offline URL parser built on [URI]: breaks a URL into scheme, authority,
 * host, port, path segments, query parameters and fragment — all on-device.
 */
class UrlParserTool {

    data class QueryParam(val key: String, val value: String)

    data class ParsedUrl(
        val input: String,
        val scheme: String,
        val host: String,
        val port: Int,
        val isDefaultPort: Boolean,
        val authority: String,
        val path: String,
        val pathSegments: List<String>,
        val query: String,
        val params: List<QueryParam>,
        val fragment: String,
        val userInfo: String
    )

    sealed class Result {
        data class Success(val parsed: ParsedUrl) : Result()
        data class Failure(val message: String) : Result()
    }

    fun parse(raw: String): Result {
        val input = raw.trim()
        if (input.isEmpty()) return Result.Failure("URL is empty")
        if (input.contains(" ")) return Result.Failure("URL contains spaces")
        val uri = try {
            URI(input)
        } catch (e: Exception) {
            return Result.Failure("Malformed URL: ${e.message ?: "parse error"}")
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme.isNullOrBlank()) return Result.Failure("Missing scheme (http/https/...)")
        if (uri.host.isNullOrBlank() && uri.authority != null) {
            return Result.Failure("Missing host")
        }
        val port = uri.port // -1 when absent
        val effectivePort = if (port == -1) defaultPort(scheme) else port
        val query = uri.rawQuery ?: ""
        val params = if (query.isEmpty()) emptyList() else parseParams(query)
        val path = uri.rawPath ?: ""
        return Result.Success(
            ParsedUrl(
                input = input,
                scheme = scheme,
                host = uri.host ?: "",
                port = effectivePort,
                isDefaultPort = port == -1 || port == defaultPort(scheme),
                authority = uri.authority ?: "",
                path = path,
                pathSegments = path.split('/').filter { it.isNotEmpty() },
                query = query,
                params = params,
                fragment = uri.rawFragment ?: "",
                userInfo = uri.rawUserInfo ?: ""
            )
        )
    }

    private fun parseParams(query: String): List<QueryParam> =
        query.split('&')
            .filter { it.isNotEmpty() }
            .map { pair ->
                val idx = pair.indexOf('=')
                if (idx == -1) {
                    QueryParam(decode(pair), "")
                } else {
                    QueryParam(decode(pair.take(idx)), decode(pair.substring(idx + 1)))
                }
            }

    private fun decode(s: String): String = try {
        URLDecoder.decode(s.replace("+", "%20"), Charsets.UTF_8)
    } catch (e: Exception) {
        s
    }

    private fun defaultPort(scheme: String): Int = when (scheme) {
        "http" -> 80
        "https" -> 443
        "ftp" -> 21
        else -> -1
    }

    /** Rebuilds the URL from its parts — used to show a normalized form. */
    fun normalize(p: ParsedUrl): String {
        val sb = StringBuilder()
        sb.append(p.scheme).append("://")
        if (p.userInfo.isNotEmpty()) sb.append(p.userInfo).append('@')
        sb.append(p.host)
        if (!p.isDefaultPort && p.port != -1) sb.append(':').append(p.port)
        if (p.path.isNotEmpty()) sb.append(p.path)
        if (p.query.isNotEmpty()) sb.append('?').append(p.query)
        if (p.fragment.isNotEmpty()) sb.append('#').append(p.fragment)
        return sb.toString()
    }

    fun formatResult(p: ParsedUrl): String = "${p.scheme}://${p.host}${if (p.port != -1 && !p.isDefaultPort) ":${p.port}" else ""}${p.path}"
}
