package com.gerd

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.scheduling.annotation.EnableScheduling
import java.net.UnknownHostException

@SpringBootApplication(
    exclude = [UserDetailsServiceAutoConfiguration::class],
)
@EnableScheduling
class GerdApplication

fun main(args: Array<String>) {
    val retryAttempts = System.getenv("DATABASE_STARTUP_RETRY_ATTEMPTS")?.toIntOrNull() ?: 60
    val retryDelayMillis = System.getenv("DATABASE_STARTUP_RETRY_DELAY_MILLIS")?.toLongOrNull() ?: 5_000L

    repeat(retryAttempts) { attempt ->
        try {
            runApplication<GerdApplication>(*args)
            return
        } catch (exception: Exception) {
            if (!exception.hasCause<UnknownHostException>() || attempt == retryAttempts - 1) {
                throw exception
            }

            Thread.sleep(retryDelayMillis)
        }
    }
}

private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean =
    generateSequence(this) { it.cause }.any { it is T }
