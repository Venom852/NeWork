package ru.netology.nework.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.enumeration.EventType
import java.lang.reflect.Type
import kotlin.jvm.java

object Converter {
    private val gson = Gson()
    private val typeTokenSet: Type = object : TypeToken<Set<Long>>() {}.type
    private val typeTokenMap: Type = object : TypeToken<Map<Long, UserPreview>>() {}.type

    @TypeConverter
    fun convertToJsonEventType(eventType: EventType?): String? = gson.toJson(eventType)

    @TypeConverter
    fun convertFromJsonEventType(string: String): EventType? = gson.fromJson(string, EventType::class.java)

    @TypeConverter
    fun convertToJsonCoordinates(coordinates: Coordinates?): String? = gson.toJson(coordinates)

    @TypeConverter
    fun convertFromJsonCoordinates(string: String): Coordinates? = gson.fromJson(string, Coordinates::class.java)

    @TypeConverter
    fun convertToJsonSet(set: Set<Long>): String? = gson.toJson(set)

    @TypeConverter
    fun convertFromJsonSet(string: String): Set<Long> = gson.fromJson(string, typeTokenSet)

    @TypeConverter
    fun convertToJsonMap(map: Map<Long, UserPreview>): String? = gson.toJson(map)

    @TypeConverter
    fun convertFromJsonMap(string: String): Map<Long, UserPreview> = gson.fromJson(string, typeTokenMap)
}