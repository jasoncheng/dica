package cool.mixi.dica.bean

import cool.mixi.dica.util.dicaHTMLFilter
import cool.mixi.dica.util.getBaseUri
import java.util.*

data class Status(
    var id: Int = 0,
    var user: User = User(),
    var friendica_owner: User = User(),
    var friendica_comments: Int = 0,
    var statusnet_html: String = "",
    var text: String = "",
    var geo: Geo? = Geo(),
    var in_reply_to_user_id: Int = 0,
    var in_reply_to_status_id: Int = 0,
    var in_reply_to_screen_name: String = "",
    var friendica_private: Boolean = false,
    var external_url: String = "",
    var created_at: String = "",
    var source: String? = "",
    var favorited: Boolean = false,
    var friendica_activities: FriendicaActivities = FriendicaActivities(),
    var attachments: ArrayList<Attachment>? = ArrayList(),
    var apEntry: APEntry? = APEntry(),
    var avatar: String = "",
    var retweeted_status: Status? = null,

    // extra params for status page (comments)
    var indent: Int = 0,
    var firstCommentId: Int = 0,
    var isHide: Boolean = false,

    // extra params
    var enableNSFW: Boolean = false,
    var displayedTitle: HashMap<String, String> = HashMap()
) {
    override fun equals(other: Any?): Boolean {
        if (other?.javaClass != this.javaClass) return false

        other as Status
        return other.id == this.id
    }

    fun toFriendicaShareText(): String {
        var author = this.apEntry?.author?.name
        var profile = this.apEntry?.author?.uri
        var link = this.apEntry?.id
        this.user.url.isEmpty().let {
            if (it) return@let
            author = this.user.screen_name
            profile = this.user.url
            avatar = this.user.profile_image_url_large
            link = this.external_url
        }

        var sb = StringBuffer()
        sb.append("[share ")
        sb.append("author='$author' ")
        sb.append("avatar='${this.avatar}' ")
        sb.append("posted='${this.created_at}' ")
        sb.append("profile='$profile' ")
        sb.append("link='$link' ")
        sb.append("] ")
        sb.append(
            "\n${this.statusnet_html
                .replace("\\[".toRegex(), "(")
                .replace("\\]".toRegex(), ")")
                .dicaHTMLFilter(true, external_url.getBaseUri())}\n"
        )
        sb.append("[/share]")
        return sb.toString()
    }
}