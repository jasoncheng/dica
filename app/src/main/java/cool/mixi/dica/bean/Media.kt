package cool.mixi.dica.bean

data class Media(
    var id: String = "",
    var media_id: Int = 0,
    var size:Int,
    var image: MediaImage?
)

data class MediaImage(
    var w: Int,
    var h: Int,
    var image_type: String,
    var friendica_preview_url: String?
) {
    fun hashId(): String? {
        friendica_preview_url?.let {
            "\\/photo\\/([a-z0-9]+)-".toRegex().find(it)?.groupValues?.let {group ->
                return group[1]
            }
        }
        return friendica_preview_url
    }
}