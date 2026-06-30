package com.gerd

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    exclude = [UserDetailsServiceAutoConfiguration::class],
)
@EnableScheduling
class GerdApplication

fun main(args: Array<String>) {
    runApplication<GerdApplication>(*args)
}
