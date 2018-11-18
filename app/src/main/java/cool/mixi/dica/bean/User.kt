package cool.mixi.dica.bean

import java.io.Serializable
import java.net.URL

data class User(
    var id: Int = 0,
    var profile_image_url_large: String = "",
    var url: String = "",
    var screen_name: String = "",
    var name: String = "",
    var statusnet_profile_url: String = "",
    var friends_count: Int = 0,
    var statuses_count: Int = 0,
    var followers_count: Int = 0,
    var following: Boolean = false,
    var location: String = "",
    var description: String = ""
): Serializable {
    override fun equals(other: Any?): Boolean {
        if(other?.javaClass != this.javaClass) return false
        other as User
        return other.id == this.id
    }

    fun getDomain(): String {
        return try {
            URL(url).host
        }catch (e: Exception){
            url
        }
    }
}