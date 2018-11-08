package link.mawa.android.bean

import java.io.Serializable

data class User(
    var id: Int,
    var profile_image_url_large: String,
    var url: String,
    var screen_name: String,
    var name: String,
    var statusnet_profile_url: String,
    var friends_count: Int,
    var statuses_count: Int,
    var followers_count: Int,
    var following: Boolean,
    var location: String,
    var description: String
): Serializable {
    override fun equals(other: Any?): Boolean {
        if(other?.javaClass != this.javaClass) return false
        other as User
        return other.id == this.id
    }
}