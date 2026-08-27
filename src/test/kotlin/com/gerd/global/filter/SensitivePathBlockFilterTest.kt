package com.gerd.global.filter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class SensitivePathBlockFilterTest {

    private val filter = SensitivePathBlockFilter()

    @ParameterizedTest
    @ValueSource(strings = ["/.env", "/.git/config", "/.aws/credentials", "/api/.env", "/wp-admin/install.php", "/phpinfo.php"])
    fun `민감 파일 탐색 경로는 찾을 수 없도록 차단한다`(uri: String) {
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(MockHttpServletRequest("GET", uri), response, chain)

        assertThat(response.status).isEqualTo(404)
        assertThat(chain.request).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["/api/v1/dictionary/count", "/actuator/health", "/"])
    fun `정상 서비스 경로는 통과한다`(uri: String) {
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(MockHttpServletRequest("GET", uri), response, chain)

        assertThat(chain.request).isNotNull()
    }
}
