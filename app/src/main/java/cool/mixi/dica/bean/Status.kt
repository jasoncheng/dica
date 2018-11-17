package cool.mixi.dica.bean

data class Status(
    var id: Int = 0,
    var user: User = User(),
    var friendica_owner: User = User(),
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
    var attachments: ArrayList<Attachment>? = ArrayList()
) {
    override fun equals(other: Any?): Boolean {
        if(other?.javaClass != this.javaClass)  return false

        other as Status
        return other.id == this.id
    }
}