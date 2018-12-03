package cool.mixi.dica.util

import android.arch.persistence.room.TypeConverter
import java.util.*

object TimestampConverter {

    @TypeConverter
    @JvmStatic
    fun toLong(value: Date?): Long? = value?.time

    @TypeConverter
    @JvmStatic
    fun toDate(value: Long?): Date? = value?.let(::Date)
}