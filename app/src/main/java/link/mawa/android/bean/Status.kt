package link.mawa.android.bean

data class Status(
    var id: Int,
    var user: User,
    var friendica_owner: User,
    var statusnet_html: String,
    var text: String,
    var geo: String,
    var in_reply_to_user_id: Int,
    var in_reply_to_status_id: Int,
    var friendica_private: Boolean,
    var external_url: String,
    var created_at: String,
    var source: String,
    var favorited: Boolean
)