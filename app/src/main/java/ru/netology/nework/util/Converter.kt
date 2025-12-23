package ru.netology.nework.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.enumeration.EventType
import java.lang.reflect.Type
import java.time.Instant
import kotlin.jvm.java

object Converter {
    private val gson = Gson()

    private val typeTokenSet: Type = TypeToken.getParameterized(Set::class.java, Long::class.java).type

    private val typeTokenMap: Type = TypeToken.getParameterized(Map::class.java, Long::class.java,
        UserPreview::class.java).type

    @TypeConverter
    fun convertToJsonAttachment(attachment: Attachment?): String? = gson.toJson(attachment)

    @TypeConverter
    fun convertFromJsonAttachment(string: String): Attachment? = gson.fromJson(string, Attachment::class.java)

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

    @TypeConverter
    fun convertToJsonInstant(instant: Instant?): String? = gson.toJson(instant)

    @TypeConverter
    fun convertFromJsonInstant(string: String): Instant? = gson.fromJson(string, Instant::class.java)
}