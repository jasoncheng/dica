package cool.mixi.dica.bean

import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.NOCASE
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import cool.mixi.dica.util.TimestampConverter
import java.util.*

@Entity(tableName = "tag")
data class HashTag(
    @PrimaryKey
    @ColumnInfo(collate = NOCASE) var name: String,

    @ColumnInfo var searchCount: Int = 0,

    @ColumnInfo var articleCount: Int = 0,

    @TypeConverters(TimestampConverter::class)
    @ColumnInfo var updatedAt: Date? = Calendar.getInstance().time
)