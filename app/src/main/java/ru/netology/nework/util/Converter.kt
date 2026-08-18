package ru.netology.nework.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.enumeration.EventType
import ru.netology.nework.util.AndroidUtils.toJsonUserPreview
import ru.netology.nework.util.AndroidUtils.toUserPreview
import java.lang.reflect.Type
import java.time.Instant
import kotlin.jvm.java

object Converter {
    private val gson = Gson()

    //TODO(Спросить: в чем разница получения методов type?)
//    private val typeTokenSet: Type = TypeToken.getParameterized(Set::class.java, Long::class.java).type
    private val typeTokenSet: Type = object : TypeToken<Set<Long>>() {}.type

//    private val typeTokenMap: Type = TypeToken.getParameterized(Map::class.java, Long::class.java,
//        UserPreview::class.java).type
    private val typeTokenMap: Type = object : TypeToken<Map<Long, UserPreview>>() {}.type

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

//    @TypeConverter
//    fun convertToJsonSet(set: Set<Long>): String? {
//        var number = ""
//
//        set.forEach {
//            number = "$number$it,"
//        }
//
//        return number
//    }
//
//    @TypeConverter
//    fun convertFromJsonSet(string: String): Set<Long> {
//        val listString = string.split(',')
//        val setLong = mutableSetOf<Long>()
//
//        listString.forEach { number ->
//            if (!number.isBlank()) {
//                setLong.add(number.toLong())
//                return@forEach
//            }
//        }
//
//        return setLong.toSet()
//    }
//
//    @TypeConverter
//    fun convertToJsonMap(map: Map<Long, UserPreview>): String {
//        var key = ""
//        var value = ""
//        var mapString = ""
//
//        map.forEach {
//            key = "$key${it.key},"
//            value = "$value${toJsonUserPreview(it.value)},"
//        }
//
//        mapString = "$key/$value"
//
//        return mapString
//    }
//
//    @TypeConverter
//    fun convertFromJsonMap(string: String): Map<Long, UserPreview> {
//        var keyListString = emptyList<String>()
//        var valueListString = emptyList<String>()
//        val listString = string.split('/')
//        val map = mutableMapOf<Long, UserPreview>()
//
//        keyListString = listString[0].split(',')
//        valueListString = listString[1].split(',')
//
//        keyListString.forEach { key ->
//            if (!key.isBlank()) {
//                valueListString.forEach { value ->
//                    if (!value.isBlank()) {
//                        map.put(key.toLong(), toUserPreview(value))
////                        map[key.toLong()] = toUserPreview(value)
//                        return@forEach
//                    }
//                }
//            }
//        }
//
//        return map.toMap()
//    }

    @TypeConverter
    fun convertToJsonInstant(instant: Instant?): String? = gson.toJson(instant)

    @TypeConverter
    fun convertFromJsonInstant(string: String): Instant? = gson.fromJson(string, Instant::class.java)
//
//    @TypeConverter
//    fun convertToJsonField(jsonArray: JsonArray?): String? = gson.toJson(jsonArray)
//
//    @TypeConverter
//    fun convertFromJsonField(string: String): JsonArray? = gson.fromJson(string, JsonArray::class.java)
}