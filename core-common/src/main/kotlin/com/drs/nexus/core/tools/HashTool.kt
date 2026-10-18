package com.drs.nexus.core.tools

import java.security.MessageDigest

/** Real cryptographic hashing via java.security.MessageDigest. */
class HashTool {

    enum class Algorithm(val label: String) {
        SHA_256("SHA-256"), SHA_1("SHA-1"), SHA_512("SHA-512"), MD5("MD5")
    }

    fun hash(input: String, algorithm: Algorithm): String {
        val digest = MessageDigest.getInstance(algorithm.label)
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashAll(input: String): Map<Algorithm, String> =
        Algorithm.entries.associateWith { hash(input, it) }

    fun verify(input: String, expectedHex: String, algorithm: Algorithm): Boolean =
        hash(input, algorithm).equals(expectedHex.trim().lowercase(), ignoreCase = true)
}
