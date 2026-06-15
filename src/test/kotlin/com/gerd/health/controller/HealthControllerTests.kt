package com.gerd.health.controller

import com.gerd.domain.health.controller.HealthController
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test


@ActiveProfiles("test")
@SpringBootTest
class HealthControllerTests {

    @Autowired
    lateinit var healthController: HealthController

    @Test fun healthCheck() {
        assertThat(healthController.healthCheck()).isEqualTo("OK")
    }
}