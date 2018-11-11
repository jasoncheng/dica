package cool.mixi.dica.bean

data class Status(
    var id: Int,
    var user: User,
    var friendica_owner: User,
    var statusnet_html: String,
    var text: String,
    var geo: Geo?,
    var in_reply_to_user_id: Int,
    var in_reply_to_status_id: Int,
    var in_reply_to_screen_name: String,
    var friendica_private: Boolean,
    var external_url: String,
    var created_at: String,
    var source: String,
    var favorited: Boolean,
    var friendica_activities: FriendicaActivities,
    var attachments: ArrayList<Attachment>
) {
    override fun equals(other: Any?): Boolean {
        if(other?.javaClass != this.javaClass)  return false

        other as Status
        return other.id == this.id
    }
}