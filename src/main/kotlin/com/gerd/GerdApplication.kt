package com.gerd

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GerdApplication

fun main(args: Array<String>) {
    runApplication<GerdApplication>(*args)
}
