package cool.mixi.dica.bean

import java.io.Serializable

data class Notification(
    var id: Int,
    var parent: Int,
    var url: String,
    var photo: String,
    var name: String,
    var timestamp: Long,
    var msg_plain: String,
    var seen: Int,
    var type: Int,
    var date_rel: String,
    var otype: String
): Serializable