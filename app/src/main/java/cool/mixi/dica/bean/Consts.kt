package cool.mixi.dica.bean

object Consts {

    // Room Storage Library
    const val DB_NAME = "DiCa"
    const val TTL_META = 60 * 24 * 3

    val COMPRESS_PHOTO_QUALITY = 70
    val API_CONNECT_TIMEOUT = 60.toLong()
    val API_READ_TIMEOUT = 60.toLong()
    val CACHE_SIZE_IN_MB = 10
    val TIMELINE_PAGE_SIZE = 20

    val REQ_PHOTO_PATH = 999
    val REQ_STICKER = 987

    val OTYPE_ITEM = "item"

    val FRIENDICA_WEB = "https://friendi.ca"
    val MIXI_URL = "http://dica.mixi.cool"
    val API_HOST = "https://mawa.link"
    val ID_STATUS = "statusId"

    val FG_COMPOSE = "compose"
    val FG_PHOTO_CROP = "crop"
    val FG_USERS = "users"
    val FG_PHOTO_VIEWER = "photoViewer"

    val SDCARD_FOLDER_OUT = "out"

    val EXTRA_PHOTO_URI = "photoUri"
    val EXTRA_USER = "user"
    val EXTRA_USER_ID = "userId"
    val EXTRA_USER_EMAIL = "email"
    val EXTRA_USER_URL = "url"
    val EXTRA_PHOTOS = "photos"
    val EXTRA_PHOTO_INDEX = "photoIndex"
    val EXTRA_NOTIFICATIONS = "notifications"
    val EXTRA_IN_REPLY_STATUS_ID = "in_reply_to_status_id"
    val EXTRA_IN_REPLY_USERNAME = "in_reply_to_screen_name"
    val EXTRA_STICKER_URI = "sticker"
}