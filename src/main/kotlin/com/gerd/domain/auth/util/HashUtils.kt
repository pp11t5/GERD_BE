package com.gerd.domain.auth.util

import java.security.MessageDigest
import java.util.HexFormat

object HashUtils {

    // 토큰 원문 대신 해시를 저장해 DB/Redis 유출 시 원문 노출 방지
    fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest(value.toByteArray(Charsets.UTF_8))
        return HexFormat.of().formatHex(hashed)
    }
}
