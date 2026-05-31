package com.gerd.domain.food.converter

import com.gerd.domain.food.entity.enums.FoodSource
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class FoodSourceConverter : AttributeConverter<FoodSource, String> {

    override fun convertToDatabaseColumn(attribute: FoodSource): String = attribute.code

    override fun convertToEntityAttribute(dbData: String): FoodSource = FoodSource.from(dbData)
}
