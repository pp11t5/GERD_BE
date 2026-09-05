package com.gerd.domain.food.service

import com.gerd.domain.food.dto.FoodSummaryDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TopSearchedFoodCacheTest {

    private val cache = TopSearchedFoodCache()

    @Test
    fun `캐시가 비어있으면 loader 실행 결과를 반환한다`() {
        val expected = listOf(FoodSummaryDTO("ext-1", "된장찌개", "soup_stew"))

        val result = cache.get { expected }

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `캐시에 값이 있으면 loader를 실행하지 않는다`() {
        val loadCount = AtomicInteger(0)
        cache.get {
            loadCount.incrementAndGet()
            listOf(FoodSummaryDTO("ext-1", "된장찌개", "soup_stew"))
        }

        cache.get {
            loadCount.incrementAndGet()
            listOf(FoodSummaryDTO("ext-2", "비빔밥", "rice"))
        }

        assertThat(loadCount.get()).isEqualTo(1)
    }

    @Test
    fun `동시에 캐시 미스가 나도 loader는 한 번만 실행된다`() {
        val loadCount = AtomicInteger(0)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val futures = (1..2).map {
                executor.submit(
                    Callable {
                        ready.countDown()
                        start.await(5, TimeUnit.SECONDS)
                        cache.get {
                            loadCount.incrementAndGet()
                            Thread.sleep(50)
                            listOf(FoodSummaryDTO("ext-1", "된장찌개", "soup_stew"))
                        }
                    },
                )
            }
            ready.await(5, TimeUnit.SECONDS)
            start.countDown()
            val results = futures.map { it.get(5, TimeUnit.SECONDS) }

            assertThat(loadCount.get()).isEqualTo(1)
            assertThat(results).allMatch { it == results[0] }
        } finally {
            executor.shutdown()
        }
    }
}
