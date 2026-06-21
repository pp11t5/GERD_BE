package com.gerd.domain.timeline.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.timeline.dto.TimeLineResponseDTO
import com.gerd.domain.timeline.dto.WeeklyJudgementResponseDTO
import com.gerd.domain.timeline.service.TimeLineService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class TimeLineController(
    private val timeLineService: TimeLineService,
) : TimeLineApi {

    override fun getTimeLine(
        @CurrentUser userDetails: CustomUserDetails,
        date: LocalDate,
    ): ResponseEntity<ApiResponse<TimeLineResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(timeLineService.getTimeLine(userDetails.userId, date), CommonSuccessCode.OK))

    override fun getWeeklyJudgements(
        @CurrentUser userDetails: CustomUserDetails,
        date: LocalDate,
    ): ResponseEntity<ApiResponse<List<WeeklyJudgementResponseDTO>>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(timeLineService.getWeeklyJudgements(userDetails.userId, date), CommonSuccessCode.OK))
}
