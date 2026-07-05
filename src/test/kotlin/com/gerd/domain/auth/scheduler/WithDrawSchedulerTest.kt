package com.gerd.domain.auth.scheduler

import com.gerd.domain.auth.service.WithdrawService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class WithDrawSchedulerTest {

    @Mock
    private lateinit var withdrawService: WithdrawService

    private val scheduler by lazy { WithDrawScheduler(withdrawService) }

    @Test
    fun `빈 배치가 나올 때까지 커서를 전진시키며 배치별로 물리 삭제한다`() {
        whenever(withdrawService.findUsersForHardDelete(0L, 100)).thenReturn(listOf(1L, 2L, 3L))
        whenever(withdrawService.findUsersForHardDelete(3L, 100)).thenReturn(emptyList())

        scheduler.withDraw()

        verify(withdrawService).withdrawHardDelete(1L)
        verify(withdrawService).withdrawHardDelete(2L)
        verify(withdrawService).withdrawHardDelete(3L)
        verify(withdrawService).findUsersForHardDelete(0L, 100)
        verify(withdrawService).findUsersForHardDelete(3L, 100) // 마지막 ID로 커서 전진
    }

    @Test
    fun `첫 배치가 비어 있으면 아무도 삭제하지 않는다`() {
        whenever(withdrawService.findUsersForHardDelete(0L, 100)).thenReturn(emptyList())

        scheduler.withDraw()

        verify(withdrawService, never()).withdrawHardDelete(any())
    }

    @Test
    fun `한 유저 삭제가 실패해도 나머지를 계속 처리하고 커서를 전진시킨다`() {
        whenever(withdrawService.findUsersForHardDelete(0L, 100)).thenReturn(listOf(1L, 2L, 3L))
        whenever(withdrawService.findUsersForHardDelete(3L, 100)).thenReturn(emptyList())
        doThrow(RuntimeException("boom")).whenever(withdrawService).withdrawHardDelete(2L)

        scheduler.withDraw()

        verify(withdrawService).withdrawHardDelete(1L)
        verify(withdrawService).withdrawHardDelete(2L)
        verify(withdrawService).withdrawHardDelete(3L)
        verify(withdrawService).findUsersForHardDelete(3L, 100) // 실패해도 무한 루프 없이 커서 전진
    }
}
