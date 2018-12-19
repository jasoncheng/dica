package cool.mixi.dica.bean

import androidx.room.*
import cool.mixi.dica.util.TimestampConverter
import java.io.Serializable
import java.net.URL
import java.util.*

@Entity(
    primaryKeys = ["id", "url"],
    tableName =  "user",
    indices = [
        Index(value = ["screen_name", "name"])
    ]
)
data class User (
    var id:Int = 0,
    @ColumnInfo(name = "avatar") var profile_image_url_large: String = "",
    @ColumnInfo var url: String = "",
    @ColumnInfo var screen_name: String = "",
    @ColumnInfo var name: String = "",

    @TypeConverters(TimestampConverter::class)
    @ColumnInfo  var updatedAt: Date? = Calendar.getInstance().time,

    @Ignore var statusnet_profile_url: String = "",
    @Ignore var friends_count: Int = 0,
    @Ignore var statuses_count: Int = 0,
    @Ignore var followers_count: Int = 0,
    @Ignore var following: Boolean = false,
    @Ignore var location: String = "",
    @Ignore var description: String = ""
): Serializable {

    override fun equals(other: Any?): Boolean {
        if(other?.javaClass != this.javaClass) return false
        other as User
        return other.id == this.id
    }

    fun getEmail(): String {
        return "$screen_name@${getDomain()}"
    }

    fun getDomain(): String {
        return try {
            URL(url).host
        }catch (e: Exception){
            url
        }
    }
}