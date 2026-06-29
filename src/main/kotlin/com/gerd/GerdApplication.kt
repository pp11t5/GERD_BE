package com.gerd

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class GerdApplication

fun main(args: Array<String>) {
    runApplication<GerdApplication>(*args)
}
