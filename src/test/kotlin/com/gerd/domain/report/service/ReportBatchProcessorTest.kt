package com.gerd.domain.report.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.notification.service.NotificationFacade
import com.gerd.global.config.properties.WeeklyReportScheduleProperties
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class ReportBatchProcessorTest {

    @Mock
    private lateinit var reportService: ReportService

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var notificationFacade: NotificationFacade

    private val processor by lazy {
        ReportBatchProcessor(
            reportService = reportService,
            userRepository = userRepository,
            notificationFacade = notificationFacade,
            weeklyReportScheduleProperties = WeeklyReportScheduleProperties(),
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

            inOrder(userRepository, reportService, notificationFacade) {
                verify(userRepository).findIdsAfter(0L, pageable)
                verify(reportService).getOrCreate(1L)
                verify(reportService).getOrCreate(2L)
                verify(userRepository).findIdsAfter(2L, pageable)
                verify(reportService).getOrCreate(4L)
                verify(userRepository).findIdsAfter(4L, pageable)
                // 전체 리포트 생성이 끝난 뒤에만 발송 — 생성 전 알림 선발송 경합 방지
                verify(notificationFacade).sendWeeklyReport()
            }
        }

        @Test
        fun `개별 유저 리포트 생성 실패 후에도 다음 userId 처리를 계속한다`() {
            val pageable = PageRequest.of(0, 200)
            whenever(userRepository.findIdsAfter(0L, pageable)).thenReturn(listOf(1L, 2L, 3L))
            whenever(userRepository.findIdsAfter(3L, pageable)).thenReturn(emptyList())
            whenever(reportService.getOrCreate(2L)).thenThrow(RuntimeException("failed"))

            processor.createAllReports()

            inOrder(userRepository, reportService, notificationFacade) {
                verify(reportService).getOrCreate(1L)
                verify(reportService).getOrCreate(2L)
                verify(reportService).getOrCreate(3L)
                verify(userRepository).findIdsAfter(eq(3L), any())
                // 실패가 섞여도 마지막 페이지 조회 이후에만 발송돼야 한다
                verify(notificationFacade).sendWeeklyReport()
            }
        }

        @Test
        fun `별도 알림 cron이 설정되면 생성 완료 후 즉시 알림을 보내지 않는다`() {
            whenever(userRepository.findIdsAfter(0L, PageRequest.of(0, 200))).thenReturn(emptyList())
            val scheduledProcessor = ReportBatchProcessor(
                reportService = reportService,
                userRepository = userRepository,
                notificationFacade = notificationFacade,
                weeklyReportScheduleProperties = WeeklyReportScheduleProperties(
                    notificationCron = "0 10 10 * * MON",
                ),
            )

            scheduledProcessor.createAllReports()

            verify(notificationFacade, never()).sendWeeklyReport()
        }
    }
}
