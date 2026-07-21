package com.gerd.domain.auth.service

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.auth.util.NicknameGenerator
import com.gerd.global.apiPayload.GeneralException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NicknameService(
    private val userRepository: UserRepository,
) {
    fun generateUniqueNickname(): String {
        val base = NicknameGenerator.generate()
        return resolveUniqueNickname(base)
    }

    // 마이페이지에서 사용자가 직접 입력한 닉네임으로 변경
    // exists 체크는 대부분의 중복을 걸러내는 선제 검증일 뿐 — 동시 요청 레이스는 DB unique 제약(saveAndFlush)이 최종 방어선
    @Transactional
    fun changeNickname(userId: Long, newNickname: String) {
        if (userRepository.existsByNicknameIncludingDeleted(newNickname, userId)) {
            throw GeneralException(AuthErrorCode.NICKNAME_ALREADY_IN_USE)
        }
        val user = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
        user.changeNickname(newNickname)
        runCatching { userRepository.saveAndFlush(user) }.getOrElse { e ->
            if (e !is DataIntegrityViolationException) throw e
            throw GeneralException(AuthErrorCode.NICKNAME_ALREADY_IN_USE)
        }
    }

    // 탈퇴 유예 중인 유저의 닉네임도 점유로 취급
    private fun resolveUniqueNickname(base: String): String {
        val taken = userRepository.findNicknamesByBaseIncludingDeleted(base)
        if (taken.isEmpty()) return base

        val suffixPattern = Regex("^${Regex.escape(base)}(\\d+)$")
        val maxSuffix = taken
            .mapNotNull { suffixPattern.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull() ?: 0

        return "$base${maxSuffix + 1}"
    }
}
