package com.drs.nexus.core.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Real JWT decoder: splits the token, decodes the base64url header/payload,
 * pretty-prints the claims JSON and evaluates the `exp` claim against a reference time.
 * Decoding only — no signature verification (requires the key, kept out of scope deliberately).
 */
class JwtDecoderTool {

    data class Section(val raw: String, val prettyJson: String)

    data class Claims(
        val subject: String?,
        val issuer: String?,
        val audience: String?,
        val issuedAtEpoch: Long?,
        val expiresAtEpoch: Long?
    )

    enum class ExpiryStatus { VALID, EXPIRED, NO_EXPIRY }

    data class DecodeResult(
        val header: Section,
        val payload: Section,
        val signatureRaw: String,
        val algorithm: String?,
        val claims: Claims,
        val status: ExpiryStatus,
        val secondsLeft: Long?
    )

    sealed class JwtResult {
        data class Success(val result: DecodeResult) : JwtResult()
        data class Failure(val message: String) : JwtResult()
    }

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun decode(token: String, nowEpochSeconds: Long = System.currentTimeMillis() / 1000): JwtResult {
        val parts = token.trim().split(".")
        if (parts.size != 3) {
            return JwtResult.Failure("A JWT must have 3 dot-separated sections, found ${parts.size}")
        }
        val (headRaw, payloadRaw, sigRaw) = parts
        if (headRaw.isBlank() || payloadRaw.isBlank()) {
            return JwtResult.Failure("Header or payload section is empty")
        }
        val headerJson = decodeSection(headRaw)
            ?: return JwtResult.Failure("Header is not valid base64url JSON")
        val payloadJson = decodeSection(payloadRaw)
            ?: return JwtResult.Failure("Payload is not valid base64url JSON")

        val headerObj = runCatching { json.parseToJsonElement(headerJson).jsonObjectSafe() }.getOrNull()
            ?: return JwtResult.Failure("Header is not a JSON object")
        val payloadObj = runCatching { json.parseToJsonElement(payloadJson).jsonObjectSafe() }.getOrNull()
            ?: return JwtResult.Failure("Payload is not a JSON object")

        val algorithm = headerObj["alg"]?.toString()?.trim('"')
        val subject = payloadObj["sub"]?.toString()?.trim('"')
        val issuer = payloadObj["iss"]?.toString()?.trim('"')
        val audience = payloadObj["aud"]?.toString()?.trim('"')?.removeSurrounding("[", "]")
        val issuedAt = (payloadObj["iat"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull()
        val expiresAt = (payloadObj["exp"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull()

        val status = when {
            expiresAt == null -> ExpiryStatus.NO_EXPIRY
            expiresAt > nowEpochSeconds -> ExpiryStatus.VALID
            else -> ExpiryStatus.EXPIRED
        }

        return JwtResult.Success(
            DecodeResult(
                header = Section(headRaw, prettyJson(headerJson)),
                payload = Section(payloadRaw, prettyJson(payloadJson)),
                signatureRaw = sigRaw,
                algorithm = algorithm,
                claims = Claims(subject, issuer, audience, issuedAt, expiresAt),
                status = status,
                secondsLeft = expiresAt?.let { it - nowEpochSeconds }
            )
        )
    }

    private fun decodeSection(section: String): String? {
        val normalized = section.replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return try {
            val bytes = java.util.Base64.getDecoder().decode(padded)
            String(bytes, Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun prettyJson(raw: String): String =
        runCatching { json.parseToJsonElement(raw).toString() }.getOrElse { raw }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectSafe(): JsonObject? =
        this as? JsonObject
}
