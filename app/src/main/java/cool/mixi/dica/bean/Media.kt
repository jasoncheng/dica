package cool.mixi.dica.bean

data class Media(
    var media_id: Int,
    var size:Int,
    var image: MediaImage?
)

data class MediaImage(
    var w: Int,
    var h: Int,
    var image_type: String,
    var friendica_preview_url: String?
)