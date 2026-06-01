package com.gerd.domain.food.converter

import com.gerd.domain.food.entity.enums.FoodVisibility
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class FoodVisibilityConverter : AttributeConverter<FoodVisibility, String> {

    override fun convertToDatabaseColumn(attribute: FoodVisibility): String = attribute.code

    override fun convertToEntityAttribute(dbData: String): FoodVisibility = FoodVisibility.from(dbData)
}
