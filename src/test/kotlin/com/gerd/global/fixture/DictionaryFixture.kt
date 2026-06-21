package com.gerd.global.fixture

import com.gerd.domain.auth.entity.User
import com.gerd.domain.dictionary.entity.UserFoodDictionary
import com.gerd.domain.dictionary.entity.enums.DictionaryType
import com.gerd.domain.food.entity.Food
import org.springframework.test.util.ReflectionTestUtils

object DictionaryFixture {

    fun entry(
        id: Long = 1L,
        user: User = UserFixture.user(),
        food: Food = FoodFixture.food(),
        type: DictionaryType = DictionaryType.SAFE,
    ): UserFoodDictionary = UserFoodDictionary(
        user = user,
        food = food,
        dictionaryType = type,
    ).apply {
        ReflectionTestUtils.setField(this, "id", id)
    }
}
