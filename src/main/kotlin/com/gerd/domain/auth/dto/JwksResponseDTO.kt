package com.gerd.domain.auth.dto

data class JwksResponseDTO(
    val keys: List<PublicKeyDto>,
) {
    data class PublicKeyDto(
        val kid: String,
        val kty: String,
        val alg: String,
        val use: String,
        val n: String,
        val e: String,
    )
}
