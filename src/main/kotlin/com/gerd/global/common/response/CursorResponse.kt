package com.gerd.global.common.response

/**
 * 커서 페이징 응답 공통 DTO
 */
data class CursorResponse<T, C>(
    val items: List<T>,
    val nextCursor: C? = null,
    val hasNext: Boolean = nextCursor != null,
) {
    companion object {
        fun <T, C> from(
            items: List<T>,
            pageSize: Int,
            cursorExtractor: (T) -> C,
        ): CursorResponse<T, C> {
            val hasNext = items.size > pageSize
            val pagedItems = if (hasNext) items.take(pageSize) else items
            val nextCursor = if (hasNext) cursorExtractor(pagedItems.last()) else null
            return CursorResponse(pagedItems, nextCursor, hasNext)
        }
    }
}