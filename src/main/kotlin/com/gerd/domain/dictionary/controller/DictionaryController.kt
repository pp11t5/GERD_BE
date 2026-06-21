package com.gerd.domain.dictionary.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.dictionary.dto.DictionaryCountResponseDTO
import com.gerd.domain.dictionary.dto.DictionaryCautionRiskResponseDTO
import com.gerd.domain.dictionary.dto.DictionarySafeResponseDTO
import com.gerd.domain.dictionary.service.DictionaryQueryService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class DictionaryController(
    private val dictionaryService: DictionaryQueryService,
) : DictionaryApi {

    override fun getCount(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<DictionaryCountResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(dictionaryService.getCount(userDetails.userId), CommonSuccessCode.OK))

    override fun getSafeFoods(
        @CurrentUser userDetails: CustomUserDetails,
        cursor: Long?,
        size: Int?,
    ): ResponseEntity<ApiResponse<DictionarySafeResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(
                ApiResponse.onSuccess(
                    dictionaryService.getSafeFoods(size, cursor, userDetails.userId),
                    CommonSuccessCode.OK,
                ),
            )

    override fun getCautionRiskFoods(
        @CurrentUser userDetails: CustomUserDetails,
        cursor: Long?,
        size: Int?,
    ): ResponseEntity<ApiResponse<DictionaryCautionRiskResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(
                ApiResponse.onSuccess(
                    dictionaryService.getCautionRiskFoods(size, cursor, userDetails.userId),
                    CommonSuccessCode.OK,
                ),
            )
}
