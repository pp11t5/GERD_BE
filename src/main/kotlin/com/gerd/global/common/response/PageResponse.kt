package com.gerd.global.common.response

import org.springframework.data.domain.Page

/** offset 기반 페이지 응답 
 * Spring Data Page<T>를 ApiResponse.result로 내릴 때 사용 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val isFirst: Boolean,
    val isLast: Boolean,
) {
    companion object {

        fun <T : Any> of(page: Page<T>): PageResponse<T> =
            PageResponse(
                content = page.content,
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                hasNext = page.hasNext(),
                isFirst = page.isFirst,
                isLast = page.isLast,
            )

        // Page<T> 요소를 R로 변환해서 반환 — 엔티티 → DTO 변환이 필요한 경우 사용
        fun <T : Any, R> of(page: Page<T>, converter: (T) -> R): PageResponse<R> =
            PageResponse(
                content = page.content.map(converter),
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                hasNext = page.hasNext(),
                isFirst = page.isFirst,
                isLast = page.isLast,
            )
    }
}