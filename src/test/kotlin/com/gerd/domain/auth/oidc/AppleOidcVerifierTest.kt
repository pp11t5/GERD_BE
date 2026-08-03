package com.gerd.domain.auth.oidc

import com.gerd.domain.auth.dto.JwksResponseDTO
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.config.properties.AppleProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Base64
import java.util.Date

@ExtendWith(MockitoExtension::class)
class AppleOidcVerifierTest {

    @Mock private lateinit var jwksPublicKeyProvider: JwksPublicKeyProvider

    private lateinit var keyPair: KeyPair
    private lateinit var verifier: AppleOidcVerifier
    private val properties = AppleProperties(
        iss = "https://appleid.apple.com",
        jwksUrl = "https://appleid.apple.com/auth/keys",
        clientId = "com.gerd.app",
    )

    @BeforeEach
    fun setUp() {
        keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val publicKey = keyPair.public as RSAPublicKey
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val jwks = JwksResponseDTO(
            keys = listOf(
                JwksResponseDTO.PublicKeyDto(
                    kid = "apple-key-id",
                    kty = "RSA",
                    alg = "RS256",
                    use = "sig",
                    n = encoder.encodeToString(publicKey.modulus.toUnsignedByteArray()),
                    e = encoder.encodeToString(publicKey.publicExponent.toUnsignedByteArray()),
                ),
            ),
        )
        whenever(jwksPublicKeyProvider.getKeys(properties.jwksUrl)).thenReturn(jwks)
        verifier = AppleOidcVerifier(jwksPublicKeyProvider, properties)
    }

    @Nested
    inner class `nonce 검증` {

        @Test
        fun `ID Token nonce가 요청 nonce와 같으면 claims를 반환한다`() {
            val token = createIdToken("expected-nonce")

            val claims = verifier.verify(token, "expected-nonce")

            assertThat(claims.sub).isEqualTo("apple-user-id")
            assertThat(claims.email).isEqualTo("apple@test.com")
        }

        @Test
        fun `ID Token nonce가 요청 nonce와 다르면 INVALID_TOKEN을 던진다`() {
            val token = createIdToken("token-nonce")

            assertThatThrownBy { verifier.verify(token, "request-nonce") }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_TOKEN)
        }
    }

    private fun createIdToken(nonce: String): String {
        val now = Instant.now()
        return Jwts.builder()
            .setHeaderParam("kid", "apple-key-id")
            .setIssuer(properties.iss)
            .setAudience(properties.clientId)
            .setSubject("apple-user-id")
            .claim("email", "apple@test.com")
            .claim("nonce", nonce)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(300)))
            .signWith(keyPair.private, SignatureAlgorithm.RS256)
            .compact()
    }

    private fun java.math.BigInteger.toUnsignedByteArray(): ByteArray {
        val bytes = toByteArray()
        return if (bytes.size > 1 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
    }
}
