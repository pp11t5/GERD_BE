package com.gerd.domain.auth.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Base64

class ProviderTokenUtilTest {

    @Test
    fun `암호화한 provider token을 다시 복호화한다`() {
        val encodedKey = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
        val providerTokenUtil = ProviderTokenUtil(encodedKey)

        val encrypted = providerTokenUtil.encrypt("provider-refresh-token")

        assertThat(encrypted).isNotEqualTo("provider-refresh-token")
        assertThat(providerTokenUtil.decrypt(encrypted)).isEqualTo("provider-refresh-token")
    }
}
