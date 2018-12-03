package cool.mixi.dica.bean

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import cool.mixi.dica.util.TimestampConverter
import java.util.*

@Entity(tableName = "meta")
data class Meta(
    @PrimaryKey
    @ColumnInfo(name = "url") var url: String,

    @ColumnInfo var title: String?,

    @ColumnInfo var icon: String?,

    @ColumnInfo var description: String?,

    @TypeConverters(TimestampConverter::class)
    @ColumnInfo var created: Date?
){
    fun copy(meta: Meta){
        this.url = meta.url
        this.icon = meta.icon
        this.title = meta.title
        this.description = meta.description
    }
}