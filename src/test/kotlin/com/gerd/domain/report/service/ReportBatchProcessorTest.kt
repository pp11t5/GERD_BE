package com.gerd.domain.report.service

import com.gerd.domain.auth.repository.UserRepository
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class ReportBatchProcessorTest {

    @Mock
    private lateinit var reportService: ReportService

    @Mock
    private lateinit var userRepository: UserRepository

    private val processor by lazy {
        ReportBatchProcessor(
            reportService = reportService,
            userRepository = userRepository,
        )
    }

    @Nested
    inner class `전체 리포트 생성` {

        @Test
        fun `마지막 userId 기준으로 다음 배치를 조회한다`() {
            val pageable = PageRequest.of(0, 200)
            whenever(userRepository.findIdsAfter(0L, pageable)).thenReturn(listOf(1L, 2L))
            whenever(userRepository.findIdsAfter(2L, pageable)).thenReturn(listOf(4L))
            whenever(userRepository.findIdsAfter(4L, pageable)).thenReturn(emptyList())

            processor.createAllReports()

            inOrder(userRepository, reportService) {
                verify(userRepository).findIdsAfter(0L, pageable)
                verify(reportService).getOrCreate(1L)
                verify(reportService).getOrCreate(2L)
                verify(userRepository).findIdsAfter(2L, pageable)
                verify(reportService).getOrCreate(4L)
                verify(userRepository).findIdsAfter(4L, pageable)
            }
        }

        @Test
        fun `개별 유저 리포트 생성 실패 후에도 다음 userId 처리를 계속한다`() {
            val pageable = PageRequest.of(0, 200)
            whenever(userRepository.findIdsAfter(0L, pageable)).thenReturn(listOf(1L, 2L, 3L))
            whenever(userRepository.findIdsAfter(3L, pageable)).thenReturn(emptyList())
            whenever(reportService.getOrCreate(2L)).thenThrow(RuntimeException("failed"))

            processor.createAllReports()

            verify(reportService).getOrCreate(1L)
            verify(reportService).getOrCreate(2L)
            verify(reportService).getOrCreate(3L)
            verify(userRepository).findIdsAfter(eq(3L), any())
        }
    }
}
