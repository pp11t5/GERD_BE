package com.gerd.domain.health.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@RestController
class HealthController {

    @GetMapping("/health")
    public fun healthCheck(): String {
        return "OK"
    }
}