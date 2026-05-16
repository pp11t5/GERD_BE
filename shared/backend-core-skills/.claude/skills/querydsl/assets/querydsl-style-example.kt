private val domainEntity = QDomainEntity.domainEntity
private val domainAttachment = QDomainAttachment.domainAttachment

private fun emotionCondition(
    emotionPath: EnumPath<Emotion>,
    emotion: Emotion?,
): BooleanExpression? {
    if (emotion == null) {
        return null
    }
    return emotionPath.eq(emotion)
}

private fun sortDateExpression(sortType: SortType): DateTimeExpression<LocalDateTime> =
    when (sortType) {
        SortType.RECENT,
        SortType.COUNT_ASC,
        SortType.COUNT_DESC,
        -> domainEntity.createdDate.max()

        SortType.OLDEST -> domainEntity.createdDate.min()
    }

private fun createdDateCursorCondition(
    cursorCreatedDate: LocalDateTime?,
    cursorId: Long?,
    sortDate: DateTimeExpression<LocalDateTime>,
    tieBreakerId: NumberExpression<Long>,
    ascending: Boolean,
): BooleanExpression? {
    if (cursorCreatedDate == null) {
        return null
    }

    val byDate = if (ascending) {
        sortDate.gt(cursorCreatedDate)
    } else {
        sortDate.lt(cursorCreatedDate)
    }

    if (cursorId == null) {
        return byDate
    }

    val tieBreaker = if (ascending) {
        tieBreakerId.gt(cursorId)
    } else {
        tieBreakerId.lt(cursorId)
    }

    return byDate.or(sortDate.eq(cursorCreatedDate).and(tieBreaker))
}

private fun countCursorCondition(
    cursorCount: Long?,
    cursorId: Long?,
    countExpression: NumberExpression<Long>,
    tieBreakerId: NumberExpression<Long>,
    ascending: Boolean,
): BooleanExpression? {
    if (cursorCount == null) {
        return null
    }

    val byCount = if (ascending) {
        countExpression.gt(cursorCount)
    } else {
        countExpression.lt(cursorCount)
    }

    if (cursorId == null) {
        return byCount
    }

    val tieBreaker = if (ascending) {
        tieBreakerId.gt(cursorId)
    } else {
        tieBreakerId.lt(cursorId)
    }

    return byCount.or(countExpression.eq(cursorCount).and(tieBreaker))
}

private fun orderByConditions(
    sortType: SortType,
    countExpression: NumberExpression<Long>,
    tieBreakerId: NumberExpression<Long>,
    sortDate: DateTimeExpression<LocalDateTime>,
): Array<OrderSpecifier<*>> =
    when (sortType) {
        SortType.RECENT -> arrayOf(sortDate.desc(), tieBreakerId.desc())
        SortType.OLDEST -> arrayOf(sortDate.asc(), tieBreakerId.asc())
        SortType.COUNT_DESC -> arrayOf(countExpression.desc(), tieBreakerId.desc())
        SortType.COUNT_ASC -> arrayOf(countExpression.asc(), tieBreakerId.asc())
    }

fun findByCursor(
    userId: Long,
    cursor: Cursor,
    sortType: SortType,
    size: Int,
): List<DomainItemDto> {
    val domainCount = domainEntity.count()
    val sortDate = sortDateExpression(sortType)

    return queryFactory
        .select(
            Projections.constructor(
                DomainItemDto::class.java,
                domainEntity.id,
                domainEntity.content,
                domainCount,
                sortDate,
            ),
        )
        .from(domainEntity)
        .where(domainEntity.user.id.eq(userId))
        .groupBy(domainEntity.id, domainEntity.content)
        .having(
            when (sortType) {
                SortType.RECENT -> createdDateCursorCondition(
                    cursor.lastCreatedDate(),
                    cursor.lastId(),
                    sortDate,
                    domainEntity.id,
                    false,
                )
                SortType.OLDEST -> createdDateCursorCondition(
                    cursor.lastCreatedDate(),
                    cursor.lastId(),
                    sortDate,
                    domainEntity.id,
                    true,
                )
                SortType.COUNT_DESC -> countCursorCondition(
                    cursor.lastCount(),
                    cursor.lastId(),
                    domainCount,
                    domainEntity.id,
                    false,
                )
                SortType.COUNT_ASC -> countCursorCondition(
                    cursor.lastCount(),
                    cursor.lastId(),
                    domainCount,
                    domainEntity.id,
                    true,
                )
            },
        )
        .orderBy(*orderByConditions(sortType, domainCount, domainEntity.id, sortDate))
        .limit(size + 1L)
        .fetch()
}
