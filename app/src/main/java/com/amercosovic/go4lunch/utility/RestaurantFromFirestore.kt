package com.amercosovic.go4lunch.utility

import com.amercosovic.go4lunch.model.Restaurant
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object RestaurantFromFirestore {
    // Parse JSON String from Firestore with jacksonObjectMapper, convert to Restaurant object and return it
    fun getRestaurant(data: String): Restaurant {
        val mapper = jacksonObjectMapper()
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        mapper.configure(
            com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES,
            true
        )
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
        return mapper.readValue(data, Restaurant::class.java)
    }
}