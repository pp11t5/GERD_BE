package com.gerd.domain.auth.oidc

import com.gerd.domain.auth.dto.JwksResponseDTO
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.config.properties.GoogleProperties
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
class GoogleOidcVerifierTest {

    @Mock private lateinit var jwksPublicKeyProvider: JwksPublicKeyProvider

    private lateinit var keyPair: KeyPair
    private lateinit var verifier: GoogleOidcVerifier
    private val properties = GoogleProperties(
        iss = "https://accounts.google.com",
        jwksUrl = "https://www.googleapis.com/oauth2/v3/certs",
        webClientId = "web-client-id",
        androidClientId = "android-client-id",
        iosClientId = "ios-client-id",
    )

    @BeforeEach
    fun setUp() {
        keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val publicKey = keyPair.public as RSAPublicKey
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val jwks = JwksResponseDTO(
            keys = listOf(
                JwksResponseDTO.PublicKeyDto(
                    kid = "google-key-id",
                    kty = "RSA",
                    alg = "RS256",
                    use = "sig",
                    n = encoder.encodeToString(publicKey.modulus.toUnsignedByteArray()),
                    e = encoder.encodeToString(publicKey.publicExponent.toUnsignedByteArray()),
                ),
            ),
        )
        whenever(jwksPublicKeyProvider.getKeys(properties.jwksUrl)).thenReturn(jwks)
        verifier = GoogleOidcVerifier(jwksPublicKeyProvider, properties)
    }

    @Nested
    inner class `플랫폼별 audience 허용` {

        @Test
        fun `Web Client ID를 audience로 발급된 토큰을 검증한다`() {
            val token = createIdToken(properties.webClientId)

            val claims = verifier.verify(token, nonce = null)

            assertThat(claims.sub).isEqualTo("google-user-id")
            assertThat(claims.email).isEqualTo("google@test.com")
        }

        @Test
        fun `Android Client ID를 audience로 발급된 토큰을 검증한다`() {
            val token = createIdToken(properties.androidClientId)

            val claims = verifier.verify(token, nonce = null)

            assertThat(claims.sub).isEqualTo("google-user-id")
            assertThat(claims.email).isEqualTo("google@test.com")
        }

        @Test
        fun `iOS Client ID를 audience로 발급된 토큰도 검증한다`() {
            val token = createIdToken(properties.iosClientId)

            val claims = verifier.verify(token, nonce = null)

            assertThat(claims.sub).isEqualTo("google-user-id")
        }

        @Test
        fun `등록되지 않은 audience면 INVALID_TOKEN을 던진다`() {
            val token = createIdToken(audience = "unregistered-client-id")

            assertThatThrownBy { verifier.verify(token, nonce = null) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_TOKEN)
        }
    }

    @Nested
    inner class `레거시 issuer 허용` {

        @Test
        fun `https 스킴 issuer면 검증한다`() {
            val token = createIdToken(issuer = "https://accounts.google.com")

            val claims = verifier.verify(token, nonce = null)

            assertThat(claims.sub).isEqualTo("google-user-id")
        }

        @Test
        fun `스킴 없는 레거시 issuer도 검증한다`() {
            val token = createIdToken(issuer = "accounts.google.com")

            val claims = verifier.verify(token, nonce = null)

            assertThat(claims.sub).isEqualTo("google-user-id")
        }

        @Test
        fun `등록되지 않은 issuer면 INVALID_TOKEN을 던진다`() {
            val token = createIdToken(issuer = "https://evil.example.com")

            assertThatThrownBy { verifier.verify(token, nonce = null) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_TOKEN)
        }
    }

    private fun createIdToken(
        audience: String = properties.androidClientId,
        issuer: String = properties.iss,
    ): String {
        val now = Instant.now()
        return Jwts.builder()
            .setHeaderParam("kid", "google-key-id")
            .setIssuer(issuer)
            .setAudience(audience)
            .setSubject("google-user-id")
            .claim("email", "google@test.com")
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
