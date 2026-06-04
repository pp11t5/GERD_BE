package com.gerd.global.config

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class QuerydslTestConfig(
    @PersistenceContext private val entityManager: EntityManager,
) {

    // @DataJpaTest 슬라이스는 QuerydslConfig를 로드하지 않아 JPAQueryFactory 빈이 없다
    // QueryDSL fragment를 가진 리포지토리가 슬라이스에서 생성되도록 @Import(QuerydslTestConfig::class)로 동일 빈을 등록
    @Bean
    fun jpaQueryFactory(): JPAQueryFactory = JPAQueryFactory(entityManager)
}
