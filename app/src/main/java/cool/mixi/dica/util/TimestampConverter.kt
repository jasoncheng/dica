package cool.mixi.dica.util

import androidx.room.TypeConverter
import java.util.*

object TimestampConverter {

    @TypeConverter
    @JvmStatic
    fun toLong(value: Date?): Long? = value?.time

    @TypeConverter
    @JvmStatic
    fun toDate(value: Long?): Date? = value?.let(::Date)
}