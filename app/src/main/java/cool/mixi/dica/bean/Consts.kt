package cool.mixi.dica.bean

object Consts {

    const val DB_NAME = "DiCa"
    const val TTL_META = 60 * 24 * 3
    const val TTL_USER = 60 * 24 * 5
    const val TTL_TAG = 60 * 24 * 5

    const val UPLOAD_MAX_PHOTOS = 3
    const val COMPRESS_PHOTO_QUALITY = 70
    const val API_CONNECT_TIMEOUT = 60.toLong()
    const val API_READ_TIMEOUT = 120.toLong()
    const val API_WRITE_TIMEOUT = 120.toLong()
    const val CACHE_SIZE_IN_MB = 10
    const val TIMELINE_PAGE_SIZE = 20

    const val REQ_PHOTO_PATH = 999
    const val REQ_STICKER = 987

    const val ENABLE_FULL_TEXT_SEARCH = false

    const val OTYPE_ITEM = "item"
    const val OTYPE_INTRO = "intro"

    const val FRIENDICA_WEB = "https://friendi.ca"
    const val MIXI_URL = "http://dica.mixi.cool"
    const val API_HOST = "https://meld.de"
    const val ID_STATUS = "statusId"

    const val FG_COMPOSE = "compose"
    const val FG_PHOTO_CROP = "crop"
    const val FG_USERS = "users"
    const val FG_PHOTO_VIEWER = "photoViewer"

    const val EXTRA_PHOTO_URI = "photoUri"
    const val EXTRA_USER = "user"
    const val EXTRA_USER_ID = "userId"
    const val EXTRA_USER_NAME = "userName"
    const val EXTRA_USER_EMAIL = "email"
    const val EXTRA_USER_URL = "url"
    const val EXTRA_PHOTOS = "photos"
    const val EXTRA_SEARCH_TERM = "searchTerm"
    const val EXTRA_PHOTO_INDEX = "photoIndex"
    const val EXTRA_NOTIFICATIONS = "notifications"
    const val EXTRA_IN_REPLY_STATUS_ID = "in_reply_to_status_id"
    const val EXTRA_IN_REPLY_USERNAME = "in_reply_to_screen_name"
    const val EXTRA_STICKER_URI = "sticker"
}