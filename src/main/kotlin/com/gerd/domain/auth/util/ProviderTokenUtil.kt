package com.gerd.domain.auth.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class ProviderTokenUtil(
    @Value("\${auth.provider-token-encryption-key:}") private val encodedKey: String,
) {
    private val secureRandom = SecureRandom()

    private val secretKey: SecretKeySpec by lazy {
        val keyBytes = runCatching { Base64.getDecoder().decode(encodedKey) }
            .getOrElse { throw IllegalStateException("Provider token encryption key must be Base64 encoded") }

        require(keyBytes.size == KEY_SIZE_BYTES) {
            "Provider token encryption key must be 32 bytes"
        }

        SecretKeySpec(keyBytes, "AES")
    }

    fun validateConfiguration() {
        secretKey
    }

    fun encrypt(plainText: String): String {
        val iv = ByteArray(IV_SIZE_BYTES).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE_BITS, iv))
        }
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(iv + encrypted)
    }

    fun decrypt(encryptedText: String): String {
        val payload = Base64.getUrlDecoder().decode(encryptedText)
        require(payload.size > IV_SIZE_BYTES) { "Invalid encrypted provider token" }

        val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
        val encrypted = payload.copyOfRange(IV_SIZE_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE_BITS, iv))
        }

        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BYTES = 32
        private const val IV_SIZE_BYTES = 12
        private const val TAG_SIZE_BITS = 128
    }
}
