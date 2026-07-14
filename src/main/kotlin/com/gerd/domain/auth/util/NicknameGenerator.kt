package com.gerd.domain.auth.util

object NicknameGenerator {

    fun generate(): String {
        val adjective = NicknameWords.ADJECTIVES.random()
        val noun = NicknameWords.NOUNS.random()
        return "$adjective $noun"
    }
}
